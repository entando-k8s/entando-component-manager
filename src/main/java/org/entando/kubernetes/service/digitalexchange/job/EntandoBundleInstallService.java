package org.entando.kubernetes.service.digitalexchange.job;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.exception.job.JobConflictException;
import org.entando.kubernetes.exception.k8ssvc.K8SServiceClientException;
import org.entando.kubernetes.model.bundle.BundleReader;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloader;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.EntandoBundle;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleComponentJob;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleJob;
import org.entando.kubernetes.model.digitalexchange.JobStatus;
import org.entando.kubernetes.model.digitalexchange.JobType;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntandoBundleInstallService implements ApplicationContextAware {

    private final @NonNull KubernetesService k8sService;
    private final @NonNull EntandoBundleJobService jobService;
    private final @NonNull EntandoCoreClient engineService;
    private final @NonNull BundleDownloader bundleDownloader;
    private final @NonNull EntandoBundleJobRepository jobRepo;
    private final @NonNull EntandoBundleComponentJobRepository jobComponentRepo;
    private final @NonNull InstalledEntandoBundleRepository installedComponentRepo;
    private Collection<ComponentProcessor> componentProcessors = new ArrayList<>();

    public EntandoBundleJob install(String componentId, String version) {
        EntandoDeBundle bundle = k8sService.getBundleByName(componentId)
                .orElseThrow(() -> new K8SServiceClientException("Bundle with name " + componentId + " not found"));

        Optional<EntandoBundleJob> j = searchForCompletedOrConflictingJob(bundle);

        return j.orElseGet(() -> createAndSubmitNewInstallJob(bundle, version));
    }

    private EntandoBundleJob createAndSubmitNewInstallJob(EntandoDeBundle bundle, String version) {
        EntandoDeBundleTag versionToInstall = getBundleTag(bundle, version);
        EntandoBundleJob job = createInstallJob(bundle, versionToInstall);

        submitInstallAsync(job, bundle, versionToInstall);
        return job;
    }

    private Optional<EntandoBundleJob> searchForCompletedOrConflictingJob(EntandoDeBundle bundle) {

        log.info("Verify validity of a new install job for component " + bundle.getMetadata().getName());

        EntandoBundleJob installCompletedJob = null;

        Optional<EntandoBundleJob> optionalExistingJob = getExistingJob(bundle);
        if (optionalExistingJob.isPresent()) {
            EntandoBundleJob j = optionalExistingJob.get();
            JobStatus js = j.getStatus();
            if (js.equals(JobStatus.INSTALL_COMPLETED)) {
                installCompletedJob = j;
            }
            if (js.isOfType(JobType.UNFINISHED)) {
                throw new JobConflictException("Conflict with another job for the component " + j.getComponentId()
                        + " - JOB ID: " + j.getId());
            }
        }
        return Optional.ofNullable(installCompletedJob);
    }

    private Optional<EntandoBundleJob> getExistingJob(EntandoDeBundle bundle) {
        String componentId = bundle.getSpec().getDetails().getName();
        Optional<EntandoBundleJob> lastJobStarted = jobRepo.findFirstByComponentIdOrderByStartedAtDesc(componentId);
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
                .orElseThrow(
                        () -> new InvalidBundleException("Version " + version + " not defined in bundle versions"));
    }

    private EntandoBundleJob createInstallJob(EntandoDeBundle bundle, EntandoDeBundleTag tag) {
        final EntandoBundleJob job = new EntandoBundleJob();

        job.setComponentId(bundle.getMetadata().getName());
        job.setComponentName(bundle.getSpec().getDetails().getName());
        job.setComponentVersion(tag.getVersion());
        job.setProgress(0);
        job.setStartedAt(LocalDateTime.now());
        job.setStatus(JobStatus.INSTALL_CREATED);

        EntandoBundleJob createdJob = jobRepo.save(job);
        log.debug("New installation job created " + job.toString());
        return createdJob;
    }

    public List<EntandoBundleJob> getAllJobs(String componentId) {
        return jobService.getJobs(componentId);
    }

    private void submitInstallAsync(EntandoBundleJob job, EntandoDeBundle bundle, EntandoDeBundleTag tag) {
        CompletableFuture.runAsync(() -> {
            log.info("Started new install job for component " + job.getComponentId() + "@" + tag.getVersion());

            JobStatus pipelineStatus = JobStatus.INSTALL_IN_PROGRESS;
            jobRepo.updateJobStatus(job.getId(), pipelineStatus);

            try {
                bundleDownloader.createTargetDirectory();
                Path pathToDownloadedBundle = bundleDownloader.saveBundleLocally(bundle, tag);
                pipelineStatus = installInstallables(job, new BundleReader(pathToDownloadedBundle));

                log.info("All installables have been processed correctly");
                EntandoBundle installedComponent = EntandoBundle.newFrom(bundle);
                installedComponent.setInstalled(true);
                installedComponent.setJob(job);
                installedComponentRepo.save(installedComponent);
                log.info("Component " + job.getComponentId() + " registered as installed in the system");

            } catch (Exception e) {
                log.error("An error occurred during component installation", e);
                pipelineStatus = rollback(job);
            }

            jobRepo.updateJobStatus(job.getId(), pipelineStatus);
            bundleDownloader.cleanTargetDirectory();
        });
    }

    private JobStatus rollback(EntandoBundleJob job) {
        JobStatus rollbackResult;
        // Get all the installed components for the job
        List<EntandoBundleComponentJob> jobRelatedComponents = jobComponentRepo.findAllByJob(job);

        // Filter jobs that are "uninstallable"
        List<EntandoBundleComponentJob> jobsToRollback = jobRelatedComponents.stream()
                .filter(this::isUninstallable)
                .map(EntandoBundleComponentJob::duplicate)
                .collect(Collectors.toList());

        try {
            // Cleanup resource folder
            uninstallResources(job, jobRelatedComponents);

            // For each installed component, get the installable and call uninstall in it
            componentProcessors.stream()
                    .map(processor -> processor.process(jobsToRollback))
                    .flatMap(Collection::stream)
                    .forEach(installable -> {
                        try {
                            installable.uninstall().get(); // In case of the plugin, that would mean delete the link
                            installable.getComponent().setStatus(JobStatus.INSTALL_ROLLBACK);
                        } catch (InterruptedException | ExecutionException e) {
                            log.error("Error rollbacking installed component", e);
                            installable.getComponent().setStatus(JobStatus.INSTALL_ROLLBACK_ERROR);
                        }

                        jobComponentRepo.save(installable.getComponent());
                    });

            rollbackResult = JobStatus.INSTALL_ROLLBACK;
        } catch (Exception e) {
            rollbackResult = JobStatus.INSTALL_ERROR;
        }

        return rollbackResult;
    }

    private boolean isUninstallable(EntandoBundleComponentJob component) {
        /* TODO: related to ENG-415 (https://jira.entando.org/browse/ENG-415)
          Except for IN_PROGRESS, everything should be uninstallable
          Uninstall operations should be idempotent to be able to provide this
         */
        return component.getComponentType() != ComponentType.RESOURCE
                && (component.getStatus().equals(JobStatus.INSTALL_COMPLETED)
                || (component.getStatus().equals(JobStatus.INSTALL_ERROR) && component.getComponentType() == ComponentType.PLUGIN));
    }

    private void uninstallResources(EntandoBundleJob job, List<EntandoBundleComponentJob> components) {
        componentProcessors.stream()
                .filter(processor -> processor.getComponentType() == ComponentType.RESOURCE)
                .map(processor -> processor.process(components))
                .flatMap(List::stream)
                .sorted(Comparator.comparing(Installable::getName))
                .peek(installable -> { //Mark all resources as uninstalled
                    EntandoBundleComponentJob uninstalledJobComponent = installable.getComponent();
                    uninstalledJobComponent.setJob(job);
                    uninstalledJobComponent.setStatus(JobStatus.INSTALL_ROLLBACK);
                    installable.setComponent(jobComponentRepo.save(uninstalledJobComponent));
                })
                .findFirst()
                .ifPresent(rootResourceInstallable -> {
                    //Remove root folder
                    log.info("Removing directory {}", rootResourceInstallable.getName());
                    engineService.deleteFolder(rootResourceInstallable.getName());
                });
    }

    private JobStatus installInstallables(EntandoBundleJob job, BundleReader bundleReader) {
        log.info("Processing installable list for component " + job.getComponentId());

        componentProcessors.stream()
                .map(processor -> processor.process(job, bundleReader))
                .flatMap(List::stream)
                .sorted(Comparator.comparingInt(i -> i.getInstallPriority().getPriority()))
                .peek(installable -> persistComponent(job, installable))
                .forEach(installable -> {
                    JobStatus result = installInstallable(installable);

                    if (result.equals(JobStatus.INSTALL_ERROR)) {
                        throw new EntandoComponentManagerException(job.getComponentId()
                                + " uninstallation can't proceed due to an error with one of the installed components");
                    }
                });

        return JobStatus.INSTALL_COMPLETED;
    }

    private JobStatus installInstallable(Installable installable) {
        EntandoBundleComponentJob installableComponent = installable.getComponent();
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

    private void persistComponent(EntandoBundleJob job, Installable installable) {
        EntandoBundleComponentJob component = new EntandoBundleComponentJob();
        component.setJob(job);
        component.setComponentType(installable.getComponentType());
        component.setName(installable.getName());
        component.setChecksum(installable.getChecksum());
        component.setStatus(JobStatus.INSTALL_CREATED);

        installable.setComponent(jobComponentRepo.save(component));

        log.debug("New component job created "
                + "for component of type " + installable.getComponentType() + " with name " + installable.getName());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        componentProcessors = applicationContext.getBeansOfType(ComponentProcessor.class).values();
    }

}

