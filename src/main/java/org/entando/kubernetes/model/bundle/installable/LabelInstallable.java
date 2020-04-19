package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.bundle.descriptor.LabelDescriptor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.client.core.EntandoCoreClient;

@Slf4j
public class LabelInstallable extends Installable<LabelDescriptor> {

    private EntandoCoreClient engineService;

    public LabelInstallable(EntandoCoreClient engineService,  LabelDescriptor labelDescriptor) {
        super(labelDescriptor);
        this.engineService = engineService;
    }

    @Override
    public CompletableFuture install() {
        return CompletableFuture.runAsync(() -> {
            log.info("Registering Label {}", representation.getKey());
            engineService.registerLabel(representation);
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
