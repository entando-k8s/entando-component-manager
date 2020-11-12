package org.entando.kubernetes.service.digitalexchange.job;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.controller.digitalexchange.job.model.AnalysisReport;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallActionsByComponentType;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallRequest.InstallAction;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.digitalexchange.ReportAnalysisException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.Descriptor;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloader;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderFactory;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
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
    private final @NonNull List<ReportableComponentProcessor> reportableComponentProcessorList;
    private final @NonNull Map<ReportableRemoteHandler, AnalysisReportFunction> analysisReportStrategies;

    public AnalysisReport performInstallAnalysis(EntandoDeBundle bundle, EntandoDeBundleTag tag) {

        AnalysisReport analysisReport = null;

        BundleDownloader bundleDownloader = downloaderFactory.newDownloader();

        try {
            BundleReader bundleReader = this.downloadBundleAndGetBundleReader(bundleDownloader, bundle, tag);
            Map<ReportableRemoteHandler, List<Reportable>> reportableByHandler =
                    this.getReportableComponentsByRemoteHandler(bundleReader);

            List<CompletableFuture<AnalysisReport>> futureList = reportableByHandler.keySet().stream()
                    // for each remote handler => get whole analysis report async
                    .map(key ->
                            CompletableFuture.supplyAsync(
                                    () -> analysisReportStrategies.get(key)
                                            .getAnalysisReport(reportableByHandler.get(key)))
                    )
                    .collect(Collectors.toList());

            // why using separate streams https://stackoverflow.com/questions/58700578/why-is-completablefuture-join-get-faster-in-separate-streams-than-using-one-stre

            try {
                analysisReport = futureList.stream().map(CompletableFuture::join)
                        .reduce(AnalysisReport::merge)
                        .orElseThrow(() -> {
                            throw new ReportAnalysisException(String.format(
                                    "An error occurred during the analysis report for the bundle %s with tag %s",
                                    bundle.getMetadata().getName(), tag.getVersion()));
                        });
            } catch (CompletionException e) {
                throw e.getCause() instanceof ReportAnalysisException
                        ? (ReportAnalysisException) e.getCause()
                        : e;
            }

        } finally {
            bundleDownloader.cleanTargetDirectory();
        }

        return analysisReport;
    }

    public EntandoBundleJobEntity install(EntandoDeBundle bundle, EntandoDeBundleTag tag,
            InstallAction conflictStrategy, InstallActionsByComponentType actions, AnalysisReport report) {
        EntandoBundleJobEntity job = createInstallJob(bundle, tag);
        submitInstallAsync(job, bundle, tag, conflictStrategy, actions, report);
        return job;
    }

    public EntandoBundleJobEntity install(EntandoDeBundle bundle, EntandoDeBundleTag tag) {
        return this.install(bundle, tag, InstallAction.CREATE, new InstallActionsByComponentType(),
                new AnalysisReport());
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

    private void submitInstallAsync(EntandoBundleJobEntity parentJob, EntandoDeBundle bundle, EntandoDeBundleTag tag,
            InstallAction conflictStrategy, InstallActionsByComponentType actions, AnalysisReport report) {
        CompletableFuture.runAsync(() -> {
            log.info("Started new install job for component " + parentJob.getComponentId() + "@" + tag.getVersion());

            JobTracker<EntandoBundleJobEntity> parentJobTracker = new JobTracker<>(parentJob, jobRepo);
            JobResult parentJobResult = JobResult.builder().build();
            JobScheduler scheduler = new JobScheduler();
            BundleDownloader bundleDownloader = downloaderFactory.newDownloader();

            parentJobTracker.startTracking(JobStatus.INSTALL_IN_PROGRESS);
            try {
                Queue<Installable> bundleInstallableComponents = getBundleInstallableComponents(bundle, tag,
                        bundleDownloader, conflictStrategy, actions, report);
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
                        })
                        .collect(Collectors.toCollection(ArrayDeque::new));
                scheduler.queueAll(componentJobQueue);

                JobProgress installProgress = new JobProgress(1.0 / componentJobQueue.size());

                Optional<EntandoBundleComponentJobEntity> optCompJob = scheduler.extractFromQueue();
                while (optCompJob.isPresent()) {
                    EntandoBundleComponentJobEntity installJob = optCompJob.get();
                    JobTracker<EntandoBundleComponentJobEntity> tracker = trackExecution(installJob,
                            this::executeInstall);
                    scheduler.recordProcessedComponentJob(tracker.getJob());
                    if (tracker.getJob().getStatus().equals(JobStatus.INSTALL_ERROR)) {
                        parentJobResult.setException(new EntandoComponentManagerException(parentJob.getComponentId()
                                + " install can't proceed due to an error with one of the components"));
                        break;
                    }
                    installProgress.increment();
                    parentJobTracker.setProgress(installProgress.getValue());
                    optCompJob = scheduler.extractFromQueue();
                }
                if (parentJobResult.hasException()) {
                    log.error("An error occurred during component installation", parentJobResult.getException());
                    log.warn("Rolling installation of bundle " + parentJob.getComponentId() + "@" + parentJob
                            .getComponentVersion());
                    parentJobResult = rollback(scheduler);
                } else {

                    saveAsInstalledBundle(bundle, parentJob);
                    parentJobResult.clearException();
                    parentJobResult.setStatus(JobStatus.INSTALL_COMPLETED);
                    parentJobResult.setProgress(1.0);
                    log.info("Bundle installed correctly");

                }

            } catch (Exception e) {
                log.error("An error occurred while reading components from the bundle", e);
                parentJobResult.setStatus(JobStatus.INSTALL_ERROR);
                parentJobResult.setException(e);
            }

            parentJobTracker.finishTracking(parentJobResult);
            bundleDownloader.cleanTargetDirectory();
        });
    }

    private JobResult rollback(JobScheduler scheduler) {
        JobResult result = JobResult.builder().build();
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
            result.clearException();
            result.setStatus(JobStatus.INSTALL_ROLLBACK);

        } catch (Exception rollbackException) {
            log.error("An error occurred during component rollback", rollbackException);
            result.setStatus(JobStatus.INSTALL_ERROR);
            result.setException(rollbackException);
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
        return new BundleReader(pathToDownloadedBundle);
    }

    private Queue<Installable> getBundleInstallableComponents(EntandoDeBundle bundle, EntandoDeBundleTag tag,
            BundleDownloader bundleDownloader, InstallAction conflictStrategy, InstallActionsByComponentType actions,
            AnalysisReport report) {
        BundleReader bundleReader = this.downloadBundleAndGetBundleReader(bundleDownloader, bundle, tag);
        return getInstallableComponentsByPriority(bundleReader, conflictStrategy, actions,
                report);
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
     * @param bundleReader the BundleReader to use to read the bundle
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
            InstallAction conflictStrategy, InstallActionsByComponentType actions, AnalysisReport report) {
        return processorMap.values().stream()
                .map(processor -> processor.process(bundleReader, conflictStrategy, actions, report))
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
                    String message = getMeaningfulErrorMessage(th);
                    return JobResult.builder()
                            .status(JobStatus.INSTALL_ROLLBACK_ERROR)
                            .exception(new Exception(message))
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

