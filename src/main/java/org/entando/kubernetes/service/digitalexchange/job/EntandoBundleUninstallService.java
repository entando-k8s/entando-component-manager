package org.entando.kubernetes.service.digitalexchange.job;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.EnumSet;
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
import org.entando.kubernetes.exception.digitalexchange.BundleNotInstalledException;
import org.entando.kubernetes.exception.job.JobConflictException;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleEntity;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobProgress;
import org.entando.kubernetes.model.job.JobResult;
import org.entando.kubernetes.model.job.JobScheduler;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.model.job.JobTracker;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleComponentUsageService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntandoBundleUninstallService implements EntandoBundleJobExecutor {

    private final @NonNull EntandoBundleJobRepository jobRepo;
    private final @NonNull EntandoBundleComponentJobRepository compJobRepo;
    private final @NonNull InstalledEntandoBundleRepository installedComponentRepository;
    private final @NonNull EntandoBundleComponentUsageService usageService;
    private final @NonNull Map<ComponentType, ComponentProcessor<?>> processorMap;

    public EntandoBundleJobEntity
    uninstall(String componentId) {
        EntandoBundleEntity installedBundle = installedComponentRepository.findById(componentId)
                .orElseThrow(() -> new BundleNotInstalledException("Bundle " + componentId + " is not installed"));

        verifyBundleUninstallIsPossibleOrThrow(installedBundle);

        return createAndSubmitUninstallJob(installedBundle.getJob());
    }

    private void verifyBundleUninstallIsPossibleOrThrow(EntandoBundleEntity bundle) {
        if (bundle.getJob() != null && bundle.getJob().getStatus().equals(JobStatus.INSTALL_COMPLETED)) {
            verifyNoComponentInUseOrThrow(bundle);
            verifyNoConcurrentUninstallOrThrow(bundle);
        } else {
            throw new EntandoComponentManagerException(
                    "Installed bundle " + bundle.getId() + " associated with invalid job");
        }
    }

    private void verifyNoConcurrentUninstallOrThrow(EntandoBundleEntity bundle) {
        Optional<EntandoBundleJobEntity> lastJob = jobRepo.findFirstByComponentIdOrderByStartedAtDesc(bundle.getId());
        EnumSet<JobStatus> concurrentUninstallJobStatus = EnumSet
                .of(JobStatus.UNINSTALL_IN_PROGRESS, JobStatus.UNINSTALL_CREATED);
        if (lastJob.isPresent() && lastJob.get().getStatus().isAny(concurrentUninstallJobStatus)) {
            throw new JobConflictException(
                    "A concurrent uninstall process for bundle " + bundle.getId() + " is running");
        }
    }

    private void verifyNoComponentInUseOrThrow(EntandoBundleEntity bundle) {
        List<EntandoBundleComponentJobEntity> bundleComponentJobs = compJobRepo.findAllByParentJob(bundle.getJob());
        if (bundleComponentJobs.stream()
                .anyMatch(e -> usageService.getUsage(e.getComponentType(), e.getComponentId()).getUsage() > 0)) {
            throw new JobConflictException(
                    "Some of bundle " + bundle.getId() + " components are in use and bundle can't be uninstalled");
        }
    }

    private EntandoBundleJobEntity createAndSubmitUninstallJob(EntandoBundleJobEntity lastAvailableJob) {

        EntandoBundleJobEntity uninstallJob = new EntandoBundleJobEntity();
        uninstallJob.setComponentId(lastAvailableJob.getComponentId());
        uninstallJob.setComponentName(lastAvailableJob.getComponentName());
        uninstallJob.setComponentVersion(lastAvailableJob.getComponentVersion());
        uninstallJob.setStatus(JobStatus.UNINSTALL_CREATED);
        uninstallJob.setProgress(0.0);
        EntandoBundleJobEntity savedJob = jobRepo.save(uninstallJob);

        submitUninstallAsync(uninstallJob, lastAvailableJob);

        return savedJob;
    }

    private void submitUninstallAsync(EntandoBundleJobEntity parentJob, EntandoBundleJobEntity referenceJob) {
        CompletableFuture.runAsync(() -> {
            JobTracker<EntandoBundleJobEntity> parentJobTracker = new JobTracker<>(parentJob, jobRepo);
            JobScheduler scheduler = new JobScheduler();

            JobResult parentJobResult = JobResult.builder().build();

            parentJobTracker.startTracking(JobStatus.UNINSTALL_IN_PROGRESS);
            try {
                Queue<EntandoBundleComponentJobEntity> uninstallJobs = createUninstallComponentJobs(parentJob, referenceJob);
                scheduler.queueAll(uninstallJobs);

                JobProgress uninstallProgress = new JobProgress(1.0 / uninstallJobs.size());

                Optional<EntandoBundleComponentJobEntity> optCompJob = scheduler.extractFromQueue();
                while (optCompJob.isPresent()) {
                    EntandoBundleComponentJobEntity uninstallJob = optCompJob.get();
                    JobTracker<EntandoBundleComponentJobEntity> cjt = trackExecution(uninstallJob, this::executeUninstall);
                    if (cjt.getJob().getStatus().equals(JobStatus.UNINSTALL_ERROR)) {
                        throw new EntandoComponentManagerException(parentJob.getComponentId()
                                + " uninstall can't proceed due to an error with one of the components");
                    }
                    uninstallProgress.increment();
                    parentJobTracker.setProgress(uninstallProgress.getValue());
                    scheduler.recordProcessedComponentJob(cjt.getJob());
                    optCompJob = scheduler.extractFromQueue();
                }

                installedComponentRepository.deleteById(parentJob.getComponentId());
                parentJobTracker.setProgress(1.0);

                parentJobResult.setStatus(JobStatus.UNINSTALL_COMPLETED);
                parentJobResult.clearException();
                log.info("Component " + parentJob.getComponentId() + " uninstalled successfully");

            } catch (Exception ex) {

                log.error("An error occurred while uninstalling component " + parentJob.getComponentId(), ex);
                parentJobResult.setStatus(JobStatus.UNINSTALL_ERROR);
                parentJobResult.setException(ex);
            }

            parentJobTracker.finishTracking(parentJobResult);
        });
    }

    private JobTracker<EntandoBundleComponentJobEntity> trackExecution(EntandoBundleComponentJobEntity cj,
            Function<Installable, JobResult> action) {
        JobTracker<EntandoBundleComponentJobEntity> cjt = new JobTracker<>(cj, compJobRepo);
        cjt.startTracking(JobStatus.UNINSTALL_IN_PROGRESS);
        JobResult result = action.apply(cj.getInstallable());
        cjt.finishTracking(result);
        return cjt;
    }

    private Queue<EntandoBundleComponentJobEntity> createUninstallComponentJobs(EntandoBundleJobEntity parentJob,
            EntandoBundleJobEntity referenceJob) {
        List<EntandoBundleComponentJobEntity> installJobs = compJobRepo.findAllByParentJob(referenceJob);
        return installJobs.stream()
                .map(cj -> {
                    Installable<?> i = processorMap.get(cj.getComponentType()).process(cj);
                    EntandoBundleComponentJobEntity uninstallCopy = EntandoBundleComponentJobEntity.getNewCopy(cj);
                    uninstallCopy.setParentJob(parentJob);
                    uninstallCopy.setStatus(JobStatus.UNINSTALL_CREATED);
                    uninstallCopy.setInstallable(i);
                    return uninstallCopy;
                })
                .sorted(Comparator.comparingInt(cj -> ((EntandoBundleComponentJobEntity) cj).getInstallable().getPriority()).reversed())
                .collect(Collectors.toCollection(ArrayDeque::new));
    }

    private JobResult executeUninstall(Installable<?> installable) {

        CompletableFuture<?> future = installable.uninstall();
        CompletableFuture<JobResult> uninstallResult = future
                .thenApply(vd -> {
                    log.debug("Installable '{}' uninstalled successfully", installable.getName());
                    return JobResult.builder().status(JobStatus.UNINSTALL_COMPLETED).build();
                }).exceptionally(th -> {
                    log.error("Installable '{}' had errors during uninstall", installable.getName(), th.getCause());
                    String message = getMeaningfulErrorMessage(th);
                    return JobResult.builder()
                            .status(JobStatus.UNINSTALL_ERROR)
                            .exception(new Exception(message))
                            .build();
                });

        return uninstallResult.join();
    }

}
