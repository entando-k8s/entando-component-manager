package org.entando.kubernetes.model.bundle.processor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.bundle.NpmPackageReader;
import org.entando.kubernetes.model.bundle.ZipReader;
import org.entando.kubernetes.model.bundle.descriptor.ComponentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FragmentDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJobComponent;
import org.entando.kubernetes.service.digitalexchange.entandocore.EntandoCoreService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FragmentProcessor implements ComponentProcessor {

    private final EntandoCoreService engineService;

    @Override
    public List<Installable> process(DigitalExchangeJob job, NpmPackageReader npr, ComponentDescriptor descriptor)
            throws IOException {
        Optional<List<String>> optionalFragments = Optional.ofNullable(descriptor.getComponents().getFragments());
        List<Installable> installableList = new ArrayList<>();

        if (optionalFragments.isPresent()) {
            for (String fileName: optionalFragments.get() ) {
                FragmentDescriptor frDesc = npr.readDescriptorFile(fileName, FragmentDescriptor.class);
                if (frDesc.getGuiCodePath() != null) {
                    String gcp = getRelativePath(fileName, frDesc.getGuiCodePath());
                    frDesc.setGuiCode(npr.readFileAsString(gcp));
                }
                installableList.add(new FragmentInstallable(frDesc));
            }
        }
        return installableList;
    }

    @Override
    public boolean shouldProcess(ComponentType componentType) {
        return componentType.equals(ComponentType.GUI_FRAGMENT);
    }

    @Override
    public void uninstall(DigitalExchangeJobComponent component) {
        log.info("Removing Fragment {}", component.getName());
        engineService.deleteFragment(component.getName());
    }

    public class FragmentInstallable extends Installable<FragmentDescriptor> {

        private FragmentInstallable(FragmentDescriptor fragmentDescriptor) {
            super(fragmentDescriptor);
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
}
