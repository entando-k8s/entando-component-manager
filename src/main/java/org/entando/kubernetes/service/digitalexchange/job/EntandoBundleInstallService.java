package org.entando.kubernetes.service.digitalexchange.job;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
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
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderFactory;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleEntity;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobResult;
import org.entando.kubernetes.model.job.JobScheduler;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.model.job.JobTracker;
import org.entando.kubernetes.model.job.JobType;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntandoBundleInstallService implements EntandoBundleJobExecutor {

    private final @NonNull KubernetesService k8sService;
    private final @NonNull EntandoBundleService bundleService;
    private final @NonNull EntandoBundleJobService jobService;
    private final @NonNull BundleDownloaderFactory downloaderFactory;
    private final @NonNull EntandoBundleJobRepository jobRepo;
    private final @NonNull EntandoBundleComponentJobRepository compJobRepo;
    private final @NonNull InstalledEntandoBundleRepository bundleRepository;
    private final @NonNull Map<ComponentType, ComponentProcessor<?>> processorMap;

    public EntandoBundleJobEntity install(String componentId, String version) {
        EntandoDeBundle bundle = k8sService.getBundleByName(componentId)
                .orElseThrow(() -> new K8SServiceClientException("Bundle with name " + componentId + " not found"));

        Optional<EntandoBundleJobEntity> j = searchForCompletedOrConflictingJob(bundle);

        return j.orElseGet(() -> createAndSubmitNewInstallJob(bundle, version));
    }

    private EntandoBundleJobEntity createAndSubmitNewInstallJob(EntandoDeBundle bundle, String version) {
        EntandoDeBundleTag versionToInstall = getBundleTag(bundle, version);
        EntandoBundleJobEntity job = createInstallJob(bundle, versionToInstall);

        submitInstallAsync(job, bundle, versionToInstall);
        return job;
    }

    private Optional<EntandoBundleJobEntity> searchForCompletedOrConflictingJob(EntandoDeBundle bundle) {

        log.info("Verify validity of a new install job for component " + bundle.getMetadata().getName());

        EntandoBundleJobEntity installCompletedJob = null;

        Optional<EntandoBundleJobEntity> optionalExistingJob = getExistingJob(bundle);
        if (optionalExistingJob.isPresent()) {
            EntandoBundleJobEntity j = optionalExistingJob.get();
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

    private Optional<EntandoBundleJobEntity> getExistingJob(EntandoDeBundle bundle) {
        String componentId = bundle.getSpec().getDetails().getName();
        Optional<EntandoBundleJobEntity> lastJobStarted = jobRepo.findFirstByComponentIdOrderByStartedAtDesc(componentId);
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

    private EntandoBundleJobEntity createInstallJob(EntandoDeBundle bundle, EntandoDeBundleTag tag) {
        final EntandoBundleJobEntity job = new EntandoBundleJobEntity();

        job.setComponentId(bundle.getMetadata().getName());
        job.setComponentName(bundle.getSpec().getDetails().getName());
        job.setComponentVersion(tag.getVersion());
        job.setProgress(0);
        job.setStatus(JobStatus.INSTALL_CREATED);

        EntandoBundleJobEntity createdJob = jobRepo.save(job);
        log.debug("New installation job created " + job.toString());
        return createdJob;
    }

    public List<EntandoBundleJobEntity> getAllJobs(String componentId) {
        return jobService.getJobs(componentId);
    }

    private void submitInstallAsync(EntandoBundleJobEntity parentJob, EntandoDeBundle bundle, EntandoDeBundleTag tag) {
        CompletableFuture.runAsync(() -> {
            log.info("Started new install job for component " + parentJob.getComponentId() + "@" + tag.getVersion());

            JobTracker<EntandoBundleJobEntity> parentJobTracker = new JobTracker<>(parentJob, jobRepo);
            JobScheduler scheduler = new JobScheduler();
            BundleDownloader bundleDownloader = downloaderFactory.newDownloader();

            JobResult parentJobResult = JobResult.builder().status(JobStatus.INSTALL_IN_PROGRESS).build();
            parentJobTracker.startTracking(parentJobResult.getStatus());


            try {
                Queue<Installable> bundleInstallableComponents = getBundleInstallableComponents(bundle, tag, bundleDownloader);
                Queue<EntandoBundleComponentJobEntity> componentJobQueue = bundleInstallableComponents.stream()
                        .map(i -> {
                            EntandoBundleComponentJobEntity cj = new EntandoBundleComponentJobEntity();
                            cj.setParentJob(parentJob);
                            cj.setComponentType(i.getComponentType());
                            cj.setComponentId(i.getName());
                            cj.setChecksum(i.getChecksum());
                            cj.setInstallable(i);
                            return cj;
                        })
                        .collect(Collectors.toCollection(ArrayDeque::new));
                scheduler.queueAll(componentJobQueue);

                try {
                    Optional<EntandoBundleComponentJobEntity> optCompJob = scheduler.extractFromQueue();
                    while(optCompJob.isPresent()) {
                        EntandoBundleComponentJobEntity installJob = optCompJob.get();
                        JobTracker<EntandoBundleComponentJobEntity> tracker = trackExecution(installJob, this::executeInstall);
                        scheduler.recordProcessedComponentJob(tracker.getJob());
                        if (tracker.getJob().getStatus().equals(JobStatus.INSTALL_ERROR)) {
                            throw new EntandoComponentManagerException(parentJob.getComponentId()
                                    + " install can't proceed due to an error with one of the components");
                        }
                        optCompJob = scheduler.extractFromQueue();
                    }

                    saveAsInstalledBundle(bundle, parentJob);
                    parentJobResult.clearException();
                    parentJobResult.setStatus(JobStatus.INSTALL_COMPLETED);
                    log.info("Bundle installed correctly");

                } catch (Exception installException) {
                    log.error("An error occurred during component installation", installException);
                    log.warn("Rolling installation of bundle " + parentJob.getComponentId() + "@" + parentJob.getComponentVersion() + " back");
                    parentJobResult = rollback(scheduler);
                }

            } catch (Exception e) {
                log.error("An error occurred while reading components from the bundle", e);
                parentJobResult.setStatus(JobStatus.INSTALL_ERROR);
                parentJobResult.setException(e);
            }



            parentJobTracker.stopTrackingTime(parentJobResult);
            bundleDownloader.cleanTargetDirectory();
        });
    }


    private Queue<Installable> getBundleInstallableComponents(EntandoDeBundle bundle, EntandoDeBundleTag tag, BundleDownloader bundleDownloader) {
        Path pathToDownloadedBundle = bundleDownloader.saveBundleLocally(bundle, tag);
        return getInstallableComponentsByPriority(new BundleReader(pathToDownloadedBundle));
    }

    private JobTracker<EntandoBundleComponentJobEntity> trackExecution(EntandoBundleComponentJobEntity job, Function<Installable, JobResult> action) {
        JobTracker<EntandoBundleComponentJobEntity> componentJobTracker = new JobTracker<>(job, compJobRepo);
        componentJobTracker.startTracking(JobStatus.INSTALL_IN_PROGRESS);
        JobResult result = action.apply(job.getInstallable());
        componentJobTracker.stopTrackingTime(result);
        return componentJobTracker;
    }

    private Queue<Installable> getInstallableComponentsByPriority(BundleReader bundleReader) {
        return processorMap.values().stream()
                .map(processor -> processor.process(bundleReader))
                .flatMap(List::stream)
                .sorted(Comparator.comparingInt(Installable::getPriority))
                .collect(Collectors.toCollection(ArrayDeque::new));
    }

    private void saveAsInstalledBundle(EntandoDeBundle bundle, EntandoBundleJobEntity job) {
        EntandoBundleEntity installedComponent = bundleService.convertToEntityFromEcr(bundle);
        installedComponent.setVersion(job.getComponentVersion());
        installedComponent.setJob(job);
        installedComponent.setInstalled(true);
        bundleRepository.save(installedComponent);
        log.info("Component " + job.getComponentId() + " registered as installed in the system");
    }

    private JobResult rollback(JobScheduler scheduler) {
        JobResult rollbackResult = JobResult.builder().build();
        scheduler.activateRollbackMode();
        try {
            Optional<EntandoBundleComponentJobEntity> optCompJob = scheduler.extractFromQueue();
            while(optCompJob.isPresent()) {
                EntandoBundleComponentJobEntity rollbackJob = optCompJob.get();
                if (isUninstallable(rollbackJob)) {
                    JobTracker<EntandoBundleComponentJobEntity> tracker = trackExecution(rollbackJob, this::executeRollback);
                    if (tracker.getJob().getStatus().equals(JobStatus.INSTALL_ROLLBACK_ERROR)) {
                        throw new EntandoComponentManagerException(rollbackJob.getComponentType() + " " + rollbackJob.getComponentId()
                                + " rollback can't proceed due to an error with one of the components");
                    }
                    scheduler.recordProcessedComponentJob(tracker.getJob());
                }
                optCompJob = scheduler.extractFromQueue();
            }

            log.info("Rollback operation completed successfully");
            rollbackResult.setException(null);
            rollbackResult.setStatus(JobStatus.INSTALL_ROLLBACK);

        } catch (Exception rollbackException) {
            log.error("An error occurred during component rollback", rollbackException);
            rollbackResult.setStatus(JobStatus.INSTALL_ERROR);
            rollbackResult.setException(rollbackException);
        }
        return rollbackResult;
    }

    private boolean isUninstallable(EntandoBundleComponentJobEntity component) {
        /* TODO: related to ENG-415 (https://jira.entando.org/browse/ENG-415)
          Except for IN_PROGRESS, everything should be uninstallable
          Uninstall operations should be idempotent to be able to provide this
         */
        return component.getStatus().equals(JobStatus.INSTALL_COMPLETED) ||
                (component.getStatus().equals(JobStatus.INSTALL_ERROR) && component.getComponentType() == ComponentType.PLUGIN);
    }


    private JobResult executeRollback(Installable<?> installable) {
        return installable.uninstall()
                .thenApply(vd -> JobResult.builder().status(JobStatus.INSTALL_ROLLBACK).build())
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

}

