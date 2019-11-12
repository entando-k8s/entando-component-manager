package org.entando.kubernetes.service.digitalexchange;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJobComponent;
import org.entando.kubernetes.model.digitalexchange.JobStatus;
import org.entando.kubernetes.repository.DigitalExchangeJobComponentRepository;
import org.entando.kubernetes.repository.DigitalExchangeJobRepository;
import org.entando.kubernetes.service.digitalexchange.entandocore.EntandoEngineService;
import org.entando.kubernetes.service.digitalexchange.installable.ComponentProcessor;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class DigitalExchangeUninstallService implements ApplicationContextAware {

    private final @NonNull DigitalExchangeJobRepository jobRepository;
    private final @NonNull DigitalExchangeJobComponentRepository componentRepository;
    private final @NonNull EntandoEngineService engineService;

    private Collection<ComponentProcessor> componentProcessors = new ArrayList<>();

    public DigitalExchangeJob uninstall(String componentId) {
        DigitalExchangeJob job = jobRepository.findByComponentIdAndStatusNotEqual(componentId, JobStatus.UNINSTALL_COMPLETED)
                .orElse(null);
        if (job == null || (job.getStatus() != JobStatus.INSTALL_ERROR && job.getStatus() != JobStatus.INSTALL_COMPLETED)) {
            return null;
        }

        List<DigitalExchangeJobComponent> components = componentRepository.findAllByJob(job);

        jobRepository.updateJobStatus(job.getId(), JobStatus.UNINSTALL_IN_PROGRESS);
        job.setStatus(JobStatus.UNINSTALL_IN_PROGRESS);

        submitUninstallJob(job, components);

        return job;
    }

    private void submitUninstallJob(DigitalExchangeJob job, List<DigitalExchangeJobComponent> components) {
        cleanupResourceFolder(job, components);

        CompletableFuture[] completableFutures = components.stream().map(component -> {
            if (component.getStatus() == JobStatus.INSTALL_COMPLETED && component.getComponentType() != ComponentType.RESOURCE) {

                return (CompletableFuture) deleteComponent(component)
                        .thenApply(object -> {
                            componentRepository.updateJobStatus(component.getId(), JobStatus.UNINSTALL_COMPLETED);
                            return object;
                        })
                        .exceptionally(ex -> {
                            log.error("Error while trying to uninstall component {}", component.getId(), ex);
                            componentRepository.updateJobStatus(component.getId(), JobStatus.UNINSTALL_ERROR, ex.getMessage());
                            return null;
                        });
            }

            return null;
        }).filter(Objects::nonNull).toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(completableFutures)
                .thenApply(v -> JobStatus.UNINSTALL_COMPLETED)
                .exceptionally( th -> {
                    log.error("An error occurred while uninstalling components of job " + job.getId(), th);
                    return JobStatus.UNINSTALL_ERROR;
                })
                .thenAccept(status -> jobRepository.updateJobStatus(job.getId(), status));
    }

    private void cleanupResourceFolder(DigitalExchangeJob job, List<DigitalExchangeJobComponent> components) {
        Optional<DigitalExchangeJobComponent> rootResourceFolder = components.stream().filter(component ->
                component.getComponentType() == ComponentType.RESOURCE
                        && component.getName().equals("/" + job.getComponentId())
        ).findFirst();

        if (rootResourceFolder.isPresent()) {
            engineService.deleteFolder("/" + job.getComponentId());
            components.stream().filter(component -> component.getComponentType() == ComponentType.RESOURCE)
                    .forEach(component -> componentRepository.updateJobStatus(component.getId(), JobStatus.UNINSTALL_COMPLETED));
        }
    }

    private CompletableFuture<?> deleteComponent(DigitalExchangeJobComponent component) {
        return CompletableFuture.runAsync(() ->
            componentProcessors.stream()
                    .filter(processor -> processor.shouldProcess(component.getComponentType()))
                    .forEach(processor -> processor.uninstall(component))
        );
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        componentProcessors = applicationContext.getBeansOfType(ComponentProcessor.class).values();
    }

}
