package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallRequest.InstallAction;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.CategoryDescriptor;

@Slf4j
public class CategoryInstallable extends Installable<CategoryDescriptor> {

    private final EntandoCoreClient engineService;

    public CategoryInstallable(EntandoCoreClient engineService, CategoryDescriptor descriptor, InstallAction action) {
        super(descriptor, action);
        this.engineService = engineService;
    }

    @Override
    public CompletableFuture<Void> install() {
        return CompletableFuture.runAsync(() -> {
            log.info("Registering Category {}", getName());
            if (shouldSkip()) {
                return; //Do nothing
            }

            if (shouldCreate()) {
                engineService.createCategory(representation);
            } else {
                engineService.updateCategory(representation);
            }
        });
    }

    @Override
    public CompletableFuture<Void> uninstall() {
        return CompletableFuture.runAsync(() -> {
            log.info("Removing Label {}", getName());
            if (shouldCreate()) {
                engineService.deleteCategory(getName());
            }
        });
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.CATEGORY;
    }

    @Override
    public String getName() {
        return representation.getCode();
    }

}
