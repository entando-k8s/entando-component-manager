package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.bundle.descriptor.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.processor.WidgetProcessor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.service.digitalexchange.entandocore.EntandoCoreService;

@Slf4j
public class WidgetInstallable extends Installable<WidgetDescriptor> {

    private EntandoCoreService engineService;

    public WidgetInstallable(EntandoCoreService engineService, WidgetDescriptor widgetDescriptor) {
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
