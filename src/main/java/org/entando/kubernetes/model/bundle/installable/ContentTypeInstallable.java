package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.config.tenant.thread.ContextCompletableFuture;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.contenttype.ContentTypeDescriptor;

@Slf4j
public class ContentTypeInstallable extends Installable<ContentTypeDescriptor> {

    private final EntandoCoreClient engineService;

    public ContentTypeInstallable(EntandoCoreClient service, ContentTypeDescriptor contentTypeDescriptor,
            InstallAction action) {
        super(contentTypeDescriptor, action);
        this.engineService = service;
    }

    @Override
    public CompletableFuture<Void> install() {
        return ContextCompletableFuture.runAsync(() -> {

            logConflictStrategyAction();

            if (shouldSkip()) {
                return; //Do nothing
            }

            if (shouldCreate()) {
                engineService.createContentType(representation);
            } else {
                engineService.updateContentType(representation);
            }
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
