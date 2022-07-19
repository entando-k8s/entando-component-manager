package org.entando.kubernetes.service.digitalexchange.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
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
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.model.AnalysisReport;
import org.entando.kubernetes.client.model.assembler.InstallPlanAssembler;
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
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.bundle.processor.PluginProcessor;
import org.entando.kubernetes.model.bundle.processor.WidgetProcessor;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.reportable.AnalysisReportFunction;
import org.entando.kubernetes.model.bundle.reportable.Reportable;
import org.entando.kubernetes.model.bundle.reportable.ReportableComponentProcessor;
import org.entando.kubernetes.model.bundle.reportable.ReportableRemoteHandler;
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
import org.entando.kubernetes.service.digitalexchange.JSONUtilities;
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

        return CompletableFuture.runAsync(() -> {
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
                            installJob, this::executeInstall
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

                    saveAsInstalledBundle(bundle, parentJob, bundleReader.readBundleDescriptor());
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

    private JobResult rollback(JobScheduler scheduler, JobResult result) {
        JobScheduler rollbackScheduler = scheduler.createRollbackScheduler();
        try {
            Optional<EntandoBundleComponentJobEntity> optCompJob = rollbackScheduler.extractFromQueue();
            while (optCompJob.isPresent()) {
                EntandoBundleComponentJobEntity rollbackJob = optCompJob.get();
                if (isUninstallable(rollbackJob)) {
                    JobTracker<EntandoBundleComponentJobEntity> tracker = trackExecution(rollbackJob,
                            this::executeRollback);
                    if (tracker.getJob().getStatus().equals(JobStatus.INSTALL_ROLLBACK_ERROR)) {
                        throw new EntandoComponentManagerException(
                                rollbackJob.getComponentType() + " " + rollbackJob.getComponentId()
                                        + " rollback can't proceed due to an error with one of the components");
                    }
                    rollbackScheduler.recordProcessedComponentJob(tracker.getJob());
                }
                optCompJob = rollbackScheduler.extractFromQueue();
            }

            log.info("Rollback operation completed successfully");
            result.setStatus(JobStatus.INSTALL_ROLLBACK);

        } catch (Exception rollbackException) {
            log.error("An error occurred during component rollback", rollbackException);
            result.setStatus(JobStatus.INSTALL_ERROR);
            result.setRollbackException(rollbackException);
        }
        return result;
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

        Path pathToDownloadedBundle = bundleDownloader.saveBundleLocally(bundle, tag);
        return new BundleReader(pathToDownloadedBundle, bundle);
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
            Function<Installable, JobResult> action) {
        JobTracker<EntandoBundleComponentJobEntity> componentJobTracker = new JobTracker<>(job, compJobRepo);
        componentJobTracker.startTracking(JobStatus.INSTALL_IN_PROGRESS);
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
            BundleDescriptor bundleDescriptor) {

        EntandoBundleEntity installedComponent = bundleRepository
                .findByBundleCode(bundle.getMetadata().getName())
                .orElse(bundleService.convertToEntityFromEcr(bundle));

        installedComponent.setPbcList(extractPbcListFrom(bundle));
        installedComponent.setVersion(job.getComponentVersion());
        installedComponent.setJob(job);
        installedComponent.setBundleType(BundleUtilities.extractBundleTypeFromBundle(bundle).toString());
        installedComponent.setExt(bundleDescriptor.getExt());
        installedComponent.setInstalled(true);
        bundleRepository.save(installedComponent);
        log.info("Component " + job.getComponentId() + " registered as installed in the system");
    }


    private String extractPbcListFrom(EntandoDeBundle bundle) {

        return Optional.ofNullable(bundle.getMetadata().getAnnotations()).orElseGet(HashMap::new)
                .entrySet().stream()
                .filter(e -> e.getKey().equals(EntandoDeBundleComposer.PBC_ANNOTATIONS_KEY))
                .findFirst()
                .map(Entry::getValue)
                // replace json array useless chars
                .map(v -> v.replaceAll("[\\[\\]\"]", ""))
                .orElse(null);
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

    private JobResult executeRollback(Installable<?> installable) {
        return installable.uninstall()
                .thenApply(vd -> JobResult.builder().status(JobStatus.INSTALL_ROLLBACK).build())
                .exceptionally(th -> {
                    log.error(String.format("Error rolling back %s %s",
                            installable.getComponentType(),
                            installable.getName()), th);
                    String message = getMeaningfulErrorMessage(th, installable);
                    return JobResult.builder()
                            .status(JobStatus.INSTALL_ROLLBACK_ERROR)
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

