package org.entando.kubernetes.service.digitalexchange.job;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.exception.job.JobConflictException;
import org.entando.kubernetes.exception.job.JobExecutionException;
import org.entando.kubernetes.exception.k8ssvc.K8SServiceClientException;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleComponentJob;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleJob;
import org.entando.kubernetes.model.digitalexchange.JobStatus;
import org.entando.kubernetes.model.digitalexchange.JobType;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
import org.entando.kubernetes.service.KubernetesService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntandoBundleUninstallService implements ApplicationContextAware {

    private final @NonNull EntandoBundleJobRepository jobRepository;
    private final @NonNull EntandoBundleComponentJobRepository componentRepository;
    private final @NonNull InstalledEntandoBundleRepository installedComponentRepository;
    private final @NonNull EntandoCoreClient engineService;
    private final @NonNull KubernetesService k8sService;

    private Collection<ComponentProcessor> componentProcessors = new ArrayList<>();

    public EntandoBundleJob uninstall(String componentId) {
        EntandoDeBundle bundle = k8sService.getBundleByName(componentId)
                .orElseThrow(() -> new K8SServiceClientException("Bundle with name " + componentId + " not found"));
        EntandoBundleJob lastAvailableJob = getLastAvailableJob(bundle)
                .orElseThrow(() -> new RuntimeException("No job found for " + componentId));

        verifyJobStatusCompatibleWithUninstall(lastAvailableJob);

        EntandoBundleJob uninstallJob;
        if (lastAvailableJob.getStatus().isOfType(JobType.INSTALL)) {
            uninstallJob = submitNewUninstallJob(lastAvailableJob);
        } else {
            EntandoBundleJob lastInstallAttemptJob = findLastInstallJob(bundle)
                    .orElseThrow(() -> new RuntimeException(
                            "No install job associated with " + componentId + " has been found"));
            uninstallJob = submitNewUninstallJob(lastInstallAttemptJob);
        }

        return uninstallJob;

    }

    private void verifyJobStatusCompatibleWithUninstall(EntandoBundleJob job) {
        if (job.getStatus().isOfType(JobType.UNFINISHED)) {
            throw new JobConflictException(
                    "Install job for the component " + job.getComponentId() + " is in progress - JOB ID: " + job
                            .getId());
        }
    }


    private Optional<EntandoBundleJob> getLastAvailableJob(EntandoDeBundle bundle) {
        String digitalExchange = bundle.getMetadata().getNamespace();
        String componentId = bundle.getMetadata().getName();

        return jobRepository.findFirstByDigitalExchangeAndComponentIdOrderByStartedAtDesc(digitalExchange, componentId);
    }

    private Optional<EntandoBundleJob> findLastInstallJob(EntandoDeBundle bundle) {
        String digitalExchange = bundle.getMetadata().getNamespace();
        String componentId = bundle.getMetadata().getName();
        return jobRepository.findAllByDigitalExchangeAndComponentIdOrderByStartedAtDesc(digitalExchange, componentId)
                .stream()
                .filter(j -> j.getStatus().isOfType(JobType.INSTALL))
                .findFirst();
    }

    private EntandoBundleJob submitNewUninstallJob(EntandoBundleJob lastAvailableJob) {
        List<EntandoBundleComponentJob> components = componentRepository.findAllByJob(lastAvailableJob);

        EntandoBundleJob uninstallJob = new EntandoBundleJob();
        uninstallJob.setComponentId(lastAvailableJob.getComponentId());
        uninstallJob.setComponentName(lastAvailableJob.getComponentName());
        uninstallJob.setDigitalExchange(lastAvailableJob.getDigitalExchange());
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
                uninstallStatus = uninstallComponent(job, components);
                installedComponentRepository.deleteById(job.getComponentId());
            } catch (Exception ex) {
                log.error("An error occurred while uninstalling component " + job.getComponentId(), ex);
                uninstallStatus = JobStatus.UNINSTALL_ERROR;
            }
            jobRepository.updateJobStatus(job.getId(), uninstallStatus);
        });
    }

    private JobStatus uninstallComponent(EntandoBundleJob job, List<EntandoBundleComponentJob> components) {

        try {
            cleanupResourceFolder(job, components);
        } catch (Exception e) {
            throw new JobExecutionException("An error occurred while cleaning up component "
                    + job.getComponentId() + " resources", e);
        }

        CompletableFuture[] completableFutures = components.stream()
                .map(component -> {
                    if (component.getStatus() == JobStatus.INSTALL_COMPLETED
                            && component.getComponentType() != ComponentType.RESOURCE) {
                        EntandoBundleComponentJob ujc = component.duplicate();
                        ujc.setJob(job);
                        ujc.setStatus(JobStatus.UNINSTALL_IN_PROGRESS);
                        return componentRepository.save(ujc);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .map(ujc -> {
                    CompletableFuture<Void> future = deleteComponent(ujc);
                    future.thenAccept(justVoid -> componentRepository
                            .updateJobStatus(ujc.getId(), JobStatus.UNINSTALL_COMPLETED));
                    future.exceptionally(ex -> {
                        log.error("Error while trying to uninstall component {}", ujc.getId(), ex);
                        componentRepository
                                .updateJobStatus(ujc.getId(), JobStatus.UNINSTALL_ERROR, ex.getMessage());
                        return null;
                    });
                    return future;
                })
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(completableFutures).join();
        return JobStatus.UNINSTALL_COMPLETED;
    }

    private void cleanupResourceFolder(EntandoBundleJob job, List<EntandoBundleComponentJob> components) {
        Optional<EntandoBundleComponentJob> rootResourceFolder = components.stream().filter(component ->
                component.getComponentType() == ComponentType.RESOURCE
                        && component.getName().equals("/" + job.getComponentId())
        ).findFirst();

        if (rootResourceFolder.isPresent()) {
            engineService.deleteFolder("/" + job.getComponentId());
            components.stream().filter(component -> component.getComponentType() == ComponentType.RESOURCE)
                    .forEach(component -> {
                        EntandoBundleComponentJob uninstalledJobComponent = component.duplicate();
                        uninstalledJobComponent.setJob(job);
                        uninstalledJobComponent.setStatus(JobStatus.UNINSTALL_COMPLETED);
                        componentRepository.save(uninstalledJobComponent);
                    });
        }
    }

    private CompletableFuture<Void> deleteComponent(final EntandoBundleComponentJob component) {
        return CompletableFuture.runAsync(() -> componentProcessors.stream()
                .filter(processor -> processor.shouldProcess(component.getComponentType()))
                .forEach(processor -> processor.uninstall(component))
        );
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) {
        componentProcessors = applicationContext.getBeansOfType(ComponentProcessor.class).values();
    }

}
