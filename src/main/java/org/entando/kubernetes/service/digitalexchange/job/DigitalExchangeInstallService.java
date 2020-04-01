package org.entando.kubernetes.service.digitalexchange.job;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.exception.job.JobConflictException;
import org.entando.kubernetes.exception.job.JobPackageException;
import org.entando.kubernetes.exception.k8ssvc.K8SServiceClientException;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloader;
import org.entando.kubernetes.model.bundle.BundleReader;
import org.entando.kubernetes.model.bundle.descriptor.ComponentDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeComponent;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJobComponent;
import org.entando.kubernetes.model.digitalexchange.JobStatus;
import org.entando.kubernetes.model.digitalexchange.JobType;
import org.entando.kubernetes.repository.DigitalExchangeInstalledComponentRepository;
import org.entando.kubernetes.repository.DigitalExchangeJobComponentRepository;
import org.entando.kubernetes.repository.DigitalExchangeJobRepository;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class DigitalExchangeInstallService implements ApplicationContextAware {

    private final @NonNull KubernetesService k8sService;
    private final @NonNull DigitalExchangeJobService jobService;
    private final @NonNull DigitalExchangeInstalledComponentRepository installedComponentRepository;
    private final @NonNull DigitalExchangeJobRepository jobRepository;
    private final @NonNull DigitalExchangeJobComponentRepository componentRepository;
    private final @NonNull BundleDownloader bundleDownloader;
    private Collection<ComponentProcessor> componentProcessors = new ArrayList<>();

    public DigitalExchangeJob install(String componentId, String version) {
        EntandoDeBundle bundle = k8sService.getBundleByName(componentId)
                .orElseThrow(() -> new K8SServiceClientException("Bundle with name " + componentId + " not found"));

        Optional<DigitalExchangeJob> j = searchForCompletedOrConflictingJob(bundle);

        return j.orElse(createAndSubmitNewInstallJob(bundle, version));

    }

    private DigitalExchangeJob createAndSubmitNewInstallJob(EntandoDeBundle bundle, String version) {
        EntandoDeBundleTag versionToInstall = getBundleTag(bundle, version);
        DigitalExchangeJob job = createInstallJob(bundle, versionToInstall);

        submitInstallAsync(job, bundle, versionToInstall);
        return job;
    }

    private Optional<DigitalExchangeJob> searchForCompletedOrConflictingJob(EntandoDeBundle bundle ) {

        log.info("Verify validity of a new install job for component " + bundle.getMetadata().getName());

        DigitalExchangeJob installCompletedJob = null;

        Optional<DigitalExchangeJob> optionalExistingJob = getExistingJob(bundle);
        if (optionalExistingJob.isPresent()) {
            DigitalExchangeJob j = optionalExistingJob.get();
            JobStatus js = j.getStatus();
            if (js.equals(JobStatus.INSTALL_COMPLETED)) {
                installCompletedJob = j;
            }
            if (JobType.isOfType(js, JobType.UNFINISHED)) {
                throw new JobConflictException("Conflict with another job for the component " + j.getComponentId()
                        + " - JOB ID: " + j.getId());
            }
        }
        return Optional.ofNullable(installCompletedJob);
    }

    private Optional<DigitalExchangeJob> getExistingJob(EntandoDeBundle bundle) {
        String digitalExchangeId = bundle.getMetadata().getNamespace();
        String componentId = bundle.getSpec().getDetails().getName();
        Optional<DigitalExchangeJob> lastJobStarted = jobRepository
                .findFirstByDigitalExchangeAndComponentIdOrderByStartedAtDesc(digitalExchangeId, componentId);
        if (lastJobStarted.isPresent()) {
            // To be an existing job it should be Running or completed
            if (lastJobStarted.get().getStatus() == JobStatus.UNINSTALL_COMPLETED) {
                return Optional.empty();
            }
            return lastJobStarted;
        }
        return Optional.empty();
    }

    private EntandoDeBundleTag getBundleTag(EntandoDeBundle bundle, String version) {
        log.info("Extracting version " + version + " from bundle");
        String versionToFind = BundleUtilities.getBundleVersionOrFail(bundle, version);
        return bundle.getSpec().getTags().stream().filter(t -> t.getVersion().equals(versionToFind)).findAny()
                .orElseThrow(() -> new InvalidBundleException("Version " + version + " not defined in bundle versions"));
    }

    private DigitalExchangeJob createInstallJob(EntandoDeBundle bundle, EntandoDeBundleTag tag) {
        final DigitalExchangeJob job = new DigitalExchangeJob();

        job.setComponentId(bundle.getMetadata().getName());
        job.setComponentName(bundle.getSpec().getDetails().getName());
        job.setComponentVersion(tag.getVersion());
        job.setDigitalExchange(bundle.getMetadata().getNamespace());
        job.setProgress(0);
        job.setStartedAt(LocalDateTime.now());
        job.setStatus(JobStatus.INSTALL_CREATED);

        DigitalExchangeJob createdJob = jobRepository.save(job);
        log.debug("New installation job created " + job.toString());
        return createdJob;
    }

    public List<DigitalExchangeJob> getAllJobs(String componentId) {
        return jobService.getAllJobs(componentId);
    }

    private void submitInstallAsync(DigitalExchangeJob job, EntandoDeBundle bundle, EntandoDeBundleTag tag) {
        CompletableFuture.runAsync(() -> {
            log.info("Started new install job for component " + job.getComponentId() + "@" + tag.getVersion());

            JobStatus pipelineStatus = JobStatus.INSTALL_IN_PROGRESS;
            jobRepository.updateJobStatus(job.getId(), pipelineStatus);

            try {
                bundleDownloader.createTargetDirectory();
                Path pathToDownloadedBundle = bundleDownloader.saveBundleLocally(bundle, tag);
                List<Installable> installables = getInstallables(job, pathToDownloadedBundle);
                pipelineStatus = processInstallableList(job, installables);
                if (pipelineStatus.equals(JobStatus.INSTALL_COMPLETED)) {
                    log.info("All installables have been processed correctly");
                    DigitalExchangeComponent installedComponent = DigitalExchangeComponent.newFrom(bundle);
                    installedComponent.setInstalled(true);
                    installedComponentRepository.save(installedComponent);
                    log.info("Component " + job.getComponentId() + " registered as installed in the system");
                }

            } catch (Exception e) {
                log.error("An error occurred during digital-exchange component installation", e.getCause());
                pipelineStatus = JobStatus.INSTALL_ERROR;
            }

            jobRepository.updateJobStatus(job.getId(), pipelineStatus);
            bundleDownloader.cleanTargetDirectory();
        });
    }

    private JobStatus processInstallableList(DigitalExchangeJob job, List<Installable> installableList) {
        log.info("Processing installable list for component " + job.getComponentId());
        installableList.forEach(installable -> installable.setComponent(persistComponent(job, installable)));

        JobStatus installSucceded = JobStatus.INSTALL_COMPLETED;
        for (Installable installable: installableList) {
            installSucceded = processInstallable(installable);
            if (installSucceded.equals(JobStatus.INSTALL_ERROR)) {
                throw new RuntimeException(job.getComponentId() + " installation can't proceed due to an error with one of the installed components");
            }
        }
        return installSucceded;
    }

    private JobStatus processInstallable(Installable installable) {
        DigitalExchangeJobComponent installableComponent = installable.getComponent();
        componentRepository.updateJobStatus(installableComponent.getId(), JobStatus.INSTALL_IN_PROGRESS);

        CompletableFuture<?> future = installable.install();
        CompletableFuture<JobStatus> installResult = future.thenApply(vd -> {
            log.debug("Installable '{}' finished successfully", installable.getName());
            componentRepository.updateJobStatus(installableComponent.getId(), JobStatus.INSTALL_COMPLETED);
            return JobStatus.INSTALL_COMPLETED;
        }).exceptionally(th -> {
            log.error("Installable '{}' has errors", installable.getName(), th.getCause());

            installableComponent.setStatus(JobStatus.INSTALL_ERROR);
            if (th.getCause() != null) {
                String message = th.getCause().getMessage();
                if (th.getCause() instanceof HttpClientErrorException) {
                    HttpClientErrorException httpException = (HttpClientErrorException) th.getCause();
                    message =
                            httpException.getMessage() + "\n" + httpException.getResponseBodyAsString();
                }
                componentRepository
                        .updateJobStatus(installableComponent.getId(), JobStatus.INSTALL_ERROR, message);
            } else {
                componentRepository
                        .updateJobStatus(installableComponent.getId(), JobStatus.INSTALL_ERROR, th.getMessage());
            }
            return JobStatus.INSTALL_ERROR;
        });
        return installResult.join();
    }

    private List<Installable> getInstallables(DigitalExchangeJob job, Path p) {
        log.info("Extracting installable components from downloaded bundle");
        try {
            BundleReader r = new BundleReader(p);
            ComponentDescriptor descriptor = r.readBundleDescriptor();
            return getInstallables(job, r, descriptor);
        } catch (IOException e) {
            log.error("An error occurred while getting installables components", e);
            throw new UncheckedIOException(e);
        }
    }

    private List<Installable> getInstallables(DigitalExchangeJob job, BundleReader r,
            ComponentDescriptor descriptor) throws IOException {
        List<Installable> installables = new LinkedList<>();
        for (ComponentProcessor processor : componentProcessors) {
            ofNullable(processor.process(job, r, descriptor))
                    .ifPresent(installables::addAll);
        }
        return installables;
    }

    private DigitalExchangeJobComponent persistComponent(DigitalExchangeJob job, Installable installable) {
        DigitalExchangeJobComponent component = new DigitalExchangeJobComponent();
        component.setJob(job);
        component.setComponentType(installable.getComponentType());
        component.setName(installable.getName());
        component.setChecksum(installable.getChecksum());
        component.setStatus(JobStatus.INSTALL_CREATED);

        component = componentRepository.save(component);

        log.debug("New component job created "
                + "for component of type " + installable.getComponentType() + " with name " + installable.getName());
        return component;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        componentProcessors = applicationContext.getBeansOfType(ComponentProcessor.class).values();
    }

}

