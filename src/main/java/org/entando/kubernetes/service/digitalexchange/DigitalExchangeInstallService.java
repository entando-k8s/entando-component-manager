package org.entando.kubernetes.service.digitalexchange;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.controller.digitalexchange.component.DigitalExchangeComponent;
import org.entando.kubernetes.exception.JobNotFoundException;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeEntity;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJobComponent;
import org.entando.kubernetes.model.digitalexchange.JobStatus;
import org.entando.kubernetes.repository.DigitalExchangeJobComponentRepository;
import org.entando.kubernetes.repository.DigitalExchangeJobRepository;
import org.entando.kubernetes.service.digitalexchange.client.DigitalExchangeBaseCall;
import org.entando.kubernetes.service.digitalexchange.client.DigitalExchangesClient;
import org.entando.kubernetes.service.digitalexchange.component.DigitalExchangeComponentsService;
import org.entando.kubernetes.service.digitalexchange.installable.Installable;
import org.entando.kubernetes.service.digitalexchange.installable.ComponentProcessor;
import org.entando.kubernetes.service.digitalexchange.job.JobExecutionException;
import org.entando.kubernetes.service.digitalexchange.job.ZipReader;
import org.entando.kubernetes.service.digitalexchange.job.model.ComponentDescriptor;
import org.entando.kubernetes.service.digitalexchange.model.DigitalExchange;
import org.entando.kubernetes.service.digitalexchange.signature.SignatureMatchingException;
import org.entando.kubernetes.service.digitalexchange.signature.SignatureUtil;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipFile;

import static java.util.Optional.ofNullable;

@Slf4j
@Service
@RequiredArgsConstructor
public class DigitalExchangeInstallService implements ApplicationContextAware {

    private final @NonNull DigitalExchangesService exchangesService;
    private final @NonNull DigitalExchangeComponentsService digitalExchangeComponentsService;
    private final @NonNull DigitalExchangesClient client;
    private final @NonNull DigitalExchangeJobRepository jobRepository;
    private final @NonNull DigitalExchangeJobComponentRepository componentRepository;

    private Collection<ComponentProcessor> componentProcessors = new ArrayList<>();

    public DigitalExchangeJob install(final String digitalExchangeId, final String componentId) {
        final DigitalExchangeEntity digitalExchange = exchangesService.findEntityById(digitalExchangeId);
        final DigitalExchangeComponent component = digitalExchangeComponentsService
                .getComponent(digitalExchange.convert(), componentId).getPayload();
        final Optional<DigitalExchangeJob> existingJob = jobRepository
                .findByComponentIdAndStatusNotEqual(componentId, JobStatus.UNINSTALL_COMPLETED);

        if (existingJob.isPresent()) {
            return existingJob.get();
        }

        final DigitalExchangeJob job = new DigitalExchangeJob();

        job.setComponentId(componentId);
        job.setComponentName(component.getName());
        job.setComponentVersion(component.getVersion());
        job.setDigitalExchange(digitalExchange);
        job.setProgress(0);
        job.setStartedAt(LocalDateTime.now());
        job.setStatus(JobStatus.INSTALL_CREATED);

        jobRepository.save(job);

        submitInstallAsync(job, digitalExchange.convert(), component);

        return job;
    }

    public DigitalExchangeJob getJob(final String componentId) {
        return jobRepository.findByComponentIdAndStatusNotEqual(componentId, JobStatus.UNINSTALL_COMPLETED)
                .orElseThrow(JobNotFoundException::new);
    }

