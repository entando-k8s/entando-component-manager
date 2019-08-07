package org.entando.kubernetes.service.digitalexchange;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.controller.digitalexchange.component.DigitalExchangeComponent;
import org.entando.kubernetes.model.EntandoPluginDeploymentResponse;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.digitalexchange.JobStatus;
import org.entando.kubernetes.model.digitalexchange.JobType;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.client.DigitalExchangeBaseCall;
import org.entando.kubernetes.service.digitalexchange.client.DigitalExchangesClient;
import org.entando.kubernetes.service.digitalexchange.component.DigitalExchangeComponentsService;
import org.entando.kubernetes.service.digitalexchange.entandocore.EntandoEngineService;
import org.entando.kubernetes.service.digitalexchange.job.JobExecutionException;
import org.entando.kubernetes.service.digitalexchange.job.ZipReader;
import org.entando.kubernetes.service.digitalexchange.job.model.ComponentDescriptor;
import org.entando.kubernetes.service.digitalexchange.job.model.ComponentSpecDescriptor;
import org.entando.kubernetes.service.digitalexchange.job.model.Descriptor;
import org.entando.kubernetes.service.digitalexchange.job.model.PageModelDescriptor;
import org.entando.kubernetes.service.digitalexchange.job.model.ServiceDescriptor;
import org.entando.kubernetes.service.digitalexchange.job.model.WidgetDescriptor;
import org.entando.kubernetes.service.digitalexchange.job.processors.ComponentProcessor;
import org.entando.kubernetes.service.digitalexchange.job.processors.ComponentProcessorResolver;
import org.entando.kubernetes.service.digitalexchange.model.DigitalExchange;
import org.entando.kubernetes.service.digitalexchange.signature.SignatureMatchingException;
import org.entando.kubernetes.service.digitalexchange.signature.SignatureUtil;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipFile;

import static java.util.Optional.ofNullable;

@Slf4j
@Service
@RequiredArgsConstructor
public class DigitalExchangeInstallService {

    private final @NonNull KubernetesService kubernetesService;
    private final @NonNull DigitalExchangesService exchangesService;
    private final @NonNull DigitalExchangeComponentsService digitalExchangeComponentsService;
    private final @NonNull DigitalExchangesClient client;
    private final @NonNull ComponentProcessorResolver processorResolver;
    private final @NonNull EntandoEngineService entandoEngineService;

    private static final Map<String, JobStatus> statusMap = new HashMap<>();

    static {
        statusMap.put("requested", JobStatus.CREATED);
        statusMap.put("started", JobStatus.IN_PROGRESS);
        statusMap.put("successful", JobStatus.COMPLETED);
        statusMap.put("failed", JobStatus.ERROR);
    }

    public DigitalExchangeJob install(final String digitalExchangeId, final String componentId) {
        final DigitalExchange digitalExchange = exchangesService.findById(digitalExchangeId);
        final DigitalExchangeComponent component = digitalExchangeComponentsService
                .getComponent(digitalExchange, componentId).getPayload();

        install(digitalExchange, component);
        return null; // we need to persist the job, because not every plugin will have a service
    }

    public DigitalExchangeJob getJob(final String componentId) {
        final EntandoPluginDeploymentResponse deployment = kubernetesService.getDeployment(componentId);
        final DigitalExchangeJob job = new DigitalExchangeJob();

        job.setComponentId(componentId);
        job.setComponentName(deployment.getPlugin());
        job.setComponentVersion(deployment.getImage());
        job.setDigitalExchangeId(deployment.getDigitalExchangeId());
        job.setDigitalExchangeUrl(deployment.getDigitalExchangeUrl());
        job.setJobType(JobType.INSTALL);
        job.setProgress(0);
        job.setStatus(statusMap.getOrDefault(deployment.getDeploymentPhase(), JobStatus.CREATED));

        return job;
    }

    private void install(final DigitalExchange digitalExchange, final DigitalExchangeComponent component) {
        Path tempZipPath = null;

        try {
            tempZipPath = Files.createTempFile(component.getId(), "");

            final DigitalExchangeBaseCall<InputStream> call = new DigitalExchangeBaseCall<>(
                    HttpMethod.GET, "digitalExchange", "components", component.getId(), "package");

            try (final InputStream in = client.getStreamResponse(digitalExchange, call)) {
                Files.copy(in, tempZipPath, StandardCopyOption.REPLACE_EXISTING);
            }

            if (StringUtils.isNotEmpty(digitalExchange.getPublicKey())) {
                verifyDownloadedContentSignature(tempZipPath, digitalExchange, component);
            }

            extractZip(tempZipPath, digitalExchange, component.getId());
        } catch (SignatureMatchingException ex) {
            log.error("Component signature doesn't match public key", ex);
            throw new JobExecutionException("Unable to verify component signature", ex);
        } catch (IOException | UncheckedIOException ex) {
            log.error("Error while downloading component", ex);
            throw new JobExecutionException("Unable to save component", ex);
        } finally {
            if (tempZipPath != null && !tempZipPath.toFile().delete()) {
                log.warn("Unable to delete temporary zip file {}", tempZipPath.toFile().getAbsolutePath());
            }
        }
    }

