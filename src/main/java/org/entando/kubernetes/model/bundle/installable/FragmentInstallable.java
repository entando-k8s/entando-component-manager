package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.model.bundle.descriptor.FragmentDescriptor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;

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
            log.info("Registering Fragment {}", representation.getCode());
            engineService.registerFragment(representation);
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
