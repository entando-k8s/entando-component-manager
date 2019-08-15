package org.entando.kubernetes.service.digitalexchange.installable;

import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.service.digitalexchange.entandocore.EntandoEngineService;
import org.entando.kubernetes.service.digitalexchange.job.model.PageModelDescriptor;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class PageModelInstallable extends Installable<PageModelDescriptor> {

    private final EntandoEngineService engineService;

    public PageModelInstallable(final PageModelDescriptor pageModelDescriptor, final EntandoEngineService engineService) {
        super(pageModelDescriptor);
        this.engineService = engineService;
    }

    @Override
    public CompletableFuture install() {
        return CompletableFuture.runAsync(() -> {
            log.info("Registering Page Model {}", representation.getCode());
            engineService.registerPageModel(representation);
        });
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.PAGE_MODEL;
    }

    @Override
    public String getName() {
        return representation.getCode();
    }

}
