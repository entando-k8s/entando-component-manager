package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.config.tenant.thread.ContextCompletableFuture;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.ContentTemplateDescriptor;

@Slf4j
public class ContentTemplateInstallable extends Installable<ContentTemplateDescriptor> {

    private final EntandoCoreClient engineService;

    public ContentTemplateInstallable(EntandoCoreClient service, ContentTemplateDescriptor contentTemplateDescriptor,
            InstallAction action) {
        super(contentTemplateDescriptor, action);
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
                engineService.createContentTemplate(representation);
            } else {
                engineService.updateContentTemplate(representation);
            }
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
