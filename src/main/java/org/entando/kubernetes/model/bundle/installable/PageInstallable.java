package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;

@Slf4j
public class PageInstallable extends Installable<PageDescriptor> {

    private final EntandoCoreClient engineService;

    public PageInstallable(EntandoCoreClient engineService, PageDescriptor pd) {
        super(pd);
        this.engineService = engineService;
    }

    @Override
    public CompletableFuture<Void> install() {
        return CompletableFuture.runAsync(() -> {
            log.info("Registering Page {}", getName());
            engineService.registerPage(representation);
        });
    }

    @Override
    public CompletableFuture<Void> uninstall() {
        return CompletableFuture.runAsync(() -> {
            log.info("Removing Page {}", getName());
            engineService.deletePage(getName());
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

    @Override
    public InstallPriority getInstallPriority() {
        return InstallPriority.PAGE;
    }
}
