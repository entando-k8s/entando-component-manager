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
import org.entando.kubernetes.model.bundle.reader.BundleReader;
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
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntandoBundleInstallService implements EntandoBundleJobExecutor {

    private final @NonNull EntandoBundleService bundleService;
    private final @NonNull BundleDownloaderFactory downloaderFactory;
    private final @NonNull EntandoBundleJobRepository jobRepo;
    private final @NonNull EntandoBundleComponentJobRepository compJobRepo;
    private final @NonNull InstalledEntandoBundleRepository bundleRepository;
    private final @NonNull Map<ComponentType, ComponentProcessor<?>> processorMap;

    public EntandoBundleJobEntity install(EntandoDeBundle bundle, EntandoDeBundleTag tag) {
        EntandoBundleJobEntity job = createInstallJob(bundle, tag);
        submitInstallAsync(job, bundle, tag);
        return job;
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
                int totalComponents = componentJobQueue.size();
                double increment = 1.0/totalComponents;

                try {
                    Optional<EntandoBundleComponentJobEntity> optCompJob = scheduler.extractFromQueue();
                    while (optCompJob.isPresent()) {
                        EntandoBundleComponentJobEntity installJob = optCompJob.get();
                        JobTracker<EntandoBundleComponentJobEntity> tracker = trackExecution(installJob, this::executeInstall);
                        scheduler.recordProcessedComponentJob(tracker.getJob());
                        if (tracker.getJob().getStatus().equals(JobStatus.INSTALL_ERROR)) {
                            throw new EntandoComponentManagerException(parentJob.getComponentId()
                                    + " install can't proceed due to an error with one of the components");
                        }
                        parentJobTracker.incrementProgress(increment);
                        optCompJob = scheduler.extractFromQueue();
                    }

                    saveAsInstalledBundle(bundle, parentJob);
                    parentJobResult.clearException();
                    parentJobResult.setStatus(JobStatus.INSTALL_COMPLETED);
                    parentJobResult.setProgress(1.0);
                    log.info("Bundle installed correctly");

                } catch (Exception installException) {
                    log.error("An error occurred during component installation", installException);
                    log.warn("Rolling installation of bundle " + parentJob.getComponentId() + "@" + parentJob.getComponentVersion()
                            + " back");

                    JobScheduler rollbackScheduler = scheduler.createRollbackScheduler();
                    try {
                        Optional<EntandoBundleComponentJobEntity> optCompJob = rollbackScheduler.extractFromQueue();
                        while (optCompJob.isPresent()) {
                            EntandoBundleComponentJobEntity rollbackJob = optCompJob.get();
                            if (isUninstallable(rollbackJob)) {
                                JobTracker<EntandoBundleComponentJobEntity> tracker = trackExecution(rollbackJob, this::executeRollback);
                                if (tracker.getJob().getStatus().equals(JobStatus.INSTALL_ROLLBACK_ERROR)) {
                                    throw new EntandoComponentManagerException(rollbackJob.getComponentType() + " " + rollbackJob.getComponentId()
                                            + " rollback can't proceed due to an error with one of the components");
                                }
                                rollbackScheduler.recordProcessedComponentJob(tracker.getJob());
                            }
                            optCompJob = rollbackScheduler.extractFromQueue();
                        }

                        log.info("Rollback operation completed successfully");
                        parentJobResult.clearException();
                        parentJobResult.setStatus(JobStatus.INSTALL_ROLLBACK);

                    } catch (Exception rollbackException) {
                        log.error("An error occurred during component rollback", rollbackException);
                        parentJobResult.setStatus(JobStatus.INSTALL_ERROR);
                        parentJobResult.setException(rollbackException);
                    }
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

    private Queue<Installable> getBundleInstallableComponents(EntandoDeBundle bundle, EntandoDeBundleTag tag,
            BundleDownloader bundleDownloader) {
        Path pathToDownloadedBundle = bundleDownloader.saveBundleLocally(bundle, tag);
        return getInstallableComponentsByPriority(new BundleReader(pathToDownloadedBundle));
    }

    private JobTracker<EntandoBundleComponentJobEntity> trackExecution(EntandoBundleComponentJobEntity job,
            Function<Installable, JobResult> action) {
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

    private boolean isUninstallable(EntandoBundleComponentJobEntity component) {
        /* TODO: related to ENG-415 (https://jira.entando.org/browse/ENG-415)
          Except for IN_PROGRESS, everything should be uninstallable
          Uninstall operations should be idempotent to be able to provide this
         */
        return component.getStatus().equals(JobStatus.INSTALL_COMPLETED)
                || (component.getStatus().equals(JobStatus.INSTALL_ERROR) && component.getComponentType() == ComponentType.PLUGIN);
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
                    String message = getMeaningfulErrorMessage(th);
                    log.error("Installable '{}' has errors: {}", installable.getName(), message, th);
                    return JobResult.builder()
                            .status(JobStatus.INSTALL_ERROR)
                            .exception(new Exception(message))
                            .build();
                });

        return installResult.join();
    }

}

