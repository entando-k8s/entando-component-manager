package org.entando.kubernetes.service.digitalexchange.job;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
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
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.digitalexchange.BundleNotInstalledException;
import org.entando.kubernetes.exception.job.JobConflictException;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloader;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.EntandoBundle;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleComponentJob;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleJob;
import org.entando.kubernetes.model.digitalexchange.JobResult;
import org.entando.kubernetes.model.digitalexchange.JobStatus;
import org.entando.kubernetes.model.digitalexchange.JobTracker;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleComponentUsageService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntandoBundleUninstallService {

    private final @NonNull EntandoBundleJobRepository jobRepository;
    private final @NonNull EntandoBundleComponentJobRepository componentRepository;
    private final @NonNull InstalledEntandoBundleRepository installedComponentRepository;
    private final @NonNull EntandoCoreClient engineService;
    private final @NonNull EntandoBundleComponentUsageService usageService;
    private final @NonNull KubernetesService k8sService;
    private final @NonNull BundleDownloader bundleDownloader;
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
        Optional<EntandoBundleJob> lastJob = jobRepository.findFirstByComponentIdOrderByStartedAtDesc(bundle.getId());
        EnumSet<JobStatus> concurrentUninstallJobStatus = EnumSet
                .of(JobStatus.UNINSTALL_IN_PROGRESS, JobStatus.UNINSTALL_CREATED);
        if (lastJob.isPresent() && lastJob.get().getStatus().isAny(concurrentUninstallJobStatus)) {
            throw new JobConflictException(
                    "A concurrent uninstall process for bundle " + bundle.getId() + " is running");
        }
    }

    private void verifyNoComponentInUseOrThrow(EntandoBundle bundle) {
        List<EntandoBundleComponentJob> bundleComponentJobs = componentRepository.findAllByJob(bundle.getJob());
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
        uninstallJob.setStartedAt(LocalDateTime.now());
        uninstallJob.setStatus(JobStatus.UNINSTALL_CREATED);
        uninstallJob.setProgress(0.0);
        EntandoBundleJob savedJob = jobRepository.save(uninstallJob);

        submitUninstallAsync(uninstallJob, lastAvailableJob);

        return savedJob;
    }

    private void submitUninstallAsync(EntandoBundleJob job, EntandoBundleJob referenceJob) {
        CompletableFuture.runAsync(() -> {
            JobTracker tracker = new JobTracker();
            tracker.setJob(job);
            tracker.getJob().setStatus(JobStatus.UNINSTALL_IN_PROGRESS);
            jobRepository.save(tracker.getJob());

            try {
                List<EntandoBundleComponentJob> uninstallJobs = createUninstallComponentJobs(referenceJob, tracker.getJob());
                tracker.queueAllComponentJobs(uninstallJobs);

                Optional<EntandoBundleComponentJob> ocj = tracker.extractNextComponentJobToProcess();
                while(ocj.isPresent()) {
                    EntandoBundleComponentJob componentJob = ocj.get();
                    componentJob.setStatus(JobStatus.UNINSTALL_IN_PROGRESS);
                    componentRepository.save(componentJob);
                    Installable installable = componentJob.getInstallable();
                    JobResult operationResult = executeUninstall(installable);
                    componentJob.setStatus(operationResult.getStatus());
                    operationResult.getException().ifPresent(ex -> componentJob.setErrorMessage(ex.getMessage()));
                    componentRepository.save(componentJob);
                    tracker.recordProcessedComponentJob(componentJob);
                    if (operationResult.getStatus().equals(JobStatus.UNINSTALL_ERROR)) {
                        throw new EntandoComponentManagerException(tracker.getJob().getComponentId()
                                + " uninstall can't proceed due to an error with one of the components");
                    }
                    ocj = tracker.extractNextComponentJobToProcess();
                }

                installedComponentRepository.deleteById(tracker.getJob().getComponentId());
                tracker.getJob().setStatus(JobStatus.UNINSTALL_COMPLETED);
                log.info("Component " + tracker.getJob().getComponentId() + " uninstalled successfully");

            } catch (Exception ex) {

                log.error("An error occurred while uninstalling component " + tracker.getJob().getComponentId(), ex);
                tracker.getJob().setStatus(JobStatus.UNINSTALL_ERROR);
            }

            jobRepository.save(tracker.getJob());
        });
    }

    private List<EntandoBundleComponentJob> createUninstallComponentJobs(EntandoBundleJob referenceJob, EntandoBundleJob currentJob) {
        List<EntandoBundleComponentJob> installJobs = componentRepository.findAllByJob(referenceJob);
        return installJobs.stream()
                .map(cj -> {
                    EntandoBundleComponentJob uninstallJob = cj.duplicate();
                    uninstallJob.setJob(currentJob);
                    Installable uninstallJobWorker = processorMap.get(uninstallJob.getComponentType()).process(uninstallJob);
                    uninstallJob.setInstallable(uninstallJobWorker);
                    return uninstallJob;
                })
                .sorted(Comparator.comparingInt((EntandoBundleComponentJob cj) ->
                        cj.getInstallable().getInstallPriority().getPriority()).reversed())
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
                    String message = th.getMessage();
                    if (th.getCause() != null) {
                        message = th.getCause().getMessage();
                        if (th.getCause() instanceof HttpClientErrorException) {
                            HttpClientErrorException httpException = (HttpClientErrorException) th.getCause();
                            message =
                                    httpException.getMessage() + "\n" + httpException.getResponseBodyAsString();
                        }
                    }
                    return JobResult.builder()
                            .status(JobStatus.UNINSTALL_ERROR)
                            .exception(new Exception(message))
                            .build();
                });

        return uninstallResult.join();
    }

}