    private void submitInstallAsync(final DigitalExchangeJob job, final DigitalExchange digitalExchange, final DigitalExchangeComponent component) {
        CompletableFuture.runAsync(() -> {
            Path tempZipPath = null;

            try {
                tempZipPath = Files.createTempFile(component.getId(), "");

                final DigitalExchangeBaseCall<InputStream> call = new DigitalExchangeBaseCall<>(
                        HttpMethod.GET, "digitalExchange", "components", component.getId(), "package");

                try (final InputStream in = client.getStreamResponse(digitalExchange, call)) {
                    Files.copy(in, tempZipPath, StandardCopyOption.REPLACE_EXISTING);
                }

                if (StringUtils.isNotEmpty(job.getDigitalExchange().getPublicKey())) {
                    verifyDownloadedContentSignature(tempZipPath, digitalExchange, component);
                }

                extractZip(job, tempZipPath);
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
        });
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

    private void extractZip(final DigitalExchangeJob job, final Path tempZipPath) {
        log.info("Processing DEPKG File");
        try (final ZipFile zip = new ZipFile(tempZipPath.toFile())) {
            final ZipReader zipReader = new ZipReader(zip);
            final ComponentDescriptor descriptor = zipReader.readDescriptorFile("descriptor.yaml", ComponentDescriptor.class);
            final List<Installable> installables = getInstallables(job, zipReader, descriptor);

            installables.forEach(installable -> installable.setComponent(persistComponent(job, installable)));

            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) { e.printStackTrace(); }

                final CompletableFuture[] completableFutures = installables.stream().map(installable -> {
                    final DigitalExchangeJobComponent component = installable.getComponent();
                    componentRepository.updateJobStatus(component.getId(), JobStatus.INSTALL_IN_PROGRESS);

                    final CompletableFuture<?> future = installable.install();
                    try {
                        future.get();
                        log.info("Installable '{}' finished successfully", installable.getName());
                        componentRepository.updateJobStatus(component.getId(), JobStatus.INSTALL_COMPLETED);
                    } catch (InterruptedException | ExecutionException ex) {
                        log.error("Installable '{}' has errors", installable.getName(), ex);
                        component.setStatus(JobStatus.INSTALL_ERROR);

                        if (ex.getCause() != null) {
                            String message = ex.getCause().getMessage();
                            if (ex.getCause() instanceof HttpClientErrorException) {
                                final HttpClientErrorException httpException = (HttpClientErrorException) ex.getCause();
                                message = httpException.getMessage() + "\n" + httpException.getResponseBodyAsString();
                            }
                            componentRepository.updateJobStatus(component.getId(), JobStatus.INSTALL_ERROR, message);
                        } else {
                            componentRepository.updateJobStatus(component.getId(), JobStatus.INSTALL_ERROR, ex.getMessage());
                        }
                    }

                    return future;
                }).toArray(CompletableFuture[]::new);
                CompletableFuture.allOf(completableFutures).whenComplete((object, ex) -> {
                    final JobStatus status = ex == null ? JobStatus.INSTALL_COMPLETED : JobStatus.INSTALL_ERROR;
                    jobRepository.updateJobStatus(job.getId(), status);
                });
            }).start();

            log.info("Finished processing. Have a nice day!");
            // add labels, etc
        } catch (IOException ex) {
            log.error("Error while extracting zip file", ex);
            throw new JobExecutionException("Unable to extract zip file", ex);
        }
    }

    private List<Installable> getInstallables(final DigitalExchangeJob job, final ZipReader zipReader,
                                              final ComponentDescriptor descriptor) throws IOException {
        final List<Installable> installables = new LinkedList<>();
        for (final ComponentProcessor processor : componentProcessors) {
            ofNullable(processor.process(job, zipReader, descriptor))
                .ifPresent(installables::addAll);
        }
        return installables;
    }

    private DigitalExchangeJobComponent persistComponent(final DigitalExchangeJob job, final Installable installable) {
        final DigitalExchangeJobComponent component = new DigitalExchangeJobComponent();
        component.setJob(job);
        component.setComponentType(installable.getComponentType());
        component.setName(installable.getName());
        component.setChecksum(installable.getChecksum());
        component.setStatus(JobStatus.INSTALL_CREATED);
        return componentRepository.save(component);
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        componentProcessors = applicationContext.getBeansOfType(ComponentProcessor.class).values();
    }

}
