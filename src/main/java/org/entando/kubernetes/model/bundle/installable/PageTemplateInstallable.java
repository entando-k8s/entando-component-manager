package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.model.bundle.descriptor.PageTemplateDescriptor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;

@Slf4j
public class PageTemplateInstallable extends Installable<PageTemplateDescriptor> {

    private final EntandoCoreClient engineService;

    public PageTemplateInstallable(EntandoCoreClient engineService, PageTemplateDescriptor pageTemplateDescriptor) {
        super(pageTemplateDescriptor);
        this.engineService = engineService;
    }

    @Override
    public CompletableFuture<Void> install() {
        return CompletableFuture.runAsync(() -> {
            log.info("Registering Page Model {}", getName());
            engineService.registerPageModel(representation);
        });
    }

    @Override
    public CompletableFuture<Void> uninstall() {
        return CompletableFuture.runAsync(() -> {
            log.info("Removing PageTemplate {}", getName());
            engineService.deletePageModel(getName());
        });
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.PAGE_TEMPLATE;
    }

    @Override
    public String getName() {
        return representation.getCode();
    }

    @Override
    public InstallPriority getInstallPriority() {
        return InstallPriority.PAGE_TEMPLATE;
    }
}
