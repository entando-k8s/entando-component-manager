package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallRequest.InstallAction;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.FragmentDescriptor;

@Slf4j
public class FragmentInstallable extends Installable<FragmentDescriptor> {

    private final EntandoCoreClient engineService;

    public FragmentInstallable(EntandoCoreClient engineService, FragmentDescriptor fragmentDescriptor,
            InstallAction action) {
        super(fragmentDescriptor, action);
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
                engineService.createFragment(representation);
            } else {
                engineService.updateFragment(representation);
            }
        });
    }

    @Override
    public CompletableFuture<Void> uninstall() {
        return CompletableFuture.runAsync(() -> {
            log.info("Removing Fragment {}", getName());
            if (shouldCreate()) {
                engineService.deleteFragment(getName());
            }
        });
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.FRAGMENT;
    }

    @Override
    public String getName() {
        return representation.getCode();
    }

}
