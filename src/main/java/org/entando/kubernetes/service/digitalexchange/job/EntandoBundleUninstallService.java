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
import org.entando.kubernetes.client.model.EntandoCoreComponentDeleteResponse.EntandoCoreComponentDelete;
import org.entando.kubernetes.client.model.EntandoCoreComponentDeleteResponse.EntandoCoreComponentDeleteStatus;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.digitalexchange.BundleNotInstalledException;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.exception.job.JobConflictException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.bundle.usage.ComponentUsage;
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

    private static final String KEY_SEP = "_";

    public EntandoBundleJobEntity uninstall(String componentId) {
        EntandoBundleEntity installedBundle = installedComponentRepository.findByBundleCode(componentId)
                .orElseThrow(() -> new BundleNotInstalledException("Bundle " + componentId + " is not installed"));

        verifyBundleUninstallIsAllowedOrThrow(componentId);

        verifyBundleUninstallIsPossibleOrThrow(installedBundle);

        List<ComponentUsage> componentUsages = verifyAndGetListOfComponentUsageOrThrow(installedBundle);

        return createAndSubmitUninstallJob(installedBundle.getJob(), componentUsages);
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

    private List<ComponentUsage> verifyAndGetListOfComponentUsageOrThrow(EntandoBundleEntity bundle) {
        List<EntandoBundleComponentJobEntity> bundleComponentJobs = compJobRepo.findAllByParentJob(bundle.getJob());
        return usageService.getComponentsUsageDetails(bundleComponentJobs).stream()
                .filter(componentUsage -> {
                    if (componentUsage.getHasExternal()) {
                        throw new JobConflictException(String.format(
                                "Component '%s' of bundle '%s' contains external references and bundle can't be "
                                        + "uninstalled", componentUsage.getCode(), bundle.getId()));
                    }
                    return true;
                }).collect(Collectors.toList());
    }


    private EntandoBundleJobEntity createAndSubmitUninstallJob(EntandoBundleJobEntity lastAvailableJob,
            List<ComponentUsage> componentUsages) {

        EntandoBundleJobEntity uninstallJob = new EntandoBundleJobEntity();
        uninstallJob.setComponentId(lastAvailableJob.getComponentId());
        uninstallJob.setComponentName(lastAvailableJob.getComponentName());
        uninstallJob.setComponentVersion(lastAvailableJob.getComponentVersion());
        uninstallJob.setStatus(JobStatus.UNINSTALL_CREATED);
        uninstallJob.setProgress(0.0);
        EntandoBundleJobEntity savedJob = jobRepo.save(uninstallJob);

        submitUninstallAsync(uninstallJob, lastAvailableJob, componentUsages);

        return savedJob;
    }

    private void submitUninstallAsync(EntandoBundleJobEntity parentJob, EntandoBundleJobEntity referenceJob,
            List<ComponentUsage> componentUsages) {
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

                parentJobResult.clearException();
                JobStatus finalStatus = executeDeleteFromAppEngine(
                        keepNeededComponentsOnly(componentUsages, componentToUninstallFromAppEngine),
                        parentJobResult);
                parentJobResult.setStatus(finalStatus);

                if (JobStatus.UNINSTALL_COMPLETED.equals(finalStatus)) {
                    installedComponentRepository.deleteByBundleCode(parentJob.getComponentId());
                } else {
                    parentJobResult.setUninstallException(new EntandoComponentManagerException(parentJob.getUninstallErrors()));
                }

                uninstallProgress.increment();
                parentJobResult.setProgress(1.0);

                log.info("Uninstall operation completed with status '{}' for component:'{}'",
                        finalStatus,
                        parentJob.getComponentId());

            } catch (Exception ex) {
                log.error("An error occurred while uninstalling component " + parentJob.getComponentId(), ex);
                parentJobResult.setStatus(JobStatus.UNINSTALL_ERROR);
                parentJobResult.setUninstallException(ex);
            }

            parentJobTracker.finishTracking(parentJobResult);
        });
    }

    private List<EntandoBundleComponentJobEntity> keepNeededComponentsOnly(List<ComponentUsage> componentUsages,
            List<EntandoBundleComponentJobEntity> componentToUninstallFromAppEngine) {
        return componentToUninstallFromAppEngine
                .stream()
                .filter(cje -> filterComponentToDelete(cje, componentUsages))
                .collect(Collectors.toList());
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

    private boolean filterComponentToDelete(EntandoBundleComponentJobEntity cje,
            List<ComponentUsage> componentUsages) {
        return componentUsages.stream()
                .filter(componentUsage -> componentUsage.getType().equals(cje.getComponentType().getTypeName())
                        && componentUsage.getCode().equals(cje.getComponentId()))
                .findFirst()
                .map(ComponentUsage::isExist)
                .orElse(true);
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
            JobResult parentJobResult) {
        EntandoCoreComponentDeleteResponse response = entandoCoreClient.deleteComponents(toDelete.stream()
                .map(EntandoCoreComponentDeleteRequest::fromEntity)
                .flatMap(Optional::stream)
                .collect(Collectors.toList()));

        switch (response.getStatus()) {
            case FAILURE:
                log.debug("All deletes are in error with response:'{}'", response);
                markGlobalError(parentJobResult, response);
                markSingleErrors(toDelete,response);
                return JobStatus.UNINSTALL_ERROR;
            case PARTIAL_SUCCESS:
                log.debug("Partial deletes are in error with response:'{}'", response);
                markGlobalError(parentJobResult, response);
                markSingleErrors(toDelete,response);
                return JobStatus.UNINSTALL_PARTIAL_COMPLETED;
            case SUCCESS:
            default:
                log.debug("All deletes ok with response:'{}'", response);
                return JobStatus.UNINSTALL_COMPLETED;
        }
    }

    private void markSingleErrors(List<EntandoBundleComponentJobEntity> toDelete,
            EntandoCoreComponentDeleteResponse response) {
        // convenience map to speed up the search of an EntandoBundleComponentJobEntity related to a specific component
        // included in the app-engine response
        Map<String, EntandoBundleComponentJobEntity> toDeleteMap = toDelete.stream()
                .collect(Collectors.toMap(this::composeComponentJobEntityUniqueKey,Function.identity()));

        response.getComponents()
                .stream()
                .filter(c -> EntandoCoreComponentDeleteStatus.FAILURE.equals(c.getStatus()))
                .map(entandoCoreComponentDelete ->
                        // find the EntandoBundleComponentJobEntity, related to the ith component of the response,
                        // to set on it the appropriate error message
                        Optional.ofNullable(
                                        toDeleteMap.get(composeComponentDeleteUniqueKey(entandoCoreComponentDelete)))
                                .orElseThrow(() -> new IllegalStateException(
                                        String.format("Missing job for component '%s' of type '%s'",
                                                entandoCoreComponentDelete.getCode(),
                                                entandoCoreComponentDelete.getType())))
                ).forEach(cje -> {
                    // set the error message and code and save to DB
                    cje.setUninstallErrorMessage("Error in deleting component from app-engine");
                    cje.setUninstallErrorCode(100);
                    compJobRepo.save(cje);
                });
    }

    private String composeComponentDeleteUniqueKey(EntandoCoreComponentDelete entandoCoreComponentDelete) {
        if (entandoCoreComponentDelete == null
                || StringUtils.isEmpty(entandoCoreComponentDelete.getCode())
                || StringUtils.isEmpty(entandoCoreComponentDelete.getType())) {
            throw new IllegalArgumentException("Error in composing key from Entando Core Component Delete: "
                    + "element, code or type fields have null or empty values");
        }
        return entandoCoreComponentDelete.getCode() + KEY_SEP + entandoCoreComponentDelete.getType();
    }

    private String composeComponentJobEntityUniqueKey(EntandoBundleComponentJobEntity componentJobEntity) {
        if (componentJobEntity == null
                || StringUtils.isEmpty(componentJobEntity.getComponentId())
                || StringUtils.isEmpty(componentJobEntity.getComponentType().getTypeName())) {
            throw new IllegalArgumentException("Error in composing key from Entando Bundle Component Job Entity: "
                    + "element, code or type fields have null or empty values");
        }
        return componentJobEntity.getComponentId() + KEY_SEP + componentJobEntity.getComponentType().getTypeName();
    }

    private void markGlobalError(JobResult parentJobResult, EntandoCoreComponentDeleteResponse response) {
        try {
            String uninstallErrors = objectMapper.writeValueAsString(response.getComponents().stream()
                    .filter(c -> !EntandoCoreComponentDeleteStatus.SUCCESS.equals(c.getStatus())).collect(
                            Collectors.toList()));
            String valueToSave = StringUtils.truncate(uninstallErrors, MAX_COMMON_SIZE_OF_STRINGS);
            log.debug("save error inside parentJob:'{}'", valueToSave);
            parentJobResult.setUninstallException(new EntandoComponentManagerException(valueToSave));

        } catch (JsonProcessingException ex) {
            log.error("with response:'{}' we had a json error", response, ex);
        }
    }
}
