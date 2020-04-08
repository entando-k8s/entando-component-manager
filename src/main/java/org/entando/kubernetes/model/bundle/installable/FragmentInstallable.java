package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.bundle.descriptor.FragmentDescriptor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.service.digitalexchange.entandocore.EntandoCoreService;

@Slf4j
public class FragmentInstallable extends Installable<FragmentDescriptor> {

    private EntandoCoreService engineService;

    public FragmentInstallable(EntandoCoreService engineService, FragmentDescriptor fragmentDescriptor) {
        super(fragmentDescriptor);
        this.engineService = engineService;
    }

    @Override
    public CompletableFuture install() {
        return CompletableFuture.runAsync(() -> {
            log.info("Registering Fragment {}", representation.getCode());
            engineService.registerFragment(representation);
        });
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.GUI_FRAGMENT;
    }

    @Override
    public String getName() {
        return representation.getCode();
    }

}
