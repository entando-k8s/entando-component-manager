package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.LabelDescriptor;

@Slf4j
public class LabelInstallable extends Installable<LabelDescriptor> {

    private final EntandoCoreClient engineService;

    public LabelInstallable(EntandoCoreClient engineService, LabelDescriptor labelDescriptor, InstallAction action) {
        super(labelDescriptor, action);
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
                engineService.createLabel(representation);
            } else {
                engineService.updateLabel(representation);
            }
        });
    }

    @Override
    public CompletableFuture<Void> uninstallFromEcr() {
        return CompletableFuture.runAsync(() -> log.info("Removing Label {}", getName()));
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.LABEL;
    }

    @Override
    public String getName() {
        return representation.getKey();
    }

}
