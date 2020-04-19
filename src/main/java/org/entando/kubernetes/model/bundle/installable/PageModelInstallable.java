package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.bundle.descriptor.PageModelDescriptor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.client.core.EntandoCoreClient;

@Slf4j
public class PageModelInstallable extends Installable<PageModelDescriptor> {

    private EntandoCoreClient engineService;

    public PageModelInstallable(EntandoCoreClient engineService, PageModelDescriptor pageModelDescriptor) {
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
