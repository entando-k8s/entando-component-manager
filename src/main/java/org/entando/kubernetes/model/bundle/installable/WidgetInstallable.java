package org.entando.kubernetes.model.bundle.installable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.DescriptorMetadata;
import org.entando.kubernetes.model.job.ComponentDataEntity;
import org.entando.kubernetes.repository.ComponentDataRepository;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.service.digitalexchange.JSONUtilities;
import org.entando.kubernetes.service.digitalexchange.templating.WidgetTemplateGeneratorService.SystemParams;
import org.entando.kubernetes.validator.ValidationFunctions;

@Slf4j
public class WidgetInstallable extends Installable<WidgetDescriptor> {

    private final EntandoCoreClient engineService;
    private final ComponentDataRepository componentDataRepository;

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
                return; // Do nothing
            }

            if (representation.getType().equals(WidgetDescriptor.TYPE_WIDGET_STANDARD)) {
                finalizeConfigUI(representation);
            }

            if (shouldApplyToAppEngine(representation)) {
                if (shouldCreate()) {
                    engineService.createWidget(representation);
                } else {
                    engineService.updateWidget(representation);
                }
            }

            saveWidgetDefinitionToEcr();

        });
    }

    private void saveWidgetDefinitionToEcr() {
        if (representation.getType().equals(WidgetDescriptor.TYPE_WIDGET_APPBUILDER)) {
            finalizeMetadataSystemParams(representation);
        }
        ComponentDataEntity widgetComponentEntity = retrieveWidgetFromDbAndUpdate().orElseGet(
                this::convertDescriptorToEntity);
        componentDataRepository.save(widgetComponentEntity);
    }

    private void finalizeConfigUI(WidgetDescriptor representation) {
        var customUi = representation.getCustomUi();
        representation.setCustomUi(
                representation.getDescriptorMetadata().getTemplateGeneratorService().updateWidgetTemplate(
                        customUi,
                        representation.getApiClaims(),
                        representation.getDescriptorMetadata().getBundleId()));
    }

    private boolean shouldApplyToAppEngine(WidgetDescriptor descriptor) {
        if (descriptor.isVersion1()) {
            return true;
        } else {
            return WidgetDescriptor.TYPE_WIDGET_STANDARD.equals(descriptor.getType());
        }
    }

    private void finalizeMetadataSystemParams(WidgetDescriptor representation) {
        DescriptorMetadata originalMetadata = representation.getDescriptorMetadata();
        SystemParams systemParams = originalMetadata
                .getTemplateGeneratorService()
                .generateSystemParamsWithIngressPath(representation.getApiClaims(), originalMetadata.getBundleId());

        representation.setDescriptorMetadata(DescriptorMetadata.builder()
                .templateGeneratorService(originalMetadata.getTemplateGeneratorService())
                .bundleId(originalMetadata.getBundleId())
                .bundleCode(originalMetadata.getBundleCode())
                .filename(originalMetadata.getFilename())
                .systemParams(systemParams)
                .pluginIngressPathMap(originalMetadata.getPluginIngressPathMap())
                .assets(originalMetadata.getAssets()).build());

    }

    @Override
    public CompletableFuture<Void> uninstall() {
        return CompletableFuture.runAsync(() -> {
            log.info("Removing Widget {}", getName());
            if (shouldCreate() && shouldApplyToAppEngine(representation)) {
                engineService.deleteWidget(getName());
            }
            deleteWidgetDefinitionFromEcr();
        });
    }

    private void deleteWidgetDefinitionFromEcr() {
        retrieveWidgetFromDb().ifPresent(componentDataRepository::delete);
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

    private Optional<ComponentDataEntity> retrieveWidgetFromDbAndUpdate() {
        return retrieveWidgetFromDb().map(this::upgradeEntity);
    }

    private ComponentDataEntity upgradeEntity(ComponentDataEntity entity) {
        entity.setBundleId(representation.getDescriptorMetadata().getBundleId());
        entity.setComponentType(ComponentType.WIDGET);
        entity.setComponentSubType(representation.getType());
        entity.setComponentName(representation.getName());
        entity.setComponentCode(representation.getCode());
        entity.setComponentGroup(representation.getGroup());
        entity.setComponentDescriptor(mkWidgetCleanedUpDescriptor());
        return entity;
    }

    private ComponentDataEntity convertDescriptorToEntity() {

        String widgetDescriptor = mkWidgetCleanedUpDescriptor();

        return ComponentDataEntity.builder()
                .bundleId(representation.getDescriptorMetadata().getBundleId())
                .componentType(ComponentType.WIDGET)
                .componentSubType(representation.getType())
                .componentId(null)
                .componentName(composeWidgetName())
                .componentCode(representation.getCode())
                .componentGroup(representation.getGroup())
                .componentDescriptor(widgetDescriptor)
                .build();
    }

    private String mkWidgetCleanedUpDescriptor() {
        WidgetDescriptor cleanedUp = new WidgetDescriptor(
                representation.getDescriptorVersion(),
                representation.getCode(),
                representation.getTitles(),
                representation.getGroup(),
                representation.getCustomUiPath(),
                representation.getWidgetCategory(),
                null,
                null,
                representation.getConfigWidget(),
                representation.getName(),
                representation.getType(),
                representation.getConfigMfe(),
                representation.getApiClaims(),
                representation.getParams(),
                representation.getContextParams(),
                representation.getCustomElement(),
                representation.getExt(),
                representation.getParamsDefaults(),
                representation.getDescriptorMetadata(),
                representation.getParentName(),
                representation.getParentCode()
        );

        
        String res = JSONUtilities.serializeDescriptor(cleanedUp);
        if (res.length() >= MAX_COMMON_SIZE_OF_STRINGS) {
            log.warn("Detected possible overflow ({}) in deserialization of widgetDescriptor blob", res.length());
        }
        return res;
    }

    private String composeWidgetName() {
        if (StringUtils.isNotBlank(representation.getName())) {
            return representation.getName();
        }

        if (ValidationFunctions.isEntityCodeValid(representation.getCode())) {
            return BundleUtilities.extractNameFromEntityCode(representation.getCode());
        }

        return representation.getCode();
    }

}