    private void verifyDownloadedContentSignature(
            final Path tempZipPath, final DigitalExchange digitalExchange,
            final DigitalExchangeComponent component) throws IOException {

        try (final InputStream in = Files.newInputStream(tempZipPath, StandardOpenOption.READ)) {
            final boolean signatureMatches = SignatureUtil.verifySignature(
                    in, SignatureUtil.publicKeyFromPEM(digitalExchange.getPublicKey()),
                    component.getSignature());
            if (!signatureMatches) {
                throw new SignatureMatchingException("Component signature not matching public key");
            }
        }
    }

    public void extractZip(final Path tempZipPath, final DigitalExchange digitalExchange, final String componentId) {
        log.info("Processing DEPKG File");
        try (final ZipFile zip = new ZipFile(tempZipPath.toFile())) {
            final ZipReader zipReader = new ZipReader(zip);
            final ComponentDescriptor descriptor = zipReader.readDescriptorFile("descriptor.yaml", ComponentDescriptor.class)
                    .orElseThrow(() -> new JobExecutionException("`descriptor.yaml` is mandatory and wasn't found in the root directory"));
            final Optional<ServiceDescriptor> serviceDescriptor = ofNullable(descriptor.getComponents())
                    .map(ComponentSpecDescriptor::getService);

            if (serviceDescriptor.isPresent()) {
                final ComponentProcessor<ServiceDescriptor> processor = processorResolver.resolve(ServiceDescriptor.class);
                processor.processComponent(digitalExchange, componentId, serviceDescriptor.get(), zipReader, null);
            }

            final Optional<List<String>> widgetsDescriptor = ofNullable(descriptor.getComponents())
                    .map(ComponentSpecDescriptor::getWidgets);
            final Optional<List<String>> pageModelsDescriptor = ofNullable(descriptor.getComponents())
                    .map(ComponentSpecDescriptor::getPageModels);

            final String componentFolder = "/" + componentId;
            entandoEngineService.createFolder(componentFolder);
            final List<String> resourceFolders = zipReader.getResourceFolders();
            for (final String resourceFolder : resourceFolders) {
                log.info("Creating folder {}", resourceFolder);
                entandoEngineService.createFolder(componentFolder + "/" + resourceFolder);
            }

            final List<String> resourceFiles = zipReader.getResourceFiles();
            for (final String resourceFile : resourceFiles) {
                log.info("Uploading file {}", resourceFile);
                zipReader.readFileAsDescriptor(resourceFile).ifPresent(file -> {
                    file.setFolder(componentFolder + "/" + file.getFolder());
                    entandoEngineService.uploadFile(file);
                });
            }

            if (widgetsDescriptor.isPresent()) {
                processDescriptor(digitalExchange, componentId, zipReader, widgetsDescriptor.get(), WidgetDescriptor.class);
            }
            if (pageModelsDescriptor.isPresent()) {
                processDescriptor(digitalExchange, componentId, zipReader, pageModelsDescriptor.get(), PageModelDescriptor.class);
            }
            // add contentModel, contentType, labels, etc
            // we have to handle the exceptions gracefully

        } catch (IOException ex) {
            log.error("Error while extracting zip file", ex);
            throw new JobExecutionException("Unable to extract zip file", ex);
        }
    }

    private <T extends Descriptor> void processDescriptor(
            final DigitalExchange digitalExchange, final String componentId,
            final ZipReader zipReader, final List<String> descriptors, final Class<T> clazz) throws IOException {
        final ComponentProcessor<T> processor = processorResolver.resolve(clazz);
        for (final String fileName : descriptors) {
            final T descriptor = zipReader.readDescriptorFile(fileName, clazz).orElse(null);
            final String folder = fileName.contains("/") ? fileName.substring(0, fileName.lastIndexOf("/")) : "";
            if (descriptor == null) {
                log.warn("File {} not found in the package", fileName);
            }
            processor.processComponent(digitalExchange, componentId, descriptor, zipReader, folder);
        }
    }

}
