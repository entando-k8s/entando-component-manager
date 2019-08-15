package org.entando.kubernetes.service.digitalexchange.installable;

import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.service.digitalexchange.entandocore.EntandoEngineService;
import org.entando.kubernetes.service.digitalexchange.job.model.WidgetDescriptor;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class WidgetInstallable extends Installable<WidgetDescriptor> {

    private final EntandoEngineService engineService;

    public WidgetInstallable(final WidgetDescriptor widgetDescriptor, final EntandoEngineService engineService) {
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
