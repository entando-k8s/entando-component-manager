package org.entando.kubernetes.model.bundle.installable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.model.job.ComponentDataEntity;
import org.entando.kubernetes.repository.ComponentDataRepository;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

@Slf4j
public class WidgetInstallable extends Installable<WidgetDescriptor> {

    private final EntandoCoreClient engineService;
    private final ComponentDataRepository componentDataRepository;
    private final ObjectMapper jsonMapper = new ObjectMapper();

    public WidgetInstallable(EntandoCoreClient engineService, WidgetDescriptor widgetDescriptor, InstallAction action,
            ComponentDataRepository componentDataRepository) {
        super(widgetDescriptor, action);
        this.engineService = engineService;
        this.componentDataRepository = componentDataRepository;
    }

    @Override
    public CompletableFuture<Void> install() {
        return CompletableFuture.runAsync(() -> {

            logConflictStrategyAction();

            if (shouldSkip()) {
                return; //Do nothing
            }

            finalizeConfigUI(representation);

            if (!representation.isAuxiliary()) {
                if (shouldCreate()) {
                    engineService.createWidget(representation);
                } else {
                    engineService.updateWidget(representation);
                }
            }

            // FIXME usually save on db and than do web service to rollback transaction, here we don't have transaction ???
            ComponentDataEntity widgetComponentEntity = retrieveWidgetFromDb().orElse(convertDescriptorToEntity());
            componentDataRepository.save(widgetComponentEntity);

        });
    }

    private void finalizeConfigUI(WidgetDescriptor representation) {
        var customUi = representation.getCustomUi();
        representation.setCustomUi(
                representation.getDescriptorMetadata().getTemplateGeneratorService().updateWidgetTemplate(
                        customUi,
                        representation.getApiClaims(),
                        representation.getDescriptorMetadata().getBundleId())
        );
    }

    @Override
    public CompletableFuture<Void> uninstall() {
        return CompletableFuture.runAsync(() -> {
            log.info("Removing Widget {}", getName());
            if (shouldCreate() && !representation.isAuxiliary()) {
                engineService.deleteWidget(getName());
            }
            retrieveWidgetFromDb().ifPresent(componentDataRepository::delete);
        });
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.WIDGET;
    }

    @Override
    public String getName() {
        return representation.getCode();
    }

    private Optional<ComponentDataEntity> retrieveWidgetFromDb() {
        return componentDataRepository.findByComponentTypeAndComponentCode(ComponentType.WIDGET,
                representation.getCode());
    }

    private ComponentDataEntity convertDescriptorToEntity() {
        String widgetDescriptor = null;
        try {
            widgetDescriptor = jsonMapper.writeValueAsString(representation);
        } catch (JsonProcessingException ex) {
            log.error("error unmarshalling widgetDescriptor from object with code:'{}'",
                    representation.getCode(), ex);
            throw Problem.valueOf(Status.INTERNAL_SERVER_ERROR,
                    String.format(
                            "error unmarshalling widgetDescriptor from object with code:'%s' error:'%s'",
                            representation.getCode(), ex.getMessage()));
        }
        return ComponentDataEntity.builder()
                .bundleId(representation.getDescriptorMetadata().getBundleId())
                .componentType(ComponentType.WIDGET)
                .componentSubType(representation.getType())
                .componentId(null)
                .componentName(representation.getName())
                .componentCode(representation.getCode())
                .componentGroup(representation.getGroup())
                .componentDescriptor(widgetDescriptor)
                .build();
    }
}
