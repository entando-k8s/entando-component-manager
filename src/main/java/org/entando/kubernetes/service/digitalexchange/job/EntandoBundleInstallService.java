package org.entando.kubernetes.service.digitalexchange.job;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.entando.kubernetes.model.digitalexchange.*;
import org.entando.kubernetes.model.job.*;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntandoBundleInstallService implements EntandoBundleJobExecutor {

    private final @NonNull KubernetesService k8sService;
    private final @NonNull EntandoBundleJobService jobService;
    private final @NonNull BundleDownloader bundleDownloader;
    private final @NonNull EntandoBundleJobRepository jobRepo;
    private final @NonNull EntandoBundleComponentJobRepository jobComponentRepo;
    private final @NonNull InstalledEntandoBundleRepository installedComponentRepo;
    private final @NonNull Map<ComponentType, ComponentProcessor<?>> processorMap;

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
        job.setStatus(JobStatus.INSTALL_CREATED);

        EntandoBundleJob createdJob = jobRepo.save(job);
        log.debug("New installation job created " + job.toString());
        return createdJob;
    }

    public List<EntandoBundleJob> getAllJobs(String componentId) {
        return jobService.getJobs(componentId);
    }

    private void submitInstallAsync(EntandoBundleJob parentJob, EntandoDeBundle bundle, EntandoDeBundleTag tag) {
        CompletableFuture.runAsync(() -> {
            log.info("Started new install job for component " + parentJob.getComponentId() + "@" + tag.getVersion());

            JobResult parentJobResult = JobResult.builder().status(JobStatus.INSTALL_IN_PROGRESS).build();

            JobScheduler scheduler = new JobScheduler();
            parentJob.setStartedAt(LocalDateTime.now());
            parentJob.setStatus(parentJobResult.getStatus());
            jobRepo.save(parentJob);

            List<Installable> bundleInstallableComponents = getBundleInstallableComponents(bundle, tag);
            scheduler.queueAll(bundleInstallableComponents);

            try {
                Optional<Installable> optInstallable = scheduler.extractFromQueue();
                while(optInstallable.isPresent()) {
                    JobTracker cjt = install(optInstallable.get(), parentJob);
                    scheduler.recordProcessedComponentJob(cjt);
                    if (cjt.getTrackedJob().getStatus().equals(JobStatus.INSTALL_ERROR)) {
                        throw new EntandoComponentManagerException(parentJob.getComponentId()
                                + " install can't proceed due to an error with one of the components");
                    }
                    optInstallable = scheduler.extractFromQueue();
                }

                saveAsInstalledBundle(bundle, parentJob);
                parentJobResult.clearException();
                parentJobResult.setStatus(JobStatus.INSTALL_COMPLETED);
                log.info("Bundle installed correctly");

            } catch (Exception installException) {
                log.error("An error occurred during component installation", installException);
                parentJobResult = rollback(scheduler);
            }

            parentJob.setFinishedAt(LocalDateTime.now());
            parentJob.setStatus(parentJobResult.getStatus());
            if (parentJobResult.hasException()) {
                parentJob.setErrorMessage(parentJobResult.getErrorMessage());
            }
            jobRepo.save(parentJob);
            bundleDownloader.cleanTargetDirectory();
        });
    }

    private JobResult rollback(JobScheduler scheduler) {
        JobResult rollbackResult = JobResult.builder().build();
        scheduler.activateRollbackMode();
        try {
            Optional<Installable> optInstallable = scheduler.extractFromQueue();
            while(optInstallable.isPresent()) {
                Installable rollbackInstallable = optInstallable.get();
                JobTracker cjt = processComponentJobRollback(rollbackInstallable);
                if (cjt.getTrackedJob().getStatus().equals(JobStatus.INSTALL_ROLLBACK_ERROR)) {
                    throw new EntandoComponentManagerException(cjt.getTrackedJob().getComponentType() + " " + cjt.getTrackedJob().getName()
                            + " rollback can't proceed due to an error with one of the components");
                }
                scheduler.recordProcessedComponentJob(cjt);
                optInstallable = scheduler.extractFromQueue();
            }

            rollbackResult.setException(null);
            rollbackResult.setStatus(JobStatus.INSTALL_ROLLBACK_COMPLETED);

        } catch (Exception rollbackException) {

            log.error("An error occurred during component rollback", rollbackException);
            rollbackResult.setStatus(JobStatus.INSTALL_ERROR);
            rollbackResult.setException(rollbackException);
        }
        return rollbackResult;
    }

    private List<Installable> getBundleInstallableComponents(EntandoDeBundle bundle, EntandoDeBundleTag tag) {
        Path pathToDownloadedBundle = bundleDownloader.saveBundleLocally(bundle, tag);
        return getInstallableComponentsByPriority(new BundleReader(pathToDownloadedBundle));
    }

    private JobTracker processComponentJobRollback(Installable installable) {
        JobTracker cjt = new JobTracker(installable, installable.getJob().getJob(), jobComponentRepo);
        if (isUninstallable(cjt.getTrackedJob())) {
            cjt.startTracking(JobStatus.INSTALL_ROLLBACK_IN_PROGRESS);
            JobResult rollbackJR = rollback(installable);
            cjt.stopTrackingTime(rollbackJR);
        }
        return cjt;
    }

    private JobTracker install(Installable installable, EntandoBundleJob parentJob) {
        JobTracker componentJobTracker = new JobTracker(installable, parentJob, jobComponentRepo);
        componentJobTracker.startTracking(JobStatus.INSTALL_IN_PROGRESS);
        JobResult installableResult = executeInstall(installable);
        componentJobTracker.stopTrackingTime(installableResult);
        return componentJobTracker;
    }

    private List<Installable> getInstallableComponentsByPriority(BundleReader bundleReader) {
        return processorMap.values().stream()
                .map(processor -> processor.process(bundleReader))
                .flatMap(List::stream)
                .sorted(Comparator.comparingInt(Installable::getPriority))
                .collect(Collectors.toList());
    }

    private void saveAsInstalledBundle(EntandoDeBundle bundle, EntandoBundleJob job) {
        EntandoBundle installedComponent = EntandoBundle.newFrom(bundle);
        installedComponent.setInstalled(true);
        installedComponent.setJob(job);
        installedComponentRepo.save(installedComponent);
        log.info("Component " + job.getComponentId() + " registered as installed in the system");
    }

    private JobResult rollback(Installable<?> installable) {
        return installable.uninstall()
                .thenApply(vd -> JobResult.builder().status(JobStatus.INSTALL_ROLLBACK_COMPLETED).build())
                .exceptionally(th -> {
                    log.error(String.format("Error rolling back %s %s",
                            installable.getComponentType(),
                            installable.getName()), th);
                    String message = getMeaningfulErrorMessage(th);
                    return JobResult.builder()
                            .status(JobStatus.INSTALL_ROLLBACK_ERROR)
                            .exception(new Exception(message))
                            .build();
                })
                .join();
    }

    private boolean isUninstallable(EntandoBundleComponentJob component) {
        /* TODO: related to ENG-415 (https://jira.entando.org/browse/ENG-415)
          Except for IN_PROGRESS, everything should be uninstallable
          Uninstall operations should be idempotent to be able to provide this
         */
        return component.getStatus().equals(JobStatus.INSTALL_COMPLETED) ||
                (component.getStatus().equals(JobStatus.INSTALL_ERROR) && component.getComponentType() == ComponentType.PLUGIN);
    }



    private <T> JobResult executeInstall(Installable<T> installable) {

        CompletableFuture<?> future = installable.install();
        CompletableFuture<JobResult> installResult = future
                .thenApply(vd -> {
                    log.debug("Installable '{}' finished successfully", installable.getName());
                    return JobResult.builder().status(JobStatus.INSTALL_COMPLETED).build();
                }).exceptionally(th -> {
                    log.error("Installable '{}' has errors", installable.getName(), th.getCause());
                    String message = getMeaningfulErrorMessage(th);
                    return JobResult.builder()
                            .status(JobStatus.INSTALL_ERROR)
                            .exception(new Exception(message))
                            .build();
                });

        return installResult.join();
    }


//    private EntandoBundleComponentJob buildComponentJob(EntandoBundleJob job, Installable installable) {
//        EntandoBundleComponentJob component = new EntandoBundleComponentJob();
//        component.setJob(job);
//        component.setComponentType(installable.getComponentType());
//        component.setName(installable.getName());
//        component.setChecksum(installable.getChecksum());
//        component.setStatus(JobStatus.INSTALL_CREATED);
//
//        log.debug("New component job created "
//                + "for component of type " + installable.getComponentType() + " with name " + installable.getName());
//        return component;
//    }

}

