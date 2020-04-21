package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.model.bundle.descriptor.ContentModelDescriptor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;

@Slf4j
public class ContentModelInstallable extends Installable<ContentModelDescriptor> {

    private final EntandoCoreClient engineService;

    public ContentModelInstallable(EntandoCoreClient service,
            final ContentModelDescriptor contentModelDescriptor) {
        super(contentModelDescriptor);
        this.engineService = service;
    }

    @Override
    public CompletableFuture<Void> install() {
        return CompletableFuture.runAsync(() -> {
            log.info("Registering Content Model {}", representation.getId());
            engineService.registerContentModel(representation);
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

}
