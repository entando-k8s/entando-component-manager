package org.entando.kubernetes.model.bundle.processor;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.bundle.BundleReader;
import org.entando.kubernetes.model.bundle.descriptor.ComponentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.LabelDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.installable.LabelInstallable;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleJob;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleComponentJob;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.springframework.stereotype.Service;

/**
 * Processor to create Labels
 *
 * @author Sergio Marcelino
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LabelProcessor implements ComponentProcessor {

    private final EntandoCoreClient engineService;

    @Override
    public List<Installable> process(final EntandoBundleJob job, final BundleReader npr,
                                               final ComponentDescriptor descriptor) throws IOException {

        final Optional<List<LabelDescriptor>> labelDescriptors = ofNullable(descriptor.getComponents()).map(ComponentSpecDescriptor::getLabels);
        final List<Installable> installables = new LinkedList<>();

        if (labelDescriptors.isPresent()) {
            for (final LabelDescriptor labelDescriptor : labelDescriptors.get()) {
                installables.add(new LabelInstallable(engineService, labelDescriptor));
            }
        }

        return installables;
    }

    @Override
    public boolean shouldProcess(final ComponentType componentType) {
        return componentType == ComponentType.LABEL;
    }

    @Override
    public void uninstall(final EntandoBundleComponentJob component) {
        log.info("Removing Label {}", component.getName());
        engineService.deleteLabel(component.getName());
    }

}
