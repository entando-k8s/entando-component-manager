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
import org.entando.kubernetes.exception.job.JobConflictException;
import org.entando.kubernetes.exception.job.JobExecutionException;
import org.entando.kubernetes.exception.k8ssvc.K8SServiceClientException;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJobComponent;
import org.entando.kubernetes.model.digitalexchange.JobStatus;
import org.entando.kubernetes.model.digitalexchange.JobType;
import org.entando.kubernetes.repository.DigitalExchangeJobComponentRepository;
import org.entando.kubernetes.repository.DigitalExchangeJobRepository;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.entandocore.EntandoCoreService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DigitalExchangeUninstallService implements ApplicationContextAware {

    private final @NonNull DigitalExchangeJobRepository jobRepository;
    private final @NonNull DigitalExchangeJobComponentRepository componentRepository;
    private final @NonNull EntandoCoreService engineService;
    private final @NonNull KubernetesService k8sService;

    private Collection<ComponentProcessor> componentProcessors = new ArrayList<>();

    public DigitalExchangeJob uninstall(String componentId) {
        EntandoDeBundle bundle = k8sService.getBundleByName(componentId)
                .orElseThrow(() -> new K8SServiceClientException("Bundle with name " + componentId + " not found"));
        DigitalExchangeJob lastAvailableJob = getLastAvaialableJob(bundle)
                .orElseThrow(() -> new RuntimeException("No job found for " + componentId));

        verifyJobStatusCompatibleWithUninstall(lastAvailableJob);

        DigitalExchangeJob uninstallJob;
        if (JobType.isOfType(lastAvailableJob.getStatus(), JobType.INSTALL)) {
            uninstallJob = submitNewUninstallJob(lastAvailableJob);
        } else {
            DigitalExchangeJob lastInstallAttemptJob = findLastInstallJob(bundle)
                    .orElseThrow(() -> new RuntimeException("No install job associated with " + componentId + " has been found"));
            uninstallJob = submitNewUninstallJob(lastInstallAttemptJob);
        }

        return uninstallJob;

    }

    private void verifyJobStatusCompatibleWithUninstall(DigitalExchangeJob job) {
        if (JobType.isOfType(job.getStatus(), JobType.UNFINISHED)) {
            throw new JobConflictException(
                    "Install job for the component " + job.getComponentId() + " is in progress - JOB ID: " + job.getId());
        }
    }

    private Optional<DigitalExchangeJob> getLastAvaialableJob(EntandoDeBundle bundle) {
        String digitalExchange = bundle.getMetadata().getNamespace();
        String componentId = bundle.getMetadata().getName();

        return jobRepository.findFirstByDigitalExchangeAndComponentIdOrderByStartedAtDesc(digitalExchange, componentId);
    }

    private Optional<DigitalExchangeJob> findLastInstallJob(EntandoDeBundle bundle) {
        String digitalExchange = bundle.getMetadata().getNamespace();
        String componentId = bundle.getMetadata().getName();
        return jobRepository.findAllByDigitalExchangeAndComponentIdOrderByStartedAtDesc(digitalExchange, componentId)
                .stream()
                .filter(j -> JobType.isOfType(j.getStatus(), JobType.INSTALL))
                .findFirst();
    }

    private DigitalExchangeJob submitNewUninstallJob(DigitalExchangeJob lastAvailableJob) {
        final List<DigitalExchangeJobComponent> components = componentRepository.findAllByJob(lastAvailableJob);

        DigitalExchangeJob uninstallJob = new DigitalExchangeJob();
        uninstallJob.setComponentId(lastAvailableJob.getComponentId());
        uninstallJob.setComponentName(lastAvailableJob.getComponentName());
        uninstallJob.setDigitalExchange(lastAvailableJob.getDigitalExchange());
        uninstallJob.setComponentVersion(lastAvailableJob.getComponentVersion());
        uninstallJob.setStartedAt(LocalDateTime.now());
        uninstallJob.setStatus(JobStatus.UNINSTALL_CREATED);
        uninstallJob.setProgress(0.0);

        DigitalExchangeJob savedJob = jobRepository.save(uninstallJob);
        submitUninstallAsync(uninstallJob, components);

        return savedJob;
    }

    private void submitUninstallAsync(DigitalExchangeJob job, List<DigitalExchangeJobComponent> components) {
        CompletableFuture.runAsync(() -> {
            jobRepository.updateJobStatus(job.getId(), JobStatus.UNINSTALL_IN_PROGRESS);

            try {
                cleanupResourceFolder(job, components);
            } catch (Exception e) {
                jobRepository.updateJobStatus(job.getId(), JobStatus.UNINSTALL_ERROR);
                throw new JobExecutionException("An error occurred while cleaning up component "
                        + job.getComponentId() + " resources", e);
            }

            CompletableFuture[] completableFutures = components.stream()
                    .map(component -> {
                        if (component.getStatus() == JobStatus.INSTALL_COMPLETED
                                && component.getComponentType() != ComponentType.RESOURCE) {
                            DigitalExchangeJobComponent ujc = component.duplicate();
                            ujc.setJob(job);
                            ujc.setStatus(JobStatus.UNINSTALL_IN_PROGRESS);
                            return componentRepository.save(ujc);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .map(ujc -> {
                        CompletableFuture<Void> future = deleteComponent(ujc);
                        future.thenAccept(justVoid -> componentRepository.updateJobStatus(ujc.getId(), JobStatus.UNINSTALL_COMPLETED));
                        future.exceptionally(ex -> {
                            log.error("Error while trying to uninstall component {}", ujc.getId(), ex);
                            componentRepository
                                    .updateJobStatus(ujc.getId(), JobStatus.UNINSTALL_ERROR, ex.getMessage());
                            return null;
                        });
                        return future;
                    })
                    .toArray(CompletableFuture[]::new);

            CompletableFuture.allOf(completableFutures).whenComplete((object, ex) -> {
                JobStatus status = ex == null ? JobStatus.UNINSTALL_COMPLETED : JobStatus.UNINSTALL_ERROR;
                jobRepository.updateJobStatus(job.getId(), status);
            });
        });
    }

    private void cleanupResourceFolder(DigitalExchangeJob job, List<DigitalExchangeJobComponent> components) {
        Optional<DigitalExchangeJobComponent> rootResourceFolder = components.stream().filter(component ->
                component.getComponentType() == ComponentType.RESOURCE
                        && component.getName().equals("/" + job.getComponentId())
        ).findFirst();

        if (rootResourceFolder.isPresent()) {
            engineService.deleteFolder("/" + job.getComponentId());
            components.stream().filter(component -> component.getComponentType() == ComponentType.RESOURCE)
                    .forEach(component -> {
                        DigitalExchangeJobComponent uninstalledJobComponent = component.duplicate();
                        uninstalledJobComponent.setJob(job);
                        uninstalledJobComponent.setStatus(JobStatus.UNINSTALL_COMPLETED);
                        componentRepository.save(uninstalledJobComponent);
                    });
        }
    }

    private CompletableFuture<Void> deleteComponent(final DigitalExchangeJobComponent component) {
        return CompletableFuture.runAsync(() -> {
                    componentProcessors.stream()
                            .filter(processor -> processor.shouldProcess(component.getComponentType()))
                            .forEach(processor -> processor.uninstall(component));
                }
        );
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) {
        componentProcessors = applicationContext.getBeansOfType(ComponentProcessor.class).values();
    }

}
