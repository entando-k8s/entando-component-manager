package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.content.ContentDescriptor;

@Slf4j
public class ContentInstallable extends Installable<ContentDescriptor> {

    private final EntandoCoreClient engineService;

    public ContentInstallable(EntandoCoreClient service, ContentDescriptor contentDescriptor) {
        super(contentDescriptor);
        this.engineService = service;
    }

    @Override
    public CompletableFuture<Void> install() {
        return CompletableFuture.runAsync(() -> {
            log.info("Registering Content {}", getName());
            engineService.registerContent(representation);
        });
    }

    @Override
    public CompletableFuture<Void> uninstall() {
        return CompletableFuture.runAsync(() -> {
            log.info("Removing Content {}", getName());
            engineService.deleteContent(getName());
        });
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.CONTENT;
    }

    @Override
    public String getName() {
        return representation.getId();
    }


}