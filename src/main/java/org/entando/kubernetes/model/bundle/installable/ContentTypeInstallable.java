package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.bundle.descriptor.ContentTypeDescriptor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.client.core.EntandoCoreClient;

@Slf4j
public class ContentTypeInstallable extends Installable<ContentTypeDescriptor> {

    private EntandoCoreClient engineService;

    public ContentTypeInstallable(EntandoCoreClient service, ContentTypeDescriptor contentTypeDescriptor) {
        super(contentTypeDescriptor);
        this.engineService = service;
    }

    @Override
    public CompletableFuture install() {
        return CompletableFuture.runAsync(() -> {
            log.info("Registering Content Type {}", representation.getCode());
            engineService.registerContentType(representation);
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

}
