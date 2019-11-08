package org.entando.kubernetes.service.digitalexchange;

import java.nio.file.StandardCopyOption;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.controller.digitalexchange.component.DigitalExchangeComponent;
import org.entando.kubernetes.exception.JobNotFoundException;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeEntity;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJobComponent;
import org.entando.kubernetes.model.digitalexchange.InstallableInstallResult;
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
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipFile;

import static java.util.Optional.ofNullable;

@Slf4j
@Service
public class DigitalExchangeInstallService implements ApplicationContextAware {

    private final DigitalExchangesService exchangesService;
    private final DigitalExchangeComponentsService digitalExchangeComponentsService;
    private final DigitalExchangesClient client;
    private final DigitalExchangeJobRepository jobRepository;
    private final DigitalExchangeJobComponentRepository componentRepository;

    private Collection<ComponentProcessor> componentProcessors = new ArrayList<>();

    public DigitalExchangeInstallService(
            DigitalExchangesService exchangesService,
            DigitalExchangeComponentsService digitalExchangeComponentsService,
            DigitalExchangesClient client, DigitalExchangeJobRepository jobRepository,
            DigitalExchangeJobComponentRepository componentRepository) {
        this.exchangesService = exchangesService;
        this.digitalExchangeComponentsService = digitalExchangeComponentsService;
        this.client = client;
        this.jobRepository = jobRepository;
        this.componentRepository = componentRepository;
    }

    @Transactional(rollbackFor = Throwable.class)
    public DigitalExchangeJob install(final String digitalExchangeId, final String componentId) {
        final DigitalExchangeEntity digitalExchange = exchangesService.findEntityById(digitalExchangeId);
        final DigitalExchangeComponent component = digitalExchangeComponentsService
                .getComponent(digitalExchange.convert(), componentId).getPayload();
        final Optional<DigitalExchangeJob> existingJob = jobRepository
                .findByComponentIdAndStatusNotEqual(componentId, JobStatus.UNINSTALLED);

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
        job.setStatus(JobStatus.CREATED);

        jobRepository.save(job);

        install(job, digitalExchange.convert(), component);

        return job;
    }

    public DigitalExchangeJob getJob(final String componentId) {
        return jobRepository.findByComponentIdAndStatusNotEqual(componentId, JobStatus.UNINSTALLED)
                .orElseThrow(JobNotFoundException::new);
    }

    private void install(final DigitalExchangeJob job, final DigitalExchange digitalExchange, final DigitalExchangeComponent component) {

        jobRepository.updateJobStatus(job.getId(), JobStatus.IN_PROGRESS);

        getComponentPackageStream(digitalExchange, component)
                .thenApply(packageStream -> createComponentLocalFile(component, packageStream))
                .thenApply(packageLocalPath -> {
                    if (StringUtils.isNotEmpty(job.getDigitalExchange().getPublicKey())) {
                        verifyDownloadedContentSignature(packageLocalPath, digitalExchange, component);
                    }
                    return packageLocalPath;
                })
                .thenApply(packageLocalPath -> extractInstallableFromZip(job, packageLocalPath))
                .thenAccept(installableList -> processAllInstallable(job, installableList))
                .thenApply(this::handleInstallationSuccess)
                .exceptionally(this::handleInstallationFailure)
                .thenAccept(jobStatus -> jobRepository.updateJobStatus(job.getId(), jobStatus));

    }


    private CompletableFuture<InputStream> getComponentPackageStream(DigitalExchange digitalExchange, DigitalExchangeComponent component) {
        return CompletableFuture.supplyAsync(() -> {
            DigitalExchangeBaseCall<InputStream> call = new DigitalExchangeBaseCall<>(
                    HttpMethod.GET, "digitalExchange", "components", component.getId(), "package");
            return  client.getStreamResponse(digitalExchange, call);
        });
    }

    private Path createComponentLocalFile(DigitalExchangeComponent component, InputStream packageStream) {
        Path tempPath = null;
        try {
            tempPath = Files.createTempFile(component.getId(), "");
            Files.copy(packageStream, tempPath, StandardCopyOption.REPLACE_EXISTING);
            return tempPath;
        } catch (IOException e) {
           throw new JobExecutionException("An error occurred while copying the package stream locally", e, tempPath);
        }
    }

    private void verifyDownloadedContentSignature(
            final Path tempPath, final DigitalExchange digitalExchange,
            final DigitalExchangeComponent component) {

        try (final InputStream in = Files.newInputStream(tempPath, StandardOpenOption.READ)) {
            final boolean signatureMatches = SignatureUtil.verifySignature(
                    in, SignatureUtil.publicKeyFromPEM(digitalExchange.getPublicKey()),
                    component.getSignature());
            if (!signatureMatches) {
                throw new SignatureMatchingException("Component signature not matching public key");
            }
        } catch (IOException e) {
            throw new JobExecutionException("An error occurred while copying the package stream locally", e, tempPath);
        }
    }

    private List<Installable> extractInstallableFromZip(DigitalExchangeJob job, Path tempPath) {
        try (ZipFile zip = new ZipFile(tempPath.toFile())) {
            ZipReader zipReader = new ZipReader(zip);
            ComponentDescriptor descriptor = zipReader.readDescriptorFile("descriptor.yaml", ComponentDescriptor.class);
            return getInstallables(job, zipReader, descriptor);
        } catch (IOException e) {
            throw new JobExecutionException("Unable to extract the installables from the zip file", e, tempPath);
        }
    }

    private void processAllInstallable(DigitalExchangeJob job, List<Installable> installableList) {
        installableList.forEach(installable -> installable.setComponent(persistComponent(job, installable)));

        List<CompletableFuture> cfl = installableList.stream()
                .peek(i ->  componentRepository.updateJobStatus(i.getComponent().getId(), JobStatus.IN_PROGRESS))
                .map(Installable::install)
                .map(cf -> cf.thenAcceptAsync(o -> {
                    InstallableInstallResult iir = (InstallableInstallResult) o; // Required for type erasure
                    componentRepository.updateJobStatus(iir.getInstallable().getComponent().getId(), JobStatus.COMPLETED);
                }))
                .collect(Collectors.toList());

        CompletableFuture
                .allOf(cfl.toArray(new CompletableFuture[]{}))
                .thenRun(() -> log.info("Finished processing. Have a nice day!"))
                .exceptionally(ex -> {
                    log.error("Error while extracting zip file", ex);
                    throw new JobExecutionException("An error occurred while installing components", ex);
                });

    }

    private JobStatus handleInstallationFailure(Throwable ex) {
        log.error("An error occurred while processing the digital-exchange package", ex);
        if (ex instanceof JobExecutionException) {
            JobExecutionException e = (JobExecutionException) ex;
            if (e.getJobAssociatedTempPath() != null && !e.getJobAssociatedTempPath().toFile().delete()) {
                log.warn("Unable to delete temporary zip file {}", e.getJobAssociatedTempPath().toFile().getAbsolutePath());
            }
        }
        return JobStatus.ERROR;
    }

    private JobStatus handleInstallationSuccess(Void any) {
        return JobStatus.COMPLETED;
    }

    private List<Installable> getInstallables(final DigitalExchangeJob job, final ZipReader zipReader,
                                              final ComponentDescriptor descriptor) throws IOException {
        List<Installable> installables = new LinkedList<>();
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
        component.setStatus(JobStatus.CREATED);
        return componentRepository.save(component);
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        componentProcessors = applicationContext.getBeansOfType(ComponentProcessor.class).values();
    }

}
