package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
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

            logConflictStrategyAction();

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
    public CompletableFuture<Void> uninstallFromEcr() {
        return CompletableFuture.runAsync(() -> log.info("Removing Category {}", getName()));
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
