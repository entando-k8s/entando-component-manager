package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;

@Slf4j
public class PageInstallable extends Installable<PageDescriptor> {

    private EntandoCoreClient engineService;

    public PageInstallable(EntandoCoreClient engineService, PageDescriptor pd) {
        super(pd);
        this.engineService = engineService;
    }

    @Override
    public CompletableFuture install() {
        return CompletableFuture.runAsync(() -> {
            log.info("Registering Page {}", representation.getCode());
            engineService.registerPage(representation);
        });
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.PAGE;
    }

    @Override
    public String getName() {
        return representation.getCode();
    }

}
