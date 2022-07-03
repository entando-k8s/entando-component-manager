package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;

@Slf4j
public class WidgetInstallable extends Installable<WidgetDescriptor> {

    private final EntandoCoreClient engineService;

    public WidgetInstallable(EntandoCoreClient engineService, WidgetDescriptor widgetDescriptor, InstallAction action) {
        super(widgetDescriptor, action);
        this.engineService = engineService;
    }

    @Override
    public CompletableFuture<Void> install() {
        return CompletableFuture.runAsync(() -> {

            logConflictStrategyAction();

            if (shouldSkip()) {
                return; //Do nothing
            }

            finalizeConfigUI(representation);

            if (shouldCreate()) {
                engineService.createWidget(representation);
            } else {
                engineService.updateWidget(representation);
            }
        });
    }

    private void finalizeConfigUI(WidgetDescriptor representation) {
        var customUi = representation.getCustomUi();
        representation.setCustomUi(
                representation.getDescriptorMetadata().getTemplateGeneratorService().updateWidgetTemplate(
                        customUi,
                        representation.getApiClaims(),
                        representation.getDescriptorMetadata().getPluginIngressPathMap(),
                        representation.getDescriptorMetadata().getBundleId())
        );
    }

    @Override
    public CompletableFuture<Void> uninstall() {
        return CompletableFuture.runAsync(() -> {
            log.info("Removing Widget {}", getName());
            if (shouldCreate()) {
                engineService.deleteWidget(getName());
            }
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

}
