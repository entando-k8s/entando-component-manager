package org.entando.kubernetes.service.digitalexchange.job;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.exception.job.JobConflictException;
import org.entando.kubernetes.exception.k8ssvc.K8SServiceClientException;
import org.entando.kubernetes.model.bundle.BundleReader;
import org.entando.kubernetes.model.bundle.descriptor.ComponentDescriptor;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloader;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.EntandoBundle;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleJob;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleComponentJob;
import org.entando.kubernetes.model.digitalexchange.JobStatus;
import org.entando.kubernetes.model.digitalexchange.JobType;
import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

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

    private Optional<EntandoBundleJob> searchForCompletedOrConflictingJob(EntandoDeBundle bundle ) {

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
        String digitalExchangeId = bundle.getMetadata().getNamespace();
        String componentId = bundle.getSpec().getDetails().getName();
        Optional<EntandoBundleJob> lastJobStarted = jobRepo
                .findFirstByDigitalExchangeAndComponentIdOrderByStartedAtDesc(digitalExchangeId, componentId);
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
                .orElseThrow(() -> new InvalidBundleException("Version " + version + " not defined in bundle versions"));
    }

    private EntandoBundleJob createInstallJob(EntandoDeBundle bundle, EntandoDeBundleTag tag) {
        final EntandoBundleJob job = new EntandoBundleJob();

        job.setComponentId(bundle.getMetadata().getName());
        job.setComponentName(bundle.getSpec().getDetails().getName());
        job.setComponentVersion(tag.getVersion());
        job.setDigitalExchange(bundle.getMetadata().getNamespace());
        job.setProgress(0);
        job.setStartedAt(LocalDateTime.now());
        job.setStatus(JobStatus.INSTALL_CREATED);

        EntandoBundleJob createdJob = jobRepo.save(job);
        log.debug("New installation job created " + job.toString());
        return createdJob;
    }

    public List<EntandoBundleJob> getAllJobs(String componentId) {
        return jobService.getAllJobs(componentId);
    }

    private void submitInstallAsync(EntandoBundleJob job, EntandoDeBundle bundle, EntandoDeBundleTag tag) {
        CompletableFuture.runAsync(() -> {
            log.info("Started new install job for component " + job.getComponentId() + "@" + tag.getVersion());

            JobStatus pipelineStatus = JobStatus.INSTALL_IN_PROGRESS;
            jobRepo.updateJobStatus(job.getId(), pipelineStatus);

            try {
                bundleDownloader.createTargetDirectory();
                Path pathToDownloadedBundle = bundleDownloader.saveBundleLocally(bundle, tag);
                List<Installable> installables = getInstallables(job, pathToDownloadedBundle);
                pipelineStatus = processInstallableList(job, installables);
                if (pipelineStatus.equals(JobStatus.INSTALL_COMPLETED)) {
                    log.info("All installables have been processed correctly");
                    EntandoBundle installedComponent = EntandoBundle.newFrom(bundle);
                    installedComponent.setInstalled(true);
                    installedComponentRepo.save(installedComponent);
                    log.info("Component " + job.getComponentId() + " registered as installed in the system");
                }

            } catch (Exception e) {
                log.error("An error occurred during digital-exchange component installation", e.getCause());
                pipelineStatus = rollback(job);
            }

            jobRepo.updateJobStatus(job.getId(), pipelineStatus);
            bundleDownloader.cleanTargetDirectory();
        });
    }

    private JobStatus rollback(EntandoBundleJob job) {
        JobStatus rollbackResult;
        // Get all the installed components for the job
        List<EntandoBundleComponentJob> jobRelatedComponents = jobComponentRepo.findAllByJob(job);

        // Filter jobs that are "uninstallable"
        List<EntandoBundleComponentJob> installedOrInProgress = jobRelatedComponents.stream()
                .filter(j -> j.getStatus().equals(JobStatus.INSTALL_COMPLETED))
                .collect(Collectors.toList());

        try {
            // Cleanup resource folder
            cleanupResourceFolder(job, installedOrInProgress);
            // For each installed component
            List<EntandoBundleComponentJob> nonResourceComponents =
                    installedOrInProgress.stream().filter(c -> c.getComponentType() != ComponentType.RESOURCE)
                    .collect(Collectors.toList());
            for(EntandoBundleComponentJob jc: nonResourceComponents) {
                // Revert the operation
                EntandoBundleComponentJob revertJob = jc.duplicate();
                componentProcessors.stream()
                        .filter(processor -> processor.shouldProcess(revertJob.getComponentType()))
                        .forEach(processor -> processor.uninstall(revertJob));
                revertJob.setStatus(JobStatus.INSTALL_ROLLBACK);
                jobComponentRepo.save(revertJob);
            }
            rollbackResult = JobStatus.INSTALL_ROLLBACK;
        } catch (Exception e) {
           rollbackResult = JobStatus.INSTALL_ERROR;
        }
        // In case of the plugin, that would mean delete the link
        return rollbackResult;
    }

    private void cleanupResourceFolder(EntandoBundleJob job, List<EntandoBundleComponentJob> components) {
        String componentRootFolder = "/" + job.getComponentId();
        Optional<EntandoBundleComponentJob> rootResourceFolder = components.stream().filter(component ->
                component.getComponentType() == ComponentType.RESOURCE
                        && component.getName().equals(componentRootFolder)
        ).findFirst();

        if (rootResourceFolder.isPresent()) {
            engineService.deleteFolder(componentRootFolder);
            components.stream().filter(component -> component.getComponentType() == ComponentType.RESOURCE)
                    .forEach(component -> {
                        EntandoBundleComponentJob uninstalledJobComponent = component.duplicate();
                        uninstalledJobComponent.setJob(job);
                        uninstalledJobComponent.setStatus(JobStatus.INSTALL_ROLLBACK);
                        jobComponentRepo.save(uninstalledJobComponent);
                    });
        }
    }

    private JobStatus processInstallableList(EntandoBundleJob job, List<Installable> installableList) {
        log.info("Processing installable list for component " + job.getComponentId());

        JobStatus installSucceded = JobStatus.INSTALL_COMPLETED;
        for (Installable installable: installableList) {
            installable.setComponent(persistComponent(job, installable));
            installSucceded = processInstallable(installable);
            if (installSucceded.equals(JobStatus.INSTALL_ERROR)) {
                throw new RuntimeException(job.getComponentId() + " installation can't proceed due to an error with one of the installed components");
            }
        }
        return installSucceded;
    }

    private JobStatus processInstallable(Installable installable) {
        EntandoBundleComponentJob installableComponent = installable.getComponent();
        jobComponentRepo.updateJobStatus(installableComponent.getId(), JobStatus.INSTALL_IN_PROGRESS);

        CompletableFuture<?> future = installable.install();
        CompletableFuture<JobStatus> installResult = future.thenApply(vd -> {
            log.debug("Installable '{}' finished successfully", installable.getName());
            jobComponentRepo.updateJobStatus(installableComponent.getId(), JobStatus.INSTALL_COMPLETED);
            return JobStatus.INSTALL_COMPLETED;
        }).exceptionally(th -> {
            log.error("Installable '{}' has errors", installable.getName(), th.getCause());

            installableComponent.setStatus(JobStatus.INSTALL_ERROR);
            if (th.getCause() != null) {
                String message = th.getCause().getMessage();
                if (th.getCause() instanceof HttpClientErrorException) {
                    HttpClientErrorException httpException = (HttpClientErrorException) th.getCause();
                    message =
                            httpException.getMessage() + "\n" + httpException.getResponseBodyAsString();
                }
                jobComponentRepo
                        .updateJobStatus(installableComponent.getId(), JobStatus.INSTALL_ERROR, message);
            } else {
                jobComponentRepo
                        .updateJobStatus(installableComponent.getId(), JobStatus.INSTALL_ERROR, th.getMessage());
            }
            return JobStatus.INSTALL_ERROR;
        });
        return installResult.join();
    }

    private List<Installable> getInstallables(EntandoBundleJob job, Path p) {
        log.info("Extracting installable components from downloaded bundle");
        try {
            BundleReader r = new BundleReader(p);
            ComponentDescriptor descriptor = r.readBundleDescriptor();
            return getInstallables(job, r, descriptor);
        } catch (IOException e) {
            log.error("An error occurred while getting installables components", e);
            throw new UncheckedIOException(e);
        }
    }

    private List<Installable> getInstallables(EntandoBundleJob job, BundleReader r,
            ComponentDescriptor descriptor) throws IOException {
        List<Installable> installables = new LinkedList<>();
        for (ComponentProcessor processor : componentProcessors) {
            ofNullable(processor.process(job, r, descriptor))
                    .ifPresent(installables::addAll);
        }
        return installables;
    }

    private EntandoBundleComponentJob persistComponent(EntandoBundleJob job, Installable installable) {
        EntandoBundleComponentJob component = new EntandoBundleComponentJob();
        component.setJob(job);
        component.setComponentType(installable.getComponentType());
        component.setName(installable.getName());
        component.setChecksum(installable.getChecksum());
        component.setStatus(JobStatus.INSTALL_CREATED);

        component = jobComponentRepo.save(component);

        log.debug("New component job created "
                + "for component of type " + installable.getComponentType() + " with name " + installable.getName());
        return component;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        componentProcessors = applicationContext.getBeansOfType(ComponentProcessor.class).values();
    }

}

