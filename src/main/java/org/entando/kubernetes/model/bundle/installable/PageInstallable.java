package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;

@Slf4j
public class PageInstallable extends Installable<PageDescriptor> {

    private final EntandoCoreClient engineService;

    public PageInstallable(EntandoCoreClient engineService, PageDescriptor pd, InstallAction action) {
        super(pd, action);
        this.engineService = engineService;
    }

    @Override
    public CompletableFuture<Void> install() {
        return CompletableFuture.runAsync(() -> {

            logConflictStrategyAction();

            if (shouldSkip()) {
                return; //Do nothing
            }

            if (shouldCreate()) {
                engineService.createPage(representation);
            }

            if (representation.getStatus().equals("published")) {
                engineService.setPageStatus(getName(), EntandoCoreClient.PUBLISHED);
            }
        });
    }

    @Override
    public CompletableFuture<Void> uninstall() {
        return CompletableFuture.runAsync(() -> {
            log.info("Removing Page {}", getName());
            if (shouldCreate()) {
                // unpublish
                engineService.setPageStatus(getName(), EntandoCoreClient.DRAFT);
                // delete
                engineService.deletePage(getName());
            }
        });
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.PAGE;
    }

    @Override
    public String getName() {
        return representation.getCode();
    }

}
