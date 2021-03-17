package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.GroupDescriptor;

@Slf4j
public class GroupInstallable extends Installable<GroupDescriptor> {

    private final EntandoCoreClient engineService;

    public GroupInstallable(EntandoCoreClient engineService, GroupDescriptor descriptor, InstallAction action) {
        super(descriptor, action);
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
                engineService.createGroup(representation);
            } else {
                engineService.updateGroup(representation);
            }
        });
    }

    @Override
    public CompletableFuture<Void> uninstall() {
        return CompletableFuture.runAsync(() -> {
            log.info("Removing Group {}", getName());
            if (shouldCreate()) {
                engineService.deleteGroup(getName());
            }
        });
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.GROUP;
    }

    @Override
    public String getName() {
        return representation.getCode();
    }

}
