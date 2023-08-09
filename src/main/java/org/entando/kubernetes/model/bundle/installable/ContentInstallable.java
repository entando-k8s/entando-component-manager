package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.config.tenant.thread.ContextCompletableFuture;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.content.ContentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.content.ContentDescriptor.ContentStatus;

@Slf4j
public class ContentInstallable extends Installable<ContentDescriptor> {

    private final EntandoCoreClient engineService;

    public ContentInstallable(EntandoCoreClient service, ContentDescriptor contentDescriptor, InstallAction action) {
        super(contentDescriptor, action);
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
                engineService.createContent(representation);
            } else {
                engineService.updateContent(representation);
            }

            if (ContentStatus.PUBLIC.toString().equals(representation.getStatus())) {
                engineService.publishContent(representation);
            }
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
