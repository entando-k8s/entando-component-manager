package org.entando.kubernetes.model.bundle.processor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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
import org.entando.kubernetes.model.digitalexchange.EntandoBundleJob;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FragmentProcessor implements ComponentProcessor {

    private final EntandoCoreClient engineService;

    @Override
    public ComponentType getComponentType() {
        return ComponentType.FRAGMENT;
    }

    @Override
    public List<Installable> process(EntandoBundleJob job, BundleReader npr) {
        try {
            ComponentDescriptor descriptor = npr.readBundleDescriptor();

            Optional<List<String>> optionalFragments = Optional.ofNullable(descriptor.getComponents().getFragments());
            List<Installable> installableList = new ArrayList<>();

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
    public List<Installable> process(List<EntandoBundleComponentJob> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == ComponentType.FRAGMENT)
                .map(c -> new FragmentInstallable(engineService, c))
                .collect(Collectors.toList());
    }

}
