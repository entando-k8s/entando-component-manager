package org.entando.kubernetes.service.digitalexchange.job;

import static org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.TYPE_WIDGET_APPBUILDER;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.client.model.AnalysisReport;
import org.entando.kubernetes.client.model.EntandoCoreComponentDeleteRequest;
import org.entando.kubernetes.client.model.EntandoCoreComponentDeleteResponse;
import org.entando.kubernetes.client.model.assembler.InstallPlanAssembler;
import org.entando.kubernetes.config.tenant.thread.ContextCompletableFuture;
import org.entando.kubernetes.controller.digitalexchange.job.model.ComponentInstallPlan;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.digitalexchange.ReportAnalysisException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.Descriptor;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloader;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderFactory;
import org.entando.kubernetes.model.bundle.downloader.DownloadedBundle;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.bundle.processor.PluginProcessor;
import org.entando.kubernetes.model.bundle.processor.WidgetProcessor;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.reportable.AnalysisReportFunction;
import org.entando.kubernetes.model.bundle.reportable.Reportable;
import org.entando.kubernetes.model.bundle.reportable.ReportableComponentProcessor;
import org.entando.kubernetes.model.bundle.reportable.ReportableRemoteHandler;
import org.entando.kubernetes.model.bundle.usage.ComponentUsage;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
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
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.service.digitalexchange.EntandoDeBundleComposer;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleComponentUsageService;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleService;
import org.entando.kubernetes.service.digitalexchange.concurrency.BundleOperationsConcurrencyManager;
import org.entando.kubernetes.validator.descriptor.BundleDescriptorValidator;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntandoBundleInstallService implements EntandoBundleJobExecutor {

    public static final boolean PERFORM_CONCURRENT_CHECKS = true;
    public static final boolean DONT_PERFORM_CONCURRENT_CHECKS = false;

    public enum Operation {
        ROLLBACK,
        UNINSTALL
    }

    private final @NonNull EntandoBundleService bundleService;
    private final @NonNull BundleDownloaderFactory downloaderFactory;
    private final @NonNull EntandoBundleJobRepository jobRepo;
    private final @NonNull EntandoBundleComponentJobRepository compJobRepo;
    private final @NonNull InstalledEntandoBundleRepository bundleRepository;
    private final @NonNull Map<ComponentType, ComponentProcessor<?>> processorMap;
    private final @NonNull List<ReportableComponentProcessor> reportableComponentProcessorList;
    private final @NonNull Map<ReportableRemoteHandler, AnalysisReportFunction> analysisReportStrategies;
    private final @NonNull BundleOperationsConcurrencyManager bundleOperationsConcurrencyManager;
    private final @NonNull BundleDescriptorValidator bundleDescriptorValidator;
    private final @NonNull EntandoCoreClient entandoCoreClient;
    private final @NonNull BundleUninstallUtility bundleUninstallUtility;
    private final @NonNull EntandoBundleComponentUsageService usageService;

    private final ObjectMapper objectMapper = new ObjectMapper();


    /**
     * perform the install analysis if there isn't another running bundle operation. return the InstallPlan generated by
     * the result of the analysis
     *
     * @param bundle                  the bundle to analyze
     * @param tag                     the tag of the bundle to analyze
     * @param performConcurrencyCheck if true it check for possible concurrent operations
     * @return the generated {@link InstallPlan}
     */
    public InstallPlan generateInstallPlan(EntandoDeBundle bundle, EntandoDeBundleTag tag,
            boolean performConcurrencyCheck) {

        if (performConcurrencyCheck) {
            this.bundleOperationsConcurrencyManager.throwIfAnotherOperationIsRunningOrStartOperation();
        }

        InstallPlan installPlan;
        BundleDownloader bundleDownloader = downloaderFactory.newDownloader(tag);

        try {
            BundleReader bundleReader =
                    this.downloadBundleAndGetBundleReader(bundleDownloader, bundle, tag);

            Map<ReportableRemoteHandler, List<Reportable>> reportableByHandler =
                    this.getReportableComponentsByRemoteHandler(bundleReader);

            List<CompletableFuture<AnalysisReport>> futureList = reportableByHandler.keySet().stream()
                    // for each remote handler => get whole analysis report async
                    .map(key -> CompletableFuture.supplyAsync(() -> analysisReportStrategies.get(key)
                            .getAnalysisReport(reportableByHandler.get(key))))
                    .collect(Collectors.toList());

            // why using separate streams https://stackoverflow.com/questions/58700578/why-is-completablefuture-join-get-faster-in-separate-streams-than-using-one-stre

            try {
                installPlan = futureList.stream().map(CompletableFuture::join)
                        .map(InstallPlanAssembler::toInstallPlan)
                        .reduce(InstallPlan::merge)
                        .orElseThrow(() -> new ReportAnalysisException(String.format(
                                "An error occurred during the install plan generation for the bundle %s with tag %s",
                                bundle.getMetadata().getName(), tag.getVersion())));
            } catch (CompletionException e) {
                throw e.getCause() instanceof ReportAnalysisException
                        ? (ReportAnalysisException) e.getCause()
                        : e;
            }

        } finally {
            if (performConcurrencyCheck) {
                this.bundleOperationsConcurrencyManager.operationTerminated();
            }
            bundleDownloader.cleanTargetDirectory();
        }

        return installPlan;
    }

    public EntandoBundleJobEntity install(EntandoDeBundle bundle, EntandoDeBundleTag tag) {
        return this.install(bundle, tag, InstallAction.CREATE);
    }

    public EntandoBundleJobEntity install(EntandoDeBundle bundle, EntandoDeBundleTag tag,
            InstallAction conflictStrategy) {

        this.bundleOperationsConcurrencyManager.throwIfAnotherOperationIsRunningOrStartOperation();

        try {

            // Only request analysis report if provided conflict strategy
            final InstallPlan installPlan = conflictStrategy != InstallAction.CREATE
                    ? generateInstallPlan(bundle, tag, EntandoBundleInstallService.DONT_PERFORM_CONCURRENT_CHECKS)
                    : new InstallPlan();

            EntandoBundleJobEntity job = createInstallJob(bundle, tag, installPlan);
            submitInstallAsync(job, bundle, tag, conflictStrategy, installPlan)
                    .thenAccept(unused -> this.bundleOperationsConcurrencyManager.operationTerminated());

            return job;

        } catch (Exception e) {
            // release concurrency manager's lock
            this.bundleOperationsConcurrencyManager.operationTerminated();
            throw e;
        }
    }


    public EntandoBundleJobEntity installWithInstallPlan(EntandoDeBundle bundle, EntandoDeBundleTag tag,
            InstallPlan installPlan) {

        this.bundleOperationsConcurrencyManager.throwIfAnotherOperationIsRunningOrStartOperation();

        try {
            EntandoBundleJobEntity job = createInstallJob(bundle, tag, installPlan);
            submitInstallAsync(job, bundle, tag, InstallAction.CREATE, installPlan)
                    .thenAccept(unused -> this.bundleOperationsConcurrencyManager.operationTerminated());

            return job;

        } catch (Exception e) {
            // release concurrency manager's lock
            this.bundleOperationsConcurrencyManager.operationTerminated();
            throw e;
        }
    }

    private EntandoBundleJobEntity createInstallJob(EntandoDeBundle bundle, EntandoDeBundleTag tag,
            InstallPlan installPlan) {

        final EntandoBundleJobEntity job = new EntandoBundleJobEntity();

        job.setComponentId(bundle.getMetadata().getName());
        job.setComponentName(bundle.getSpec().getDetails().getName());
        job.setComponentVersion(tag.getVersion());
        job.setProgress(0);
        job.setStatus(JobStatus.INSTALL_CREATED);

        if (installPlan != null) {
            job.setCustomInstallation(installPlan.isCustomInstallation());

            try {
                job.setInstallPlan(objectMapper.writeValueAsString(installPlan));
            } catch (JsonProcessingException e) {
                log.error("Error converting the received install plan to string", e);
                job.setInstallPlan(null);
            }
        }

        EntandoBundleJobEntity createdJob = jobRepo.save(job);
        log.debug("New installation job created " + job.toString());
        createdJob.getComponentId();
        return createdJob;
    }

    private CompletableFuture<Void> submitInstallAsync(EntandoBundleJobEntity parentJob, EntandoDeBundle bundle,
            EntandoDeBundleTag tag, InstallAction conflictStrategy, InstallPlan installPlan) {

        return ContextCompletableFuture.runAsyncWithContext(() -> {
            log.info("Started new install job for component " + parentJob.getComponentId() + "@" + tag.getVersion());

            JobTracker<EntandoBundleJobEntity> parentJobTracker = new JobTracker<>(parentJob, jobRepo);
            JobResult parentJobResult = JobResult.builder().build();
            JobScheduler scheduler = new JobScheduler();
            BundleDownloader bundleDownloader = downloaderFactory.newDownloader(tag);

            parentJobTracker.startTracking(JobStatus.INSTALL_IN_PROGRESS);
            try {
                // PREPARES THE JOBS
                BundleReader bundleReader = this.downloadBundleAndGetBundleReader(bundleDownloader, bundle, tag);

                Queue<Installable> bundleInstallableComponents = getBundleInstallableComponents(bundleReader,
                        conflictStrategy, installPlan);

                Queue<EntandoBundleComponentJobEntity> componentJobQueue = bundleInstallableComponents.stream()
                        .map(i -> {
                            EntandoBundleComponentJobEntity cj = new EntandoBundleComponentJobEntity();
                            cj.setParentJob(parentJob);
                            cj.setComponentType(i.getComponentType());
                            cj.setComponentId(i.getName());
                            cj.setChecksum(i.getChecksum());
                            cj.setInstallable(i);
                            cj.setAction(i.getAction());
                            return cj;
                        }).collect(Collectors.toCollection(ArrayDeque::new));

                scheduler.queuePrimaryComponents(componentJobQueue);

                JobProgress installProgress = new JobProgress(1.0 / componentJobQueue.size());

                // ITERATES AND EXECUTES THE JOBS

                Optional<EntandoBundleComponentJobEntity> optCompJob = scheduler.extractFromQueue();
                while (optCompJob.isPresent()) {
                    EntandoBundleComponentJobEntity installJob = optCompJob.get();

                    JobTracker<EntandoBundleComponentJobEntity> tracker = trackExecution(
                            installJob, this::executeInstall, JobStatus.INSTALL_IN_PROGRESS
                    );

                    scheduler.recordProcessedComponentJob(tracker.getJob());
                    if (tracker.getJob().getStatus().equals(JobStatus.INSTALL_ERROR)) {
                        parentJobResult.setInstallException(new EntandoComponentManagerException(
                                tracker.getJob().getInstallErrorMessage()));
                        break;
                    }
                    installProgress.increment();
                    parentJobTracker.setProgress(installProgress.getValue());
                    optCompJob = scheduler.extractFromQueue();
                }

                // EVALUATES THE JOBS RESULTS

                if (parentJobResult.hasException()) {
                    log.error("An error occurred during component installation --- {}",
                            parentJobResult.getInstallError());
                    log.warn("Rolling installation of bundle " + parentJob.getComponentId() + "@" + parentJob
                            .getComponentVersion());
                    parentJobResult = rollback(scheduler, parentJobResult);
                } else {
                    String warnings = uninstallOrphanedComponents(parentJob, bundle, conflictStrategy, installPlan);
                    saveAsInstalledBundle(bundle, parentJob, bundleReader.readBundleDescriptor(),
                            bundleReader.getBundleDigest());
                    parentJobResult.setInstallWarnings(warnings);
                    parentJobResult.clearException();
                    parentJobResult.setStatus(JobStatus.INSTALL_COMPLETED);
                    parentJobResult.setProgress(1.0);
                    log.info("Bundle installed correctly");

                }

            } catch (Exception e) {
                log.error("An error occurred while reading components from the bundle", e);
                parentJobResult.setStatus(JobStatus.INSTALL_ERROR);
                parentJobResult.setInstallException(e);
            }

            parentJobTracker.finishTracking(parentJobResult);
            bundleDownloader.cleanTargetDirectory();
        });
    }

    private String uninstallOrphanedComponents(EntandoBundleJobEntity parentJob, EntandoDeBundle bundle,
                                            InstallAction conflictStrategy, InstallPlan installPlan) {
        try {
            Optional<EntandoBundleJobEntity> latestBundleJob = jobRepo
                    .findFirstByComponentIdAndStatusOrderByStartedAtDesc(parentJob.getComponentId(), JobStatus.INSTALL_COMPLETED);
            String version = latestBundleJob.get().getComponentVersion();
            EntandoDeBundleTag latestTag = BundleUtilities.getBundleTagOrFail(bundle, version);
            BundleDownloader latestBundleDownloader = downloaderFactory.newDownloader(latestTag);
            BundleReader latestBundleReader = this.downloadBundleAndGetBundleReader(latestBundleDownloader, bundle, latestTag);

            if (latestBundleJob.isPresent()) {
                String latestInstallPlanString = latestBundleJob.get().getInstallPlan();
                InstallPlan latestInstallPlan = objectMapper.readValue(latestInstallPlanString, InstallPlan.class);
                InstallPlan diff = calculateDiffInstallPlan(installPlan, latestInstallPlan);

                Queue<Installable> bundleInstallableComponentsDiff = getBundleInstallableComponents(latestBundleReader, conflictStrategy, diff);


                Queue<EntandoBundleComponentJobEntity> componentJobQueueDiff = getBundleComponentJobEntityQueue(parentJob, bundleInstallableComponentsDiff);

                List<ComponentUsage> componentUsageList = getComponentUsageList(componentJobQueueDiff);

                JobScheduler schedulerDiff = new JobScheduler();
                schedulerDiff.queuePrimaryComponents(componentJobQueueDiff);
                String bundleRootFolder = BundleUtilities.composeBundleResourceRootFolter(latestBundleReader);

                return uninstallDiff(bundleRootFolder, schedulerDiff, componentUsageList);

            } else {
                String warnings = "The uninstallation of orphaned components is not necessary since the bundle is not installed on the system";
                log.debug(warnings);
                return warnings;
            }
        } catch (Exception e) {
            String error = "The uninstallation of orphaned components failed with error " + e;
            log.error(error);
            return error;
        }

    }

    private List<ComponentUsage> getComponentUsageList(Queue<EntandoBundleComponentJobEntity> componentJobQueueDiff) {
        List<EntandoBundleComponentJobEntity> componentJobListDiff = new ArrayList<>(componentJobQueueDiff);
        List<ComponentUsage> componentUsageList = usageService.getComponentsUsageDetails(componentJobListDiff).stream()
                .filter(componentUsage -> {
                    if (!componentUsage.isExist() || componentUsage.getHasExternal()) {
                        log.debug(String.format(
                                "Component '%s' is not found or contains external references so it can't be uninstalled",
                                componentUsage.getCode()));
                        return false;
                    }
                    return true;
                }).collect(Collectors.toList());
        return componentUsageList;
    }

    private static Queue<EntandoBundleComponentJobEntity> getBundleComponentJobEntityQueue(EntandoBundleJobEntity parentJob,
                                                                                           Queue<Installable> bundleInstallableComponentsDiff) {
        Queue<EntandoBundleComponentJobEntity> componentJobQueueDiff = bundleInstallableComponentsDiff.stream()
                .filter(i -> {
                    if (i.getRepresentation() instanceof WidgetDescriptor) {
                        WidgetDescriptor wd = (WidgetDescriptor) i.getRepresentation();
                        return TYPE_WIDGET_APPBUILDER.equals(wd.getType());
                    }
                    return false;
                })
                .map(i -> {
                    EntandoBundleComponentJobEntity cj = new EntandoBundleComponentJobEntity();
                    cj.setParentJob(parentJob);
                    cj.setComponentType(i.getComponentType());
                    cj.setComponentId(i.getName());
                    cj.setChecksum(i.getChecksum());
                    cj.setInstallable(i);
                    cj.setAction(i.getAction());
                    return cj;
                })
                .collect(Collectors.toCollection(ArrayDeque::new));
        return componentJobQueueDiff;
    }

    private JobResult rollback(JobScheduler scheduler, JobResult result) {
        JobScheduler rollbackScheduler = scheduler.createRollbackScheduler();
        try {
            List<EntandoBundleComponentJobEntity> componentToUninstallFromAppEngine = new ArrayList<>();
            Optional<EntandoBundleComponentJobEntity> optCompJob = rollbackScheduler.extractFromQueue();
            while (optCompJob.isPresent()) {
                EntandoBundleComponentJobEntity rollbackJob = optCompJob.get();
                if (isUninstallable(rollbackJob)) {
                    JobTracker<EntandoBundleComponentJobEntity> tracker = trackExecution(rollbackJob,
                            installable -> executeUninstallFromEcr(installable, Operation.ROLLBACK), JobStatus.INSTALL_IN_PROGRESS);
                    if (tracker.getJob().getStatus().equals(JobStatus.INSTALL_ROLLBACK_ERROR)) {
                        throw new EntandoComponentManagerException(
                                rollbackJob.getComponentType() + " " + rollbackJob.getComponentId()
                                        + " rollback can't proceed due to an error with one of the components");
                    }
                    rollbackScheduler.recordProcessedComponentJob(tracker.getJob());

                    if (tracker.getJob().getInstallable().shouldUninstallFromAppEngine()) {
                        componentToUninstallFromAppEngine.add(tracker.getJob());
                    }

                }
                optCompJob = rollbackScheduler.extractFromQueue();
            }

            // remove from appEngine
            JobStatus finalStatus = executeDeleteFromAppEngine(componentToUninstallFromAppEngine, result, Operation.ROLLBACK);
            result.setStatus(finalStatus);
            log.info("Rollback operation completed");

        } catch (Exception rollbackException) {
            log.error("An error occurred during component rollback", rollbackException);
            result.setStatus(JobStatus.INSTALL_ERROR);
            result.setRollbackException(rollbackException);
        }
        return result;
    }

    private String uninstallDiff(String bundleRootName, JobScheduler uninstallDiffScheduler, List<ComponentUsage> componentUsageList)
            throws JsonProcessingException {

        Map<String, JobStatus> componentsFeedbackCM = new HashMap<>();
        Map<String, JobStatus> componentsFeedbackAppEngine = new HashMap<>();
        Map<String, Map<String, JobStatus>> componentsFeedback = new HashMap<>();
        final String uninstallationCMKey = "uninstallationOnCM";
        final String uninstallationAppEngineKey = "uninstallationOnAppEngine";

        List<EntandoBundleComponentJobEntity> componentToUninstallFromAppEngine = new ArrayList<>();

        try {
            Optional<EntandoBundleComponentJobEntity> optCompJob = uninstallDiffScheduler.extractFromQueue();

            while (optCompJob.isPresent()) {
                EntandoBundleComponentJobEntity uninstallDiffJob = optCompJob.get();
                if (bundleRootName.equals(uninstallDiffJob.getComponentId()) && ComponentType.DIRECTORY.equals(uninstallDiffJob.getComponentType())) {
                    // root directory
                    log.info("skip deletion for root folder");
                    optCompJob = uninstallDiffScheduler.extractFromQueue();
                    continue;
                }

                log.debug("Start uninstalling orphaned components on CM");
                JobTracker<EntandoBundleComponentJobEntity> tracker = trackExecution(uninstallDiffJob,
                        installable -> executeUninstallFromEcr(installable, Operation.UNINSTALL), JobStatus.UNINSTALL_IN_PROGRESS);

                componentsFeedbackCM.put(optCompJob.get().getComponentId(), tracker.getJob().getStatus());

                if (tracker.getJob().getStatus().equals(JobStatus.UNINSTALL_ERROR)) {
                    log.debug("uninstall of orphaned components on cm can't proceed due to an error with one of the components");
                    componentsFeedback.put(uninstallationCMKey, componentsFeedbackCM);
                    componentsFeedback.put(uninstallationAppEngineKey, componentsFeedbackAppEngine);
                    return objectMapper.writeValueAsString(componentsFeedback);
                }

                uninstallDiffScheduler.recordProcessedComponentJob(tracker.getJob());

                componentUsageList.stream()
                        .filter(component -> component.getCode().equals(tracker.getJob().getComponentId()))
                        .findFirst()
                        .ifPresent(matchingComponent -> componentToUninstallFromAppEngine.add(tracker.getJob()));

                optCompJob = uninstallDiffScheduler.extractFromQueue();

            }

            // remove from appEngine
            log.debug("Start uninstalling orphaned components on AppEngine");
            executeDeleteFromAppEngine(componentToUninstallFromAppEngine, Operation.UNINSTALL);
            log.info("Uninstall operation completed");

            componentsFeedbackAppEngine = componentToUninstallFromAppEngine.stream()
                    .collect(Collectors.toMap(EntandoBundleComponentJobEntity::getComponentId, EntandoBundleComponentJobEntity::getStatus));

            componentsFeedback.put(uninstallationCMKey, componentsFeedbackCM);
            componentsFeedback.put(uninstallationAppEngineKey, componentsFeedbackAppEngine);
            return objectMapper.writeValueAsString(componentsFeedback);

        } catch (Exception uninstallException) {
            log.error("An error occurred during component uninstall", uninstallException);
            componentsFeedback.put(uninstallationCMKey, componentsFeedbackCM);
            componentsFeedback.put(uninstallationAppEngineKey, componentsFeedbackAppEngine);
            return objectMapper.writeValueAsString(componentsFeedback);
        }

    }

    private Map<String, ComponentInstallPlan> calculateDiffMap(Map<String, ComponentInstallPlan> currentMap, Map<String, ComponentInstallPlan> latestMap) {
        Map<String, ComponentInstallPlan> diffMap = new HashMap<>();
        latestMap.forEach((k, v) -> {
            if (!currentMap.containsKey(k)) {
                v.setAction(InstallAction.CREATE);
                diffMap.put(k, v);
            }
        });
        return diffMap;
    }

    private InstallPlan calculateDiffInstallPlan(InstallPlan currentInstallPlan, InstallPlan latestInstallPlan) {
        InstallPlan diff = new InstallPlan();
        diff.setWidgets(calculateDiffMap(currentInstallPlan.getWidgets(), latestInstallPlan.getWidgets()));

        return diff;
    }

    private JobStatus executeDeleteFromAppEngine(List<EntandoBundleComponentJobEntity> toDelete,
                                                 JobResult parentJobResult, Operation operation) {
        EntandoCoreComponentDeleteResponse response = entandoCoreClient.deleteComponents(toDelete.stream()
                .map(EntandoCoreComponentDeleteRequest::fromEntity)
                .flatMap(Optional::stream)
                .collect(Collectors.toList()));

        JobInfo jobInfo = new JobInfo(operation);

        switch (response.getStatus()) {
            case FAILURE:
                log.debug("In {} All deletes are in error with response:'{}'", operation, response);
                if (parentJobResult != null) {
                    bundleUninstallUtility.markGlobalError(parentJobResult, response);
                    bundleUninstallUtility.markSingleErrors(toDelete,response);
                }
                return jobInfo.error;
            case PARTIAL_SUCCESS:
                log.debug("In {} Partial deletes are in error with response:'{}'", operation, response);
                if (parentJobResult != null) {
                    bundleUninstallUtility.markGlobalError(parentJobResult, response);
                    bundleUninstallUtility.markSingleErrors(toDelete,response);
                }
                return jobInfo.partial;
            case SUCCESS:
            default:
                log.debug("In {} all deletes ok with response:'{}'", operation, response);
                return jobInfo.ok;
        }
    }

    private JobStatus executeDeleteFromAppEngine(List<EntandoBundleComponentJobEntity> toDelete, Operation operation) {
        return  executeDeleteFromAppEngine(toDelete, null, operation);
    }

    /**
     * download the bundle, create a BundleReader to read it and return it.
     *
     * @param bundleDownloader the BundleDownloader responsible to download the desired bundle
     * @param bundle           the object defining the bundle to download
     * @param tag              the object defining the version of the bundle to download
     * @return the created BundleReader ready to read the bundle
     */
    private BundleReader downloadBundleAndGetBundleReader(BundleDownloader bundleDownloader, EntandoDeBundle bundle,
            EntandoDeBundleTag tag) {

        final DownloadedBundle downloadedBundle = bundleDownloader.saveBundleLocally(bundle, tag);
        return new BundleReader(downloadedBundle, bundle);
    }

    private Queue<Installable> getBundleInstallableComponents(BundleReader bundleReader, InstallAction conflictStrategy,
            InstallPlan installPlan) {

        try {
            bundleReader.readBundleDescriptor(bundleDescriptorValidator);
        } catch (IOException e) {
            throw new EntandoComponentManagerException("An error occurred while reading the root bundle descriptor");
        }

        return getInstallableComponentsByPriority(bundleReader, conflictStrategy, installPlan);
    }

    private JobTracker<EntandoBundleComponentJobEntity> trackExecution(EntandoBundleComponentJobEntity job,
            Function<Installable<?>, JobResult> action, JobStatus jobStatus) {
        JobTracker<EntandoBundleComponentJobEntity> componentJobTracker = new JobTracker<>(job, compJobRepo);
        componentJobTracker.startTracking(jobStatus);
        JobResult result = action.apply(job.getInstallable());
        componentJobTracker.finishTracking(result);
        return componentJobTracker;
    }


    /**
     * execute every ReportableProcessor to extract the relative Reportable from the descriptor and return it.
     *
     * @param bundleReader the BUndleReader to use to read the bundle
     * @return a List of Reportable extracted from the bundle components descriptors
     */
    private Map<ReportableRemoteHandler, List<Reportable>> getReportableComponentsByRemoteHandler(
            BundleReader bundleReader) {

        return reportableComponentProcessorList.stream()
                .map(reportableProcessor ->
                        reportableProcessor.getReportable(bundleReader, (ComponentProcessor<?>) reportableProcessor))
                .collect(Collectors.groupingBy(Reportable::getReportableRemoteHandler));
    }

    private Queue<Installable> getInstallableComponentsByPriority(BundleReader bundleReader,
            InstallAction conflictStrategy, InstallPlan installPlan) {

        List<? extends Installable<?>> pluginInstallables = new ArrayList<>();

        // process plugins and collect endpoints
        if (processorMap.containsKey(ComponentType.PLUGIN)) {
            pluginInstallables = processorMap.get(ComponentType.PLUGIN)
                    .process(bundleReader, conflictStrategy, installPlan);

            final Map<String, String> pluginIngressMap = pluginInstallables.stream()
                    .filter(i -> i.getComponentType() == ComponentType.PLUGIN)
                    .map(Installable::getRepresentation)
                    .collect(Collectors.toMap(
                            d -> ((PluginDescriptor) d).getDescriptorMetadata().getPluginName(),
                            d -> BundleUtilities.composeIngressPathForV1((PluginDescriptor) d)));

            ((WidgetProcessor) processorMap.get(ComponentType.WIDGET)).setPluginIngressPathMap(pluginIngressMap);
        }

        collectWidgetConfigDescriptors(bundleReader, conflictStrategy, installPlan);

        // process other components
        final List<? extends Installable<?>> installables = processorMap.values()
                .stream()
                .filter(processor -> !(processor instanceof PluginProcessor))  // skip plugins that have been already processed at the beginning of the method
                .map(processor -> processor.process(bundleReader, conflictStrategy, installPlan))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        // concat results and return
        return Stream.concat(
                        installables.stream(),
                        pluginInstallables.stream())
                .sorted(Comparator.comparingInt(Installable::getPriority))
                .collect(Collectors.toCollection(ArrayDeque::new));
    }

    /**
     * Collects the descriptors of all the WIDGET_CONFIG components.
     */
    private void collectWidgetConfigDescriptors(
            BundleReader bundleReader,
            InstallAction conflictStrategy,
            InstallPlan installPlan) {
        //~
        WidgetProcessor widgetsProcessor = (WidgetProcessor) processorMap.get(ComponentType.WIDGET);
        if (widgetsProcessor == null) {
            log.error("No widget processor was found");
            return;
        }
        var installables = widgetsProcessor.collectConfigWidgets(bundleReader, conflictStrategy, installPlan);

        var widgetConfigDescriptors = installables.stream()
                .filter(i -> i.getRepresentation().getType().equals(WidgetDescriptor.TYPE_WIDGET_CONFIG))
                .map(Installable::getRepresentation)
                .collect(Collectors.toMap(WidgetDescriptor::getName, d -> d));

        widgetsProcessor.setWidgetConfigDescriptorsMap(widgetConfigDescriptors);
    }


    private void saveAsInstalledBundle(EntandoDeBundle bundle, EntandoBundleJobEntity job,
            BundleDescriptor bundleDescriptor, String bundleDigest) {

        EntandoBundleEntity installedComponent = bundleRepository
                .findByBundleCode(bundle.getMetadata().getName())
                .orElse(bundleService.convertToEntityFromEcr(bundle));

        installedComponent.setPbcList(extractPbcListFrom(bundle));
        installedComponent.setVersion(job.getComponentVersion());
        installedComponent.setJob(job);
        installedComponent.setBundleType(BundleUtilities.extractBundleTypeFromBundle(bundle).toString());
        installedComponent.setExt(bundleDescriptor.getExt());
        installedComponent.setInstalled(true);
        installedComponent.setImageDigest(bundleDigest);
        bundleRepository.save(installedComponent);
        log.info("Component " + job.getComponentId() + " registered as installed in the system");
    }


    private String extractPbcListFrom(EntandoDeBundle bundle) {

        return Optional.ofNullable(bundle.getMetadata().getAnnotations()).orElseGet(HashMap::new)
                .entrySet().stream()
                .filter(e -> e.getKey().equals(EntandoDeBundleComposer.PBC_ANNOTATIONS_KEY))
                .findFirst()
                .map(Entry::getValue)
                .map(this::pbcJsonArrayToString)
                .orElse(null);
    }

    /**
     * receives the json representation of the pbc names collected into a json array.
     * parses it and joins it as a comma separated string
     *
     * @param pbcJsonArray the json array containing the pbc names to parse
     * @return the same array parsed in a single comma separated string
     */
    private String pbcJsonArrayToString(String pbcJsonArray) {
        try {
            return String.join(",", objectMapper.readValue(pbcJsonArray, String[].class));
        } catch (JsonProcessingException e) {
            log.error("Error parsing PBC names from {} to string array", pbcJsonArray);
            return "";
        }
    }

    private boolean isUninstallable(EntandoBundleComponentJobEntity component) {
        /* TODO: related to ENG-415 (https://jira.entando.org/browse/ENG-415)
          Except for IN_PROGRESS, everything should be uninstallable
          Uninstall operations should be idempotent to be able to provide this
         */
        return component.getStatus().equals(JobStatus.INSTALL_COMPLETED)
                || (component.getStatus().equals(JobStatus.INSTALL_ERROR)
                && component.getComponentType() == ComponentType.PLUGIN);
    }

    // TODO: moves the inner class
    @Getter
    public class JobInfo {
        Operation operation;
        JobStatus ok;
        JobStatus error;
        JobStatus partial;
        String msg;

        public JobInfo(Operation operation) {
            switch (operation) {
                case ROLLBACK:
                    this.ok = JobStatus.INSTALL_ROLLBACK;
                    this.error = JobStatus.INSTALL_ROLLBACK_ERROR;
                    this.partial = JobStatus.INSTALL_ROLLBACK_PARTIAL;
                    this.msg = "rolling back";
                    break;
                case UNINSTALL:
                    this.ok = JobStatus.UNINSTALL_COMPLETED;
                    this.error = JobStatus.UNINSTALL_ERROR;
                    this.partial = JobStatus.UNINSTALL_PARTIAL_COMPLETED;
                    this.msg = "uninstall orphans components";
                    break;
                default:
                    throw new IllegalArgumentException("Invalid Operation");
            }

        }


    }

    private JobResult executeUninstallFromEcr(Installable<?> installable, Operation operation) {
        JobInfo jobInfo = new JobInfo(operation);
        return installable.uninstallFromEcr()
                .thenApply(vd -> JobResult.builder().status(jobInfo.ok).build())
                .exceptionally(th -> {
                    log.error(String.format("Error %s %s %s",
                            jobInfo.msg,
                            installable.getComponentType(),
                            installable.getName()), th);
                    String message = getMeaningfulErrorMessage(th, installable);
                    return JobResult.builder()
                            .status(jobInfo.error)
                            .rollbackException(new EntandoComponentManagerException(message))
                            .build();
                })
                .join();
    }

    private <T extends Descriptor> JobResult executeInstall(Installable<T> installable) {

        CompletableFuture<?> future = installable.install();
        CompletableFuture<JobResult> installResult = future
                .thenApply(vd -> {
                    log.debug("Installable '{}' finished successfully", installable.getName());
                    return JobResult.builder().status(JobStatus.INSTALL_COMPLETED).build();
                }).exceptionally(th -> {
                    String message = getMeaningfulErrorMessage(th, installable);
                    log.error("Installable '{}' has errors: {}", installable.getName(), message, th);
                    return JobResult.builder()
                            .status(JobStatus.INSTALL_ERROR)
                            .installException(new EntandoComponentManagerException(message))
                            .build();
                });

        return installResult.join();
    }
}

