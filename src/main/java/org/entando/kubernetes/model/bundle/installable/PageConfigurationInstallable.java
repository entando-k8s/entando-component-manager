package org.entando.kubernetes.model.bundle.installable;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;

@Slf4j
public class PageConfigurationInstallable extends Installable<PageDescriptor> {

    private final EntandoCoreClient engineService;

    public PageConfigurationInstallable(EntandoCoreClient engineService, PageDescriptor pd,
            InstallAction action) {
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

            engineService.updatePageConfiguration(representation);

            //Configure Page Widgets
            Optional.ofNullable(representation.getWidgets())
                    .orElse(new ArrayList<>())
                    .parallelStream()
                    .forEach(w -> engineService.configurePageWidget(representation, w));

            //Publish Page
            engineService.setPageStatus(representation.getCode(), EntandoCoreClient.PUBLISHED);
        });
    }

    @Override
    public CompletableFuture<Void> uninstall() {
        return CompletableFuture.runAsync(() -> {
            // UNINSTALL IN THE PageInstallable
        });
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.PAGE_CONFIGURATION;
    }

    @Override
    public String getName() {
        return representation.getCode();
    }

}
