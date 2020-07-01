package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.model.bundle.descriptor.LabelDescriptor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;

@Slf4j
public class LabelInstallable extends Installable<LabelDescriptor> {

    private final EntandoCoreClient engineService;

    public LabelInstallable(EntandoCoreClient engineService, LabelDescriptor labelDescriptor) {
        super(labelDescriptor);
        this.engineService = engineService;
    }

    @Override
    public CompletableFuture<Void> install() {
        return CompletableFuture.runAsync(() -> {
            log.info("Registering Label {}", getName());
            engineService.registerLabel(representation);
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
        return ComponentType.LABEL;
    }

    @Override
    public String getName() {
        return representation.getKey();
    }

}
