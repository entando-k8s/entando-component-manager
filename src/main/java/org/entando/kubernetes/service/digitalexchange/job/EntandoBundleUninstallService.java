package org.entando.kubernetes.service.digitalexchange.job;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.digitalexchange.BundleNotInstalledException;
import org.entando.kubernetes.exception.job.JobConflictException;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.digitalexchange.*;
import org.entando.kubernetes.model.job.*;
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
        EntandoBundle installedBundle = installedComponentRepository.findById(componentId)
                .orElseThrow(() -> new BundleNotInstalledException("Bundle " + componentId + " is not installed"));

        verifyBundleUninstallIsPossibleOrThrow(installedBundle);

        return createAndSubmitUninstallJob(installedBundle.getJob());
    }

    private void verifyBundleUninstallIsPossibleOrThrow(EntandoBundle bundle) {
        if (bundle.getJob() != null && bundle.getJob().getStatus().equals(JobStatus.INSTALL_COMPLETED)) {
            verifyNoComponentInUseOrThrow(bundle);
            verifyNoConcurrentUninstallOrThrow(bundle);
        } else {
            throw new EntandoComponentManagerException(
                    "Installed bundle " + bundle.getId() + " associated with invalid job");
        }
    }

    private void verifyNoConcurrentUninstallOrThrow(EntandoBundle bundle) {
        Optional<EntandoBundleJob> lastJob = jobRepo.findFirstByComponentIdOrderByStartedAtDesc(bundle.getId());
        EnumSet<JobStatus> concurrentUninstallJobStatus = EnumSet
                .of(JobStatus.UNINSTALL_IN_PROGRESS, JobStatus.UNINSTALL_CREATED);
        if (lastJob.isPresent() && lastJob.get().getStatus().isAny(concurrentUninstallJobStatus)) {
            throw new JobConflictException(
                    "A concurrent uninstall process for bundle " + bundle.getId() + " is running");
        }
    }

    private void verifyNoComponentInUseOrThrow(EntandoBundle bundle) {
        List<EntandoBundleComponentJob> bundleComponentJobs = compJobRepo.findAllByJob(bundle.getJob());
        if (bundleComponentJobs.stream()
                .anyMatch(e -> usageService.getUsage(e.getComponentType(), e.getName()).getUsage() > 0)) {
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
            JobResult parentJobResult = JobResult.builder().status(JobStatus.UNINSTALL_IN_PROGRESS).build();
            JobScheduler scheduler = new JobScheduler();
            parentJob.setStartedAt(LocalDateTime.now());
            parentJob.setStatus(parentJobResult.getStatus());
            jobRepo.save(parentJob);

            try {
                List<Installable> uninstallJobs = createUninstallComponentJobs(referenceJob);
                scheduler.queueAll(uninstallJobs);

                Optional<Installable> optInst = scheduler.extractFromQueue();
                while(optInst.isPresent()) {
                    JobTracker cjt = processComponentJob(optInst.get(), parentJob);
                    if (cjt.getTrackedJob().getStatus().equals(JobStatus.UNINSTALL_ERROR)) {
                        throw new EntandoComponentManagerException(parentJob.getComponentId()
                                + " uninstall can't proceed due to an error with one of the components");
                    }
                    scheduler.recordProcessedComponentJob(cjt);
                    optInst = scheduler.extractFromQueue();
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

            parentJob.setFinishedAt(LocalDateTime.now());
            parentJob.setStatus(parentJobResult.getStatus());
            if (parentJobResult.hasException()) {
                parentJob.setErrorMessage(parentJobResult.getErrorMessage());
            }
            jobRepo.save(parentJob);
        });
    }

    private JobTracker processComponentJob(Installable i, EntandoBundleJob parentJob) {
        JobTracker cjt = new JobTracker(i, parentJob, compJobRepo);
        cjt.startTracking(JobStatus.UNINSTALL_IN_PROGRESS);
        JobResult operationResult = executeUninstall(i);
        cjt.stopTrackingTime(operationResult);
        return cjt;
    }

    private List<Installable> createUninstallComponentJobs(EntandoBundleJob referenceJob) {
        List<EntandoBundleComponentJob> installJobs = compJobRepo.findAllByJob(referenceJob);
        return installJobs.stream()
                .map(cj -> processorMap.get(cj.getComponentType()).process(cj))
                .sorted(Comparator.comparingInt(i -> ((Installable)i).getPriority()).reversed())
                .collect(Collectors.toList());
    }

    private JobResult executeUninstall(Installable installable) {

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
