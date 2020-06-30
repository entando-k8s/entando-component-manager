package org.entando.kubernetes.service.digitalexchange.job;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.exception.job.JobConflictException;
import org.entando.kubernetes.exception.k8ssvc.K8SServiceClientException;
import org.entando.kubernetes.model.bundle.BundleReader;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloader;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.EntandoBundle;
import org.entando.kubernetes.model.job.*;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntandoBundleInstallService implements EntandoBundleJobExecutor {

    private final @NonNull KubernetesService k8sService;
    private final @NonNull EntandoBundleJobService jobService;
    private final @NonNull BundleDownloader bundleDownloader;
    private final @NonNull EntandoBundleJobRepository jobRepo;
    private final @NonNull EntandoBundleComponentJobRepository compJobRepo;
    private final @NonNull InstalledEntandoBundleRepository installedComponentRepo;
    private final @NonNull Map<ComponentType, ComponentProcessor<?>> processorMap;

    public EntandoBundleJob install(String componentId, String version) {
        EntandoDeBundle bundle = k8sService.getBundleByName(componentId)
                .orElseThrow(() -> new K8SServiceClientException("Bundle with name " + componentId + " not found"));

        Optional<EntandoBundleJob> j = searchForCompletedOrConflictingJob(bundle);

        return j.orElseGet(() -> createAndSubmitNewInstallJob(bundle, version));
    }

    private EntandoBundleJob createAndSubmitNewInstallJob(EntandoDeBundle bundle, String version) {
        EntandoDeBundleTag versionToInstall = getBundleTag(bundle, version);
        EntandoBundleJob job = createInstallJob(bundle, versionToInstall);

        submitInstallAsync(job, bundle, versionToInstall);
        return job;
    }

    private Optional<EntandoBundleJob> searchForCompletedOrConflictingJob(EntandoDeBundle bundle) {

        log.info("Verify validity of a new install job for component " + bundle.getMetadata().getName());

        EntandoBundleJob installCompletedJob = null;

        Optional<EntandoBundleJob> optionalExistingJob = getExistingJob(bundle);
        if (optionalExistingJob.isPresent()) {
            EntandoBundleJob j = optionalExistingJob.get();
            JobStatus js = j.getStatus();
            if (js.equals(JobStatus.INSTALL_COMPLETED)) {
                installCompletedJob = j;
            }
            if (js.isOfType(JobType.UNFINISHED)) {
                throw new JobConflictException("Conflict with another job for the component " + j.getComponentId()
                        + " - JOB ID: " + j.getId());
            }
        }
        return Optional.ofNullable(installCompletedJob);
    }

    private Optional<EntandoBundleJob> getExistingJob(EntandoDeBundle bundle) {
        String componentId = bundle.getSpec().getDetails().getName();
        Optional<EntandoBundleJob> lastJobStarted = jobRepo.findFirstByComponentIdOrderByStartedAtDesc(componentId);
        if (lastJobStarted.isPresent()) {
            // To be an existing job it should be Running or completed
            if (lastJobStarted.get().getStatus() == JobStatus.UNINSTALL_COMPLETED) {
                return Optional.empty();
            }
            return lastJobStarted;
        }
        return Optional.empty();
    }

    private EntandoDeBundleTag getBundleTag(EntandoDeBundle bundle, String version) {
        log.info("Extracting version " + version + " from bundle");
        String versionToFind = BundleUtilities.getBundleVersionOrFail(bundle, version);
        return bundle.getSpec().getTags().stream().filter(t -> t.getVersion().equals(versionToFind)).findAny()
                .orElseThrow(
                        () -> new InvalidBundleException("Version " + version + " not defined in bundle versions"));
    }

    private EntandoBundleJob createInstallJob(EntandoDeBundle bundle, EntandoDeBundleTag tag) {
        final EntandoBundleJob job = new EntandoBundleJob();

        job.setComponentId(bundle.getMetadata().getName());
        job.setComponentName(bundle.getSpec().getDetails().getName());
        job.setComponentVersion(tag.getVersion());
        job.setProgress(0);
        job.setStatus(JobStatus.INSTALL_CREATED);

        EntandoBundleJob createdJob = jobRepo.save(job);
        log.debug("New installation job created " + job.toString());
        return createdJob;
    }

    public List<EntandoBundleJob> getAllJobs(String componentId) {
        return jobService.getJobs(componentId);
    }

    private void submitInstallAsync(EntandoBundleJob parentJob, EntandoDeBundle bundle, EntandoDeBundleTag tag) {
        CompletableFuture.runAsync(() -> {
            log.info("Started new install job for component " + parentJob.getComponentId() + "@" + tag.getVersion());

            JobTracker<EntandoBundleJob> parentJobTracker = new JobTracker<>(parentJob, jobRepo);
            JobScheduler scheduler = new JobScheduler();

            JobResult parentJobResult = JobResult.builder().status(JobStatus.INSTALL_IN_PROGRESS).build();
            parentJobTracker.startTracking(parentJobResult.getStatus());

            Queue<Installable> bundleInstallableComponents = getBundleInstallableComponents(bundle, tag);
            Queue<EntandoBundleComponentJob> componentJobQueue = bundleInstallableComponents.stream()
                    .map(i -> {
                        EntandoBundleComponentJob cj = new EntandoBundleComponentJob();
                        cj.setParentJob(parentJob);
                        cj.setComponentType(i.getComponentType());
                        cj.setComponentId(i.getName());
                        cj.setChecksum(i.getChecksum());
                        cj.setInstallable(i);
                        return cj;
                    })
                    .collect(Collectors.toCollection(ArrayDeque::new));

            scheduler.queueAll(componentJobQueue);

            try {
                Optional<EntandoBundleComponentJob> optCompJob = scheduler.extractFromQueue();
                while(optCompJob.isPresent()) {
                    EntandoBundleComponentJob installJob = optCompJob.get();
                    JobTracker<EntandoBundleComponentJob> tracker = trackExecution(installJob, this::executeInstall);
                    scheduler.recordProcessedComponentJob(tracker.getJob());
                    if (tracker.getJob().getStatus().equals(JobStatus.INSTALL_ERROR)) {
                        throw new EntandoComponentManagerException(parentJob.getComponentId()
                                + " install can't proceed due to an error with one of the components");
                    }
                    optCompJob = scheduler.extractFromQueue();
                }

                saveAsInstalledBundle(bundle, parentJob);
                parentJobResult.clearException();
                parentJobResult.setStatus(JobStatus.INSTALL_COMPLETED);
                log.info("Bundle installed correctly");

            } catch (Exception installException) {
                log.error("An error occurred during component installation", installException);
                parentJobResult = rollback(scheduler);
            }

            parentJobTracker.stopTrackingTime(parentJobResult);
            bundleDownloader.cleanTargetDirectory();
        });
    }


    private Queue<Installable> getBundleInstallableComponents(EntandoDeBundle bundle, EntandoDeBundleTag tag) {
        Path pathToDownloadedBundle = bundleDownloader.saveBundleLocally(bundle, tag);
        return getInstallableComponentsByPriority(new BundleReader(pathToDownloadedBundle));
    }

    private JobTracker<EntandoBundleComponentJob> trackExecution(EntandoBundleComponentJob job, Function<Installable, JobResult> action) {
        JobTracker<EntandoBundleComponentJob> componentJobTracker = new JobTracker<>(job, compJobRepo);
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

    private void saveAsInstalledBundle(EntandoDeBundle bundle, EntandoBundleJob job) {
        EntandoBundle installedComponent = EntandoBundle.newFrom(bundle);
        installedComponent.setInstalled(true);
        installedComponent.setJob(job);
        installedComponentRepo.save(installedComponent);
        log.info("Component " + job.getComponentId() + " registered as installed in the system");
    }

    private JobResult rollback(JobScheduler scheduler) {
        JobResult rollbackResult = JobResult.builder().build();
        scheduler.activateRollbackMode();
        try {
            Optional<EntandoBundleComponentJob> optCompJob = scheduler.extractFromQueue();
            while(optCompJob.isPresent()) {
                EntandoBundleComponentJob rollbackJob = optCompJob.get();
                if (isUninstallable(rollbackJob)) {
                    JobTracker<EntandoBundleComponentJob> tracker = trackExecution(rollbackJob, this::executeRollback);
                    if (tracker.getJob().getStatus().equals(JobStatus.INSTALL_ROLLBACK_ERROR)) {
                        throw new EntandoComponentManagerException(rollbackJob.getComponentType() + " " + rollbackJob.getComponentId()
                                + " rollback can't proceed due to an error with one of the components");
                    }
                    scheduler.recordProcessedComponentJob(tracker.getJob());
                }
                optCompJob = scheduler.extractFromQueue();
            }

            rollbackResult.setException(null);
            rollbackResult.setStatus(JobStatus.INSTALL_ROLLBACK_COMPLETED);

        } catch (Exception rollbackException) {

            log.error("An error occurred during component rollback", rollbackException);
            rollbackResult.setStatus(JobStatus.INSTALL_ERROR);
            rollbackResult.setException(rollbackException);
        }
        return rollbackResult;
    }

    private boolean isUninstallable(EntandoBundleComponentJob component) {
        /* TODO: related to ENG-415 (https://jira.entando.org/browse/ENG-415)
          Except for IN_PROGRESS, everything should be uninstallable
          Uninstall operations should be idempotent to be able to provide this
         */
        return component.getStatus().equals(JobStatus.INSTALL_COMPLETED) ||
                (component.getStatus().equals(JobStatus.INSTALL_ERROR) && component.getComponentType() == ComponentType.PLUGIN);
    }


    private JobResult executeRollback(Installable<?> installable) {
        return installable.uninstall()
                .thenApply(vd -> JobResult.builder().status(JobStatus.INSTALL_ROLLBACK_COMPLETED).build())
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
                    log.error("Installable '{}' has errors", installable.getName(), th.getCause());
                    String message = getMeaningfulErrorMessage(th);
                    return JobResult.builder()
                            .status(JobStatus.INSTALL_ERROR)
                            .exception(new Exception(message))
                            .build();
                });

        return installResult.join();
    }

}

