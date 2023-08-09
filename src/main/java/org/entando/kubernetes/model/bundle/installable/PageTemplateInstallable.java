package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.config.tenant.thread.ContextCompletableFuture;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.PageTemplateDescriptor;

@Slf4j
public class PageTemplateInstallable extends Installable<PageTemplateDescriptor> {

    private final EntandoCoreClient engineService;

    public PageTemplateInstallable(EntandoCoreClient engineService, PageTemplateDescriptor pageTemplateDescriptor,
            InstallAction action) {
        super(pageTemplateDescriptor, action);
        this.engineService = engineService;
    }

    @Override
    public CompletableFuture<Void> install() {
        return ContextCompletableFuture.runAsync(() -> {

            logConflictStrategyAction();

            if (shouldSkip()) {
                return; //Do nothing
            }

            if (shouldCreate()) {
                engineService.createPageTemplate(representation);
            } else {
                engineService.updatePageTemplate(representation);
            }
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

}
