package org.entando.kubernetes.model.bundle.installable;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallRequest.InstallAction;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.PageConfigurationDescriptor;

@Slf4j
public class PageConfigurationInstallable extends Installable<PageConfigurationDescriptor> {

    private final EntandoCoreClient engineService;

    public PageConfigurationInstallable(EntandoCoreClient engineService, PageConfigurationDescriptor pd,
            InstallAction action) {
        super(pd, action);
        this.engineService = engineService;
    }

    @Override
    public CompletableFuture<Void> install() {
        return CompletableFuture.runAsync(() -> {

            logConflictStrategyAction();

            //Create Page
            if (shouldSkip()) {
                return; //Do nothing
            }

            engineService.createPageConfiguration(representation);

            //Configure Page Widgets
            Optional.ofNullable(representation.getWidgets())
                    .orElse(new ArrayList<>())
                    .parallelStream()
                    .forEach(w -> engineService.configurePageWidget(representation, w));
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
