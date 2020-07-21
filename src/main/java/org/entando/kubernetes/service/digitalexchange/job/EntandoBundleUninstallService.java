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
import org.entando.kubernetes.model.job.EntandoBundleComponentJob;
import org.entando.kubernetes.model.job.EntandoBundleJob;
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
    private final @NonNull Map<ComponentType, ComponentProcessor> processorMap;

    public EntandoBundleJob uninstall(String componentId) {
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
        Optional<EntandoBundleJob> lastJob = jobRepo.findFirstByComponentIdOrderByStartedAtDesc(bundle.getId());
        EnumSet<JobStatus> concurrentUninstallJobStatus = EnumSet
                .of(JobStatus.UNINSTALL_IN_PROGRESS, JobStatus.UNINSTALL_CREATED);
        if (lastJob.isPresent() && lastJob.get().getStatus().isAny(concurrentUninstallJobStatus)) {
            throw new JobConflictException(
                    "A concurrent uninstall process for bundle " + bundle.getId() + " is running");
        }
    }

    private void verifyNoComponentInUseOrThrow(EntandoBundleEntity bundle) {
        List<EntandoBundleComponentJob> bundleComponentJobs = compJobRepo.findAllByParentJob(bundle.getJob());
        if (bundleComponentJobs.stream()
                .anyMatch(e -> usageService.getUsage(e.getComponentType(), e.getComponentId()).getUsage() > 0)) {
            throw new JobConflictException(
                    "Some of bundle " + bundle.getId() + " components are in use and bundle can't be uninstalled");
        }
    }

    private EntandoBundleJob createAndSubmitUninstallJob(EntandoBundleJob lastAvailableJob) {

        EntandoBundleJob uninstallJob = new EntandoBundleJob();
        uninstallJob.setComponentId(lastAvailableJob.getComponentId());
        uninstallJob.setComponentName(lastAvailableJob.getComponentName());
        uninstallJob.setComponentVersion(lastAvailableJob.getComponentVersion());
        uninstallJob.setStatus(JobStatus.UNINSTALL_CREATED);
        uninstallJob.setProgress(0.0);
        EntandoBundleJob savedJob = jobRepo.save(uninstallJob);

        submitUninstallAsync(uninstallJob, lastAvailableJob);

        return savedJob;
    }

    private void submitUninstallAsync(EntandoBundleJob parentJob, EntandoBundleJob referenceJob) {
        CompletableFuture.runAsync(() -> {
            JobTracker<EntandoBundleJob> parentJobTracker = new JobTracker<>(parentJob, jobRepo);
            JobScheduler scheduler = new JobScheduler();

            JobResult parentJobResult = JobResult.builder().status(JobStatus.UNINSTALL_IN_PROGRESS).build();
            parentJobTracker.startTracking(parentJobResult.getStatus());

            try {
                Queue<EntandoBundleComponentJob> uninstallJobs = createUninstallComponentJobs(parentJob, referenceJob);
                scheduler.queueAll(uninstallJobs);

                Optional<EntandoBundleComponentJob> optCompJob = scheduler.extractFromQueue();
                while(optCompJob.isPresent()) {
                    EntandoBundleComponentJob uninstallJob = optCompJob.get();
                    JobTracker<EntandoBundleComponentJob> cjt = trackExecution(uninstallJob, this::executeUninstall);
                    if (cjt.getJob().getStatus().equals(JobStatus.UNINSTALL_ERROR)) {
                        throw new EntandoComponentManagerException(parentJob.getComponentId()
                                + " uninstall can't proceed due to an error with one of the components");
                    }
                    scheduler.recordProcessedComponentJob(cjt.getJob());
                    optCompJob = scheduler.extractFromQueue();
                }

                installedComponentRepository.deleteById(parentJob.getComponentId());
                parentJobResult.setStatus(JobStatus.UNINSTALL_COMPLETED);
                parentJobResult.clearException();
                log.info("Component " + parentJob.getComponentId() + " uninstalled successfully");

            } catch (Exception ex) {

                log.error("An error occurred while uninstalling component " + parentJob.getComponentId(), ex);
                parentJobResult.setStatus(JobStatus.UNINSTALL_ERROR);
                parentJobResult.setException(ex);
            }

            parentJobTracker.stopTrackingTime(parentJobResult);
        });
    }

    private JobTracker<EntandoBundleComponentJob> trackExecution(EntandoBundleComponentJob cj, Function<Installable, JobResult> action) {
        JobTracker<EntandoBundleComponentJob> cjt = new JobTracker<>(cj, compJobRepo);
        cjt.startTracking(JobStatus.UNINSTALL_IN_PROGRESS);
        JobResult result = action.apply(cj.getInstallable());
        cjt.stopTrackingTime(result);
        return cjt;
    }

    private Queue<EntandoBundleComponentJob> createUninstallComponentJobs(EntandoBundleJob parentJob, EntandoBundleJob referenceJob) {
        List<EntandoBundleComponentJob> installJobs = compJobRepo.findAllByParentJob(referenceJob);
        return installJobs.stream()
                .map(cj -> {
                    Installable<?> i = processorMap.get(cj.getComponentType()).process(cj);
                    EntandoBundleComponentJob uninstallCopy = EntandoBundleComponentJob.getNewCopy(cj);
                    uninstallCopy.setParentJob(parentJob);
                    uninstallCopy.setStatus(JobStatus.UNINSTALL_CREATED);
                    uninstallCopy.setInstallable(i);
                    return uninstallCopy;
                })
                .sorted(Comparator.comparingInt(cj -> ((EntandoBundleComponentJob) cj).getInstallable().getPriority()).reversed())
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
