package org.entando.kubernetes.model.bundle.installable;

import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.model.bundle.descriptor.ContentTemplateDescriptor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class ContentTemplateInstallable extends Installable<ContentTemplateDescriptor> {

    private final EntandoCoreClient engineService;

    public ContentTemplateInstallable(EntandoCoreClient service, ContentTemplateDescriptor contentTemplateDescriptor) {
        super(contentTemplateDescriptor);
        this.engineService = service;
    }

    @Override
    public CompletableFuture<Void> install() {
        return CompletableFuture.runAsync(() -> {
            log.info("Registering Content Template {}", getName());
            engineService.registerContentModel(representation);
        });
    }

    @Override
    public CompletableFuture<Void> uninstall() {
        return CompletableFuture.runAsync(() -> {
            log.info("Removing Content Template {}", getName());
            engineService.deleteContentModel(getName());
        });
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.CONTENT_TEMPLATE;
    }

    @Override
    public String getName() {
        return representation.getId();
    }

    @Override
    public InstallPriority getInstallPriority() {
        return InstallPriority.CONTENT_TEMPLATE;
    }
}
