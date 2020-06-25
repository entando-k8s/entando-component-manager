package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.model.bundle.descriptor.WidgetDescriptor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleComponentJob;

@Slf4j
public class WidgetInstallable extends Installable<WidgetDescriptor> {

    private final EntandoCoreClient engineService;

    public WidgetInstallable(EntandoCoreClient engineService, WidgetDescriptor widgetDescriptor) {
        super(widgetDescriptor);
        this.engineService = engineService;
    }

    public WidgetInstallable(EntandoCoreClient service, EntandoBundleComponentJob component) {
        super(component);
        this.engineService = service;
    }

    @Override
    public CompletableFuture<Void> install() {
        return CompletableFuture.runAsync(() -> {
            log.info("Registering Widget {}", getName());
            engineService.registerWidget(representation);
        });
    }

    @Override
    public CompletableFuture<Void> uninstall() {
        return CompletableFuture.runAsync(() -> {
            log.info("Removing Widget {}", getName());
            engineService.deleteWidget(getName());
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

    @Override
    public WidgetDescriptor representationFromComponent(EntandoBundleComponentJob component) {
        return WidgetDescriptor.builder()
                .code(component.getName())
                .build();
    }

    @Override
    public InstallPriority getInstallPriority() {
        return InstallPriority.WIDGET;
    }
}
