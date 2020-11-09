/**
 * page creation process is split it 2 phases:
 * initialization   => create the page with stub data                              => PageInitializationInstallable
 * population       => populate page created at previous step with correct data    => PagePopulationInstallable
 */

package org.entando.kubernetes.model.bundle.installable;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallRequest.InstallAction;
import org.entando.kubernetes.model.bundle.ComponentInstallationFlow;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;

@Slf4j
public class PageInitializationInstallable extends Installable<PageDescriptor> {

    private final EntandoCoreClient engineService;

    public PageInitializationInstallable(EntandoCoreClient engineService, PageDescriptor pd, InstallAction action) {
        super(pd, action);
        this.engineService = engineService;
    }

    @Override
    public CompletableFuture<Void> install() {
        return CompletableFuture.runAsync(() -> {
            log.info("Registering Stub Page {}", getName());

            if (shouldSkip()) {
                return; //Do nothing
            }

            if (shouldCreate()) {
                engineService.createPage(representation);
            } else {
                engineService.updatePage(representation);
            }

            //Configure Page Widgets
//            Optional.ofNullable(representation.getWidgets())
//                    .orElse(new ArrayList<>())
//                    .parallelStream()
//                    .forEach(w -> engineService.configurePageWidget(representation, w));
        });
    }

    @Override
    public CompletableFuture<Void> uninstall() {
        return CompletableFuture.runAsync(() -> {
            log.info("Removing Page {}", getName());
            if(shouldCreate()) {
                engineService.deletePage(getName());
            }
        });
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.PAGE;
    }

    @Override
    public ComponentInstallationFlow getComponentInstallationFlow() {
        return ComponentInstallationFlow.PAGE_INIT;
    }

    @Override
    public String getName() {
        return representation.getCode();
    }

}
