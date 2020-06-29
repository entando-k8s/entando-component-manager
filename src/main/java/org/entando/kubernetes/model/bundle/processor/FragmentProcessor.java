package org.entando.kubernetes.model.bundle.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.BundleReader;
import org.entando.kubernetes.model.bundle.descriptor.ComponentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FragmentDescriptor;
import org.entando.kubernetes.model.bundle.installable.FragmentInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleComponentJob;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FragmentProcessor implements ComponentProcessor<FragmentDescriptor> {

    private final EntandoCoreClient engineService;

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.FRAGMENT;
    }

    @Override
    public List<Installable<FragmentDescriptor>> process(BundleReader npr) {
        try {
            ComponentDescriptor descriptor = npr.readBundleDescriptor();

            Optional<List<String>> optionalFragments = Optional.ofNullable(descriptor.getComponents().getFragments());
            List<Installable<FragmentDescriptor>> installableList = new ArrayList<>();

            if (optionalFragments.isPresent()) {
                for (String fileName : optionalFragments.get()) {
                    FragmentDescriptor frDesc = npr.readDescriptorFile(fileName, FragmentDescriptor.class);
                    if (frDesc.getGuiCodePath() != null) {
                        String gcp = getRelativePath(fileName, frDesc.getGuiCodePath());
                        frDesc.setGuiCode(npr.readFileAsString(gcp));
                    }
                    installableList.add(new FragmentInstallable(engineService, frDesc));
                }
            }
            return installableList;
        } catch (IOException e) {
            throw new EntandoComponentManagerException("Error reading bundle", e);
        }
    }

    @Override
    public List<Installable<FragmentDescriptor>> process(List<EntandoBundleComponentJob> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == ComponentType.FRAGMENT)
                .map(c -> new FragmentInstallable(engineService, this.buildDescriptorFromComponentJob(c)))
                .collect(Collectors.toList());
    }

    @Override
    public FragmentDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJob component) {
        return FragmentDescriptor.builder()
                .code(component.getName())
                .build();
    }

}
