package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.bundle.descriptor.WidgetDescriptor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.client.core.EntandoCoreClient;

@Slf4j
public class WidgetInstallable extends Installable<WidgetDescriptor> {

    private EntandoCoreClient engineService;

    public WidgetInstallable(EntandoCoreClient engineService, WidgetDescriptor widgetDescriptor) {
        super(widgetDescriptor);
        this.engineService = engineService;
    }

    @Override
    public CompletableFuture install() {
        return CompletableFuture.runAsync(() -> {
            log.info("Registering Widget {}", representation.getCode());
            engineService.registerWidget(representation);
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