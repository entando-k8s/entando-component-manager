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
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.entandocore.EntandoEngineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class DigitalExchangeUninstallService {

    private final @NonNull DigitalExchangeJobRepository jobRepository;
    private final @NonNull DigitalExchangeJobComponentRepository componentRepository;
    private final @NonNull EntandoEngineService engineService;
    private final @NonNull KubernetesService kubernetesService;

    @Transactional(noRollbackFor = Throwable.class)
    public DigitalExchangeJob uninstall(final String componentId) {
        final DigitalExchangeJob job = jobRepository.findByComponentIdAndStatusNotEqual(componentId, JobStatus.UNINSTALLED)
                .orElse(null);
        if (job == null || (job.getStatus() != JobStatus.ERROR && job.getStatus() != JobStatus.COMPLETED)) {
            return null;
        }
        final List<DigitalExchangeJobComponent> components = componentRepository.findAllByJob(job);

        jobRepository.updateJobStatus(job.getId(), JobStatus.UNINSTALLING);

        final Optional<DigitalExchangeJobComponent> rootResourceFolder = components.stream().filter(component ->
                component.getComponentType() == ComponentType.RESOURCE
                        && component.getName().equals("/" + job.getComponentId())
        ).findFirst();

        if (rootResourceFolder.isPresent()) {
            engineService.deleteFolder("/" + job.getComponentId());
            components.stream().filter(component -> component.getComponentType() == ComponentType.RESOURCE)
                    .forEach(component -> componentRepository.updateJobStatus(component.getId(), JobStatus.UNINSTALLED));
        }

        components.forEach(component -> {
            if (component.getStatus() == JobStatus.COMPLETED && component.getComponentType() != ComponentType.RESOURCE) {
                final CompletableFuture<?> future = deleteComponent(component);
                future.exceptionally(ex -> {
                    log.error("Error while trying to uninstall component {}", component.getId(), ex);
                    componentRepository.updateJobStatus(component.getId(), JobStatus.ERROR_UNINSTALLING, ex.getMessage());
                    return null;
                });
                future.thenApply(object -> {
                    componentRepository.updateJobStatus(component.getId(), JobStatus.UNINSTALLED);
                    return object;
                });
            }
        });

        job.setStatus(JobStatus.UNINSTALLING);
        return job;
    }

    private CompletableFuture<?> deleteComponent(final DigitalExchangeJobComponent component) {
        return CompletableFuture.runAsync(() -> {
            switch (component.getComponentType()) {
                case WIDGET:
                    log.info("Removing Widget {}", component.getName());
                    engineService.deleteWidget(component.getName());
                    break;

                case PAGE_MODEL:
                    log.info("Removing PageModel {}", component.getName());
                    engineService.deletePageModel(component.getName());
                    break;

                case DEPLOYMENT:
                    log.info("Removing deployment {}", component.getName());
                    kubernetesService.deleteDeployment(component.getName());
                    break;

                case RESOURCE:
                    // do nothing because we already removed the parent folder
                    break;

                // add support
                case CONTENT_MODEL:
                    break;
                case CONTENT_TYPE:
                    break;
                case LABEL:
                    break;
            }
        });


    }

}
