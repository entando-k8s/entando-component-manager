package org.entando.kubernetes.model.bundle.installable;

import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.model.bundle.descriptor.ContentTypeDescriptor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class ContentTypeInstallable extends Installable<ContentTypeDescriptor> {

    private final EntandoCoreClient engineService;

    public ContentTypeInstallable(EntandoCoreClient service, ContentTypeDescriptor contentTypeDescriptor) {
        super(contentTypeDescriptor);
        this.engineService = service;
    }

    @Override
    public CompletableFuture<Void> install() {
        return CompletableFuture.runAsync(() -> {
            log.info("Registering Content Type {}", getName());
            engineService.registerContentType(representation);
        });
    }

    @Override
    public CompletableFuture<Void> uninstall() {
        return CompletableFuture.runAsync(() -> {
            log.info("Removing Content Type {}", getName());
            engineService.deleteContentType(getName());
        });
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.CONTENT_TYPE;
    }

    @Override
    public String getName() {
        return representation.getCode();
    }


    @Override
    public InstallPriority getInstallPriority() {
        return InstallPriority.CONTENT_TYPE;
    }
}
