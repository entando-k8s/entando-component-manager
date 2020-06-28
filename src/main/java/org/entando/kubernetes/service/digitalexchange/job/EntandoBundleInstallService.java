package org.entando.kubernetes.service.digitalexchange.job;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
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
import org.entando.kubernetes.model.digitalexchange.*;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntandoBundleInstallService implements ApplicationContextAware {

    private final @NonNull KubernetesService k8sService;
    private final @NonNull EntandoBundleJobService jobService;
    private final @NonNull EntandoCoreClient engineService;
    private final @NonNull BundleDownloader bundleDownloader;
    private final @NonNull EntandoBundleJobRepository jobRepo;
    private final @NonNull EntandoBundleComponentJobRepository jobComponentRepo;
    private final @NonNull InstalledEntandoBundleRepository installedComponentRepo;
    private Collection<ComponentProcessor> componentProcessors = new ArrayList<>();

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
        job.setStartedAt(LocalDateTime.now());
        job.setStatus(JobStatus.INSTALL_CREATED);

        EntandoBundleJob createdJob = jobRepo.save(job);
        log.debug("New installation job created " + job.toString());
        return createdJob;
    }

    public List<EntandoBundleJob> getAllJobs(String componentId) {
        return jobService.getJobs(componentId);
    }

    private void submitInstallAsync(EntandoBundleJob job, EntandoDeBundle bundle, EntandoDeBundleTag tag) {
        CompletableFuture.runAsync(() -> {
            log.info("Started new install job for component " + job.getComponentId() + "@" + tag.getVersion());

            JobTracker tracker = new JobTracker(job, jobRepo);
            tracker.setJob(job);
            tracker.getJob().setStatus(JobStatus.INSTALL_IN_PROGRESS);
            jobRepo.save(tracker.getJob());

            Path pathToDownloadedBundle = bundleDownloader.saveBundleLocally(bundle, tag);
            BundleReader bundleReader = new BundleReader(pathToDownloadedBundle);
            List<Installable> installablesByPriority = componentProcessors.stream()
                    .map(processor -> processor.process(job, bundleReader))
                    .flatMap(List::stream)
                    .sorted(Comparator.comparingInt(i -> i.getInstallPriority().getPriority()))
                    .collect(Collectors.toList());

            for(Installable i: installablesByPriority) {
                EntandoBundleComponentJob componentJob = buildComponentJob(tracker.getJob(), i);
                tracker.queueComponentJob(componentJob);
            }

            try {

                Optional<EntandoBundleComponentJob> ocj = tracker.extractNextComponentJobToProcess();
                while(ocj.isPresent()) {
                    EntandoBundleComponentJob componentJob = ocj.get();
                    componentJob.setStatus(JobStatus.INSTALL_IN_PROGRESS);
                    jobComponentRepo.save(componentJob);
                    Installable installable = componentJob.getInstallable();
                    JobResult installResult = executeInstall(installable);
                    componentJob.setStatus(installResult.getStatus());
                    installResult.getException().ifPresent(ex -> componentJob.setErrorMessage(ex.getMessage()));
                    jobComponentRepo.save(componentJob);
                    tracker.recordProcessedComponentJob(componentJob);

                    if (installResult.getStatus().equals(JobStatus.INSTALL_ERROR)) {
                        throw new EntandoComponentManagerException(tracker.getJob().getComponentId()
                                + " install can't proceed due to an error with one of the components");
                    }

                    ocj = tracker.extractNextComponentJobToProcess();
                }

                tracker.getJob().setStatus(JobStatus.INSTALL_COMPLETED);
                log.info("Bundle installed correctly");

            } catch (Exception e) {
                log.error("An error occurred during component installation", e);

                tracker.activateRollbackMode();

                Optional<EntandoBundleComponentJob> ocj = tracker.extractNextComponentJobToProcess();
                while(ocj.isPresent()) {
                    EntandoBundleComponentJob componentRollbackJob = ocj.get();
                    if (isUninstallable(componentRollbackJob)) {
                        JobResult rollbackJR = rollback(componentRollbackJob.getInstallable());
                        componentRollbackJob.setStatus(rollbackJR.getStatus());
                        rollbackJR.getException().ifPresent(ex -> {
                            componentRollbackJob.setErrorMessage(ex.getMessage());
                        });
                        jobComponentRepo.save(componentRollbackJob);
                        tracker.recordProcessedComponentJob(componentRollbackJob);
                    }
                    ocj = tracker.extractNextComponentJobToProcess();
                }

                if (tracker.hasAnyComponentError()) {
                    tracker.getJob().setStatus(JobStatus.INSTALL_ERROR);
                } else {
                    tracker.getJob().setStatus(JobStatus.INSTALL_ROLLBACK);
                }

            }

            if (tracker.getJob().getStatus().equals(JobStatus.INSTALL_COMPLETED)) {
                EntandoBundle installedComponent = EntandoBundle.newFrom(bundle);
                installedComponent.setInstalled(true);
                installedComponent.setJob(tracker.getJob());
                installedComponentRepo.save(installedComponent);
                log.info("Component " + tracker.getJob().getComponentId() + " registered as installed in the system");
            }
            jobRepo.updateJobStatus(tracker.getJob().getId(), tracker.getJob().getStatus());
            bundleDownloader.cleanTargetDirectory();
        });
    }

    private JobResult rollback(Installable<?> installable) {
        return installable.uninstall()
                .thenApply(vd -> JobResult.builder().status(JobStatus.INSTALL_ROLLBACK).build())
                .exceptionally(th -> {
                    log.error(String.format("Error rolling back %s %s",
                            installable.getComponentType(),
                            installable.getName()), th);
                    return JobResult.builder()
                            .status(JobStatus.INSTALL_ROLLBACK_ERROR)
                            .exception(new Exception(th))
                            .build();
                })
                .join();
    }

    private boolean isUninstallable(EntandoBundleComponentJob component) {
        /* TODO: related to ENG-415 (https://jira.entando.org/browse/ENG-415)
          Except for IN_PROGRESS, everything should be uninstallable
          Uninstall operations should be idempotent to be able to provide this
         */
        return component.getStatus().equals(JobStatus.INSTALL_COMPLETED) ||
                (component.getStatus().equals(JobStatus.INSTALL_ERROR) && component.getComponentType() == ComponentType.PLUGIN);
    }



    private JobResult executeInstall(Installable installable) {

        CompletableFuture<?> future = installable.install();
        CompletableFuture<JobResult> installResult = future
                .thenApply(vd -> {
                    log.debug("Installable '{}' finished successfully", installable.getName());
                    return JobResult.builder().status(JobStatus.INSTALL_COMPLETED).build();
                }).exceptionally(th -> {
                    log.error("Installable '{}' has errors", installable.getName(), th.getCause());
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
                            .status(JobStatus.INSTALL_ERROR)
                            .exception(new Exception(message))
                            .build();
                });

        // FIXME I get here after having activated the rollback!
        return installResult.join();
    }

    private EntandoBundleComponentJob buildComponentJob(EntandoBundleJob job, Installable<?> installable) {
        EntandoBundleComponentJob component = new EntandoBundleComponentJob();
        component.setJob(job);
        component.setComponentType(installable.getComponentType());
        component.setName(installable.getName());
        component.setChecksum(installable.getChecksum());
        component.setStatus(JobStatus.INSTALL_CREATED);
        component.setInstallable(installable);

        log.debug("New component job created "
                + "for component of type " + installable.getComponentType() + " with name " + installable.getName());
        return component;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        componentProcessors = applicationContext.getBeansOfType(ComponentProcessor.class).values();
    }

}

