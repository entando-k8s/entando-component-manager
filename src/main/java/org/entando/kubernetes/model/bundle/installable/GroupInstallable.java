package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.model.bundle.descriptor.GroupDescriptor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;

@Slf4j
public class GroupInstallable extends Installable<GroupDescriptor> {

    private final EntandoCoreClient engineService;

    public GroupInstallable(EntandoCoreClient engineService, GroupDescriptor descriptor) {
        super(descriptor);
        this.engineService = engineService;
    }

    @Override
    public CompletableFuture<Void> install() {
        return CompletableFuture.runAsync(() -> {
            log.info("Registering Label {}", getName());
            engineService.registerGroup(representation);
        });
    }

    @Override
    public CompletableFuture<Void> uninstall() {
        return CompletableFuture.runAsync(() -> {
            log.info("Removing Label {}", getName());
            engineService.deleteLabel(getName());
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
