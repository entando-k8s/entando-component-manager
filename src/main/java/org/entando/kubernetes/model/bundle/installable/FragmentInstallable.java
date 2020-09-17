package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.model.bundle.descriptor.FragmentDescriptor;
import org.entando.kubernetes.model.bundle.ComponentType;

@Slf4j
public class FragmentInstallable extends Installable<FragmentDescriptor> {

    private final EntandoCoreClient engineService;

    public FragmentInstallable(EntandoCoreClient engineService, FragmentDescriptor fragmentDescriptor) {
        super(fragmentDescriptor);
        this.engineService = engineService;
    }

    @Override
    public CompletableFuture<Void> install() {
        return CompletableFuture.runAsync(() -> {
            log.info("Registering Fragment {}", getName());
            engineService.registerFragment(representation);
        });
    }

    @Override
    public CompletableFuture<Void> uninstall() {
        return CompletableFuture.runAsync(() -> {
            log.info("Removing Fragment {}", getName());
            engineService.deleteFragment(getName());
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
