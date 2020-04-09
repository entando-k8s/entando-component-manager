package org.entando.kubernetes.service.digitalexchange.job;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
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
import org.entando.kubernetes.exception.k8ssvc.K8SServiceClientException;
import org.entando.kubernetes.model.bundle.BundleReader;
import org.entando.kubernetes.model.bundle.descriptor.ComponentDescriptor;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloader;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
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
import org.entando.kubernetes.service.digitalexchange.entandocore.EntandoCoreService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
@Service
@RequiredArgsConstructor
public class DigitalExchangeInstallService implements ApplicationContextAware {

    private final @NonNull KubernetesService k8sService;
    private final @NonNull DigitalExchangeJobService jobService;
    private final @NonNull EntandoCoreService engineService;
    private final @NonNull BundleDownloader bundleDownloader;
    private final @NonNull DigitalExchangeJobRepository jobRepo;
    private final @NonNull DigitalExchangeJobComponentRepository jobComponentRepo;
    private final @NonNull DigitalExchangeInstalledComponentRepository installedComponentRepo;
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
            if (JobType.matches(js, JobType.UNFINISHED)) {
                throw new JobConflictException("Conflict with another job for the component " + j.getComponentId()
                        + " - JOB ID: " + j.getId());
            }
        }
        return Optional.ofNullable(installCompletedJob);
    }

    private Optional<DigitalExchangeJob> getExistingJob(EntandoDeBundle bundle) {
        String digitalExchangeId = bundle.getMetadata().getNamespace();
        String componentId = bundle.getSpec().getDetails().getName();
        Optional<DigitalExchangeJob> lastJobStarted = jobRepo
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

        DigitalExchangeJob createdJob = jobRepo.save(job);
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
            jobRepo.updateJobStatus(job.getId(), pipelineStatus);

            try {
                bundleDownloader.createTargetDirectory();
                Path pathToDownloadedBundle = bundleDownloader.saveBundleLocally(bundle, tag);
                List<Installable> installables = getInstallables(job, pathToDownloadedBundle);
                pipelineStatus = processInstallableList(job, installables);
                if (pipelineStatus.equals(JobStatus.INSTALL_COMPLETED)) {
                    log.info("All installables have been processed correctly");
                    DigitalExchangeComponent installedComponent = DigitalExchangeComponent.newFrom(bundle);
                    installedComponent.setInstalled(true);
                    installedComponentRepo.save(installedComponent);
                    log.info("Component " + job.getComponentId() + " registered as installed in the system");
                }

            } catch (Exception e) {
                log.error("An error occurred during digital-exchange component installation", e.getCause());
                pipelineStatus = rollback(job);
            }

            jobRepo.updateJobStatus(job.getId(), pipelineStatus);
            bundleDownloader.cleanTargetDirectory();
        });
    }

    private JobStatus rollback(DigitalExchangeJob job) {
        JobStatus rollbackResult;
        // Get all the installed components for the job
        List<DigitalExchangeJobComponent> jobRelatedComponents = jobComponentRepo.findAllByJob(job);

        // Filter jobs that are "uninstallable"
        List<DigitalExchangeJobComponent> installedOrInProgress = jobRelatedComponents.stream()
                .filter(j -> JobType.INSTALL_ROLLBACK.matches(j.getStatus()))
                .collect(Collectors.toList());

        try {
            // Cleanup resource folder
            cleanupResourceFolder(job, installedOrInProgress);
            // For each installed component
            for(DigitalExchangeJobComponent jc: installedOrInProgress) {
                // Revert the operation
                DigitalExchangeJobComponent revertJob = jc.duplicate();
                componentProcessors.stream()
                        .filter(processor -> processor.shouldProcess(revertJob.getComponentType()))
                        .forEach(processor -> processor.uninstall(revertJob));
                revertJob.setStatus(JobStatus.UNINSTALL_COMPLETED);
                jobComponentRepo.save(revertJob);
            }
            rollbackResult = JobStatus.INSTALL_ROLLBACK;
        } catch (Exception e) {
           rollbackResult = JobStatus.INSTALL_ERROR;
        }
        // In case of the plugin, that would mean delete the link
        return rollbackResult;
    }

    private void cleanupResourceFolder(DigitalExchangeJob job, List<DigitalExchangeJobComponent> components) {
        String componentRootFolder = "/" + job.getComponentId();
        Optional<DigitalExchangeJobComponent> rootResourceFolder = components.stream().filter(component ->
                component.getComponentType() == ComponentType.RESOURCE
                        && component.getName().equals(componentRootFolder)
        ).findFirst();

        if (rootResourceFolder.isPresent()) {
            engineService.deleteFolder(componentRootFolder);
            components.stream().filter(component -> component.getComponentType() == ComponentType.RESOURCE)
                    .forEach(component -> {
                        DigitalExchangeJobComponent uninstalledJobComponent = component.duplicate();
                        uninstalledJobComponent.setJob(job);
                        uninstalledJobComponent.setStatus(JobStatus.UNINSTALL_COMPLETED);
                        jobComponentRepo.save(uninstalledJobComponent);
                    });
        }
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
        jobComponentRepo.updateJobStatus(installableComponent.getId(), JobStatus.INSTALL_IN_PROGRESS);

        CompletableFuture<?> future = installable.install();
        CompletableFuture<JobStatus> installResult = future.thenApply(vd -> {
            log.debug("Installable '{}' finished successfully", installable.getName());
            jobComponentRepo.updateJobStatus(installableComponent.getId(), JobStatus.INSTALL_COMPLETED);
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
                jobComponentRepo
                        .updateJobStatus(installableComponent.getId(), JobStatus.INSTALL_ERROR, message);
            } else {
                jobComponentRepo
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

        component = jobComponentRepo.save(component);

        log.debug("New component job created "
                + "for component of type " + installable.getComponentType() + " with name " + installable.getName());
        return component;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        componentProcessors = applicationContext.getBeansOfType(ComponentProcessor.class).values();
    }

}

