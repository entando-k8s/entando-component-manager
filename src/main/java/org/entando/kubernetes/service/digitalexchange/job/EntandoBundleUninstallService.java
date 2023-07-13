package org.entando.kubernetes.service.digitalexchange.job;

import static org.entando.kubernetes.model.bundle.installable.Installable.MAX_COMMON_SIZE_OF_STRINGS;
import static org.entando.kubernetes.service.digitalexchange.job.PostInitServiceImpl.ECR_ACTION_UNINSTALL;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayDeque;
import java.util.ArrayList;
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
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.client.model.EntandoCoreComponentDeleteRequest;
import org.entando.kubernetes.client.model.EntandoCoreComponentDeleteResponse;
import org.entando.kubernetes.client.model.EntandoCoreComponentDeleteResponse.EntandoCoreComponentDeleteStatus;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.digitalexchange.BundleNotInstalledException;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.exception.job.JobConflictException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.model.job.EntandoBundleEntity;
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
    private final @NonNull PostInitService postInitService;
    private final @NonNull Map<ComponentType, ComponentProcessor<?>> processorMap;
    private final @NonNull EntandoCoreClient entandoCoreClient;
    private final @NonNull ObjectMapper objectMapper;

    public EntandoBundleJobEntity uninstall(String componentId) {
        EntandoBundleEntity installedBundle = installedComponentRepository.findByBundleCode(componentId)
                .orElseThrow(() -> new BundleNotInstalledException("Bundle " + componentId + " is not installed"));

        verifyBundleUninstallIsAllowedOrThrow(componentId);

        verifyBundleUninstallIsPossibleOrThrow(installedBundle);

        return createAndSubmitUninstallJob(installedBundle.getJob());
    }

    private void verifyBundleUninstallIsAllowedOrThrow(String bundleCode) {
        Optional<Boolean> isAllowed = postInitService.isEcrActionAllowed(bundleCode, ECR_ACTION_UNINSTALL);
        if (isAllowed.isPresent() && Boolean.FALSE.equals(isAllowed.get())) {
            throw new InvalidBundleException(
                    String.format("Action '%s' not allowed for bundle '%s'", ECR_ACTION_UNINSTALL, bundleCode));
        }
    }


    private void verifyBundleUninstallIsPossibleOrThrow(EntandoBundleEntity bundle) {
        if (bundle.getJob() != null && bundle.getJob().getStatus().equals(JobStatus.INSTALL_COMPLETED)) {
            // verifyNoComponentInUseOrThrow(bundle);
            verifyNoConcurrentUninstallOrThrow(bundle);
        } else {
            throw new EntandoComponentManagerException(
                    "Installed bundle " + bundle.getId() + " associated with invalid job");
        }
    }

    private void verifyNoConcurrentUninstallOrThrow(EntandoBundleEntity bundle) {
        Optional<EntandoBundleJobEntity> lastJob = jobRepo.findFirstByComponentIdOrderByStartedAtDesc(
                bundle.getBundleCode());
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
                Queue<EntandoBundleComponentJobEntity> uninstallJobs =
                        createUninstallComponentJobs(parentJob, referenceJob);

                scheduler.queuePrimaryComponents(uninstallJobs);
                // added +1 because we have a step for each components
                // and finally we added a global step to do a single call to appEngine (this new call is the +1)
                JobProgress uninstallProgress = new JobProgress(1.0 / (uninstallJobs.size() + 1));

                Optional<EntandoBundleComponentJobEntity> optCompJob = scheduler.extractFromQueue();
                List<EntandoBundleComponentJobEntity> componentToUninstallFromAppEngine = new ArrayList<>();
                while (optCompJob.isPresent()) {
                    EntandoBundleComponentJobEntity uninstallJob = optCompJob.get();
                    JobTracker<EntandoBundleComponentJobEntity> cjt = trackExecution(uninstallJob,
                            this::executeUninstall);
                    if (cjt.getJob().getStatus().equals(JobStatus.UNINSTALL_ERROR)) {
                        throw new EntandoComponentManagerException(parentJob.getComponentId()
                                + " uninstall can't proceed due to an error with one of the components");
                    }

                    if (cjt.getJob().getInstallable().shouldUninstallFromAppEngine()) {
                        componentToUninstallFromAppEngine.add(cjt.getJob());
                    }

                    uninstallProgress.increment();
                    parentJobTracker.setProgress(uninstallProgress.getValue());
                    scheduler.recordProcessedComponentJob(cjt.getJob());
                    optCompJob = scheduler.extractFromQueue();
                }

                JobStatus finalStatus = executeDeleteFromAppEngine(componentToUninstallFromAppEngine, parentJob);
                parentJobResult.setStatus(finalStatus);

                installedComponentRepository.deleteByBundleCode(parentJob.getComponentId());

                uninstallProgress.increment();
                parentJobResult.clearException();
                parentJobResult.setProgress(1.0);

                log.info("Component '{}' uninstalled successfully", parentJob.getComponentId());

            } catch (Exception ex) {
                log.error("An error occurred while uninstalling component " + parentJob.getComponentId(), ex);
                parentJobResult.setStatus(JobStatus.UNINSTALL_ERROR);
                parentJobResult.setInstallException(ex);
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
                    uninstallCopy.setAction(cj.getAction());
                    uninstallCopy.setInstallable(i);
                    return uninstallCopy;
                })
                .sorted(Comparator
                        .comparingInt(cj -> ((EntandoBundleComponentJobEntity) cj).getInstallable().getPriority())
                        .reversed())
                .collect(Collectors.toCollection(ArrayDeque::new));
    }

    private JobResult executeUninstall(Installable<?> installable) {

        CompletableFuture<?> future = installable.uninstallFromEcr();
        CompletableFuture<JobResult> uninstallResult = future
                .thenApply(vd -> {
                    log.debug("Installable '{}' uninstalled successfully", installable.getName());
                    return JobResult.builder().status(JobStatus.UNINSTALL_COMPLETED).build();
                }).exceptionally(th -> {
                    log.error("Installable '{}' had errors during uninstall", installable.getName(), th.getCause());
                    String message = getMeaningfulErrorMessage(th, installable);
                    return JobResult.builder()
                            .status(JobStatus.UNINSTALL_ERROR)
                            .installException(new EntandoComponentManagerException(message))
                            .build();
                });

        return uninstallResult.join();
    }

    private JobStatus executeDeleteFromAppEngine(List<EntandoBundleComponentJobEntity> toDelete,
            EntandoBundleJobEntity parentJob) {
        EntandoCoreComponentDeleteResponse response = entandoCoreClient.deleteComponents(toDelete.stream()
                .map(EntandoCoreComponentDeleteRequest::fromEntity)
                .flatMap(Optional::stream)
                .collect(Collectors.toList()));

        switch (response.getStatus()) {
            case FAILURE:
                log.debug("All deletes are in error with response:'{}'", response);
                markError(parentJob, response);
                return JobStatus.UNINSTALL_ERROR;
            case PARTIAL_SUCCESS:
                log.debug("Partial deletes are in error with response:'{}'", response);
                markError(parentJob, response);
                return JobStatus.UNINSTALL_PARTIAL_COMPLETED;
            case SUCCESS:
            default:
                log.debug("All deletes ok with response:'{}'", response);
                return JobStatus.UNINSTALL_COMPLETED;
        }
    }

    private void markError(EntandoBundleJobEntity parentJob, EntandoCoreComponentDeleteResponse response) {
        try {
            String uninstallErrors = objectMapper.writeValueAsString(response.getComponents().stream()
                    .filter(c -> !EntandoCoreComponentDeleteStatus.SUCCESS.equals(c.getStatus())).collect(
                            Collectors.toList()));
            String valueToSave = StringUtils.truncate(uninstallErrors, MAX_COMMON_SIZE_OF_STRINGS);
            log.debug("save error inside parentJob:'{}'", valueToSave);
            parentJob.setUninstallErrors(valueToSave);

        } catch (JsonProcessingException ex) {
            log.error("with response:'{}' we had a json error", response, ex);
        }
    }
}
