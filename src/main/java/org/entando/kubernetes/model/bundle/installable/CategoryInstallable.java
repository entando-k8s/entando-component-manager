package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.model.bundle.descriptor.CategoryDescriptor;
import org.entando.kubernetes.model.bundle.ComponentType;

@Slf4j
public class CategoryInstallable extends Installable<CategoryDescriptor> {

    private final EntandoCoreClient engineService;

    public CategoryInstallable(EntandoCoreClient engineService, CategoryDescriptor descriptor) {
        super(descriptor);
        this.engineService = engineService;
    }

    @Override
    public CompletableFuture<Void> install() {
        return CompletableFuture.runAsync(() -> {
            log.info("Registering Label {}", getName());
            engineService.registerCategory(representation);
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
