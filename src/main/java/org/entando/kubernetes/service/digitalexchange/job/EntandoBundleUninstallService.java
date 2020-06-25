package org.entando.kubernetes.service.digitalexchange.job;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
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
import org.entando.kubernetes.model.digitalexchange.JobStatus;
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
public class EntandoBundleUninstallService implements ApplicationContextAware {

    private final @NonNull EntandoBundleJobRepository jobRepository;
    private final @NonNull EntandoBundleComponentJobRepository componentRepository;
    private final @NonNull InstalledEntandoBundleRepository installedComponentRepository;
    private final @NonNull EntandoCoreClient engineService;
    private final @NonNull EntandoBundleComponentUsageService usageService;
    private final @NonNull KubernetesService k8sService;
    private final @NonNull BundleDownloader bundleDownloader;

    private Collection<ComponentProcessor> componentProcessors = new ArrayList<>();

    public EntandoBundleJob uninstall(String componentId) {
        EntandoBundle installedBundle = installedComponentRepository.findById(componentId)
                .orElseThrow(() -> new BundleNotInstalledException("Bundle " + componentId + " is not installed"));

        verifyBundleUninstallIsPossibleOrThrow(installedBundle);

        return submitNewUninstallJob(installedBundle.getJob());
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

    private EntandoBundleJob submitNewUninstallJob(EntandoBundleJob lastAvailableJob) {
        List<EntandoBundleComponentJob> components = componentRepository.findAllByJob(lastAvailableJob);

        EntandoBundleJob uninstallJob = new EntandoBundleJob();
        uninstallJob.setComponentId(lastAvailableJob.getComponentId());
        uninstallJob.setComponentName(lastAvailableJob.getComponentName());
        uninstallJob.setComponentVersion(lastAvailableJob.getComponentVersion());
        uninstallJob.setStartedAt(LocalDateTime.now());
        uninstallJob.setStatus(JobStatus.UNINSTALL_CREATED);
        uninstallJob.setProgress(0.0);

        EntandoBundleJob savedJob = jobRepository.save(uninstallJob);
        submitUninstallAsync(uninstallJob, components);

        return savedJob;
    }


    private void submitUninstallAsync(EntandoBundleJob job, List<EntandoBundleComponentJob> components) {
        CompletableFuture.runAsync(() -> {
            JobStatus uninstallStatus = JobStatus.UNINSTALL_IN_PROGRESS;
            jobRepository.updateJobStatus(job.getId(), uninstallStatus);

            try {
                uninstallResources(job, components);
                uninstallStatus = uninstallInstallables(job, components);

                installedComponentRepository.deleteById(job.getComponentId());
                log.info("Component " + job.getComponentId() + " uninstalled successfully");
            } catch (Exception ex) {
                log.error("An error occurred while uninstalling component " + job.getComponentId(), ex);
                uninstallStatus = JobStatus.UNINSTALL_ERROR;
            }

            jobRepository.updateJobStatus(job.getId(), uninstallStatus);
        });
    }

    private JobStatus uninstallInstallables(EntandoBundleJob job, List<EntandoBundleComponentJob> components) {
        log.info("Processing uninstallation list for component " + job.getComponentId());

        componentProcessors.stream()
                .filter(processor -> processor.getComponentType() != ComponentType.ASSET
                        && processor.getComponentType() != ComponentType.DIRECTORY)
                .map(processor -> processor.process(components))
                .flatMap(List::stream)
                .sorted(Comparator.comparingInt((Installable i) -> i.getInstallPriority().getPriority()).reversed())
                .forEach(installable -> {
                    persistComponent(job, installable, JobStatus.UNINSTALL_CREATED);
                    JobStatus result = uninstallInstallable(installable);

                    if (result.equals(JobStatus.UNINSTALL_ERROR)) {
                        throw new EntandoComponentManagerException(job.getComponentId()
                                + " uninstallation can't proceed due to an error with one of the installed components");
                    }
                });

        return JobStatus.UNINSTALL_COMPLETED;
    }

    private JobStatus uninstallInstallable(Installable installable) {
        EntandoBundleComponentJob installableComponent = installable.getComponent();
        componentRepository.updateJobStatus(installableComponent.getId(), JobStatus.UNINSTALL_IN_PROGRESS);

        CompletableFuture<?> future = installable.uninstall();
        CompletableFuture<JobStatus> uninstallResult = future.thenApply(vd -> {
            log.debug("Uninstallation of installable '{}' finished successfully", installable.getName());
            componentRepository.updateJobStatus(installableComponent.getId(), JobStatus.UNINSTALL_COMPLETED);
            return JobStatus.UNINSTALL_COMPLETED;
        }).exceptionally(th -> {
            log.error("Uninstallation of installable '{}' has errors", installable.getName(), th.getCause());

            installableComponent.setStatus(JobStatus.UNINSTALL_ERROR);
            if (th.getCause() != null) {
                String message = th.getCause().getMessage();
                if (th.getCause() instanceof HttpClientErrorException) {
                    HttpClientErrorException httpException = (HttpClientErrorException) th.getCause();
                    message =
                            httpException.getMessage() + "\n" + httpException.getResponseBodyAsString();
                }
                componentRepository
                        .updateJobStatus(installableComponent.getId(), JobStatus.UNINSTALL_ERROR, message);
            } else {
                componentRepository
                        .updateJobStatus(installableComponent.getId(), JobStatus.UNINSTALL_ERROR, th.getMessage());
            }
            return JobStatus.UNINSTALL_ERROR;
        });
        return uninstallResult.join();
    }

    private EntandoBundleComponentJob persistComponent(EntandoBundleJob job, Installable installable, JobStatus status) {
        EntandoBundleComponentJob component = installable.getComponent();
        component.setJob(job);
        component.setStatus(status);

        component = componentRepository.save(component);

        log.debug("New component uninstall job created "
                + "for component of type " + installable.getComponentType() + " with name " + installable.getName());

        installable.setComponent(component);
        return component;
    }

    private void uninstallResources(EntandoBundleJob job, List<EntandoBundleComponentJob> components) {
        List<Installable> resources = componentProcessors.stream()
                .filter(processor -> processor.getComponentType() == ComponentType.ASSET
                        || processor.getComponentType() == ComponentType.DIRECTORY)
                .map(processor -> processor.process(components))
                .flatMap(List::stream)
                .sorted(Comparator.comparing(Installable::getName))
                .peek(installable -> persistComponent(job, installable, JobStatus.UNINSTALL_COMPLETED))
                .collect(Collectors.toList());

        resources.stream()
                .filter(installable -> installable.getComponentType() == ComponentType.DIRECTORY)
                .findFirst()
                .ifPresent(rootResourceInstallable -> {
                    //Remove root folder
                    log.info("Removing directory {}", rootResourceInstallable.getName());
                    engineService.deleteFolder(rootResourceInstallable.getName());
                });
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) {
        componentProcessors = applicationContext.getBeansOfType(ComponentProcessor.class).values();
    }

}
