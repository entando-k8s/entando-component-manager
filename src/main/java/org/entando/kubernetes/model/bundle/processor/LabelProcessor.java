package org.entando.kubernetes.model.bundle.processor;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.bundle.ZipReader;
import org.entando.kubernetes.model.bundle.descriptor.ComponentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.LabelDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJobComponent;
import org.entando.kubernetes.service.digitalexchange.entandocore.EntandoCoreService;
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

    private final EntandoCoreService engineService;

    @Override
    public List<Installable> process(final DigitalExchangeJob job, final ZipReader zipReader,
                                               final ComponentDescriptor descriptor) throws IOException {

        final Optional<List<LabelDescriptor>> labelDescriptors = ofNullable(descriptor.getComponents()).map(ComponentSpecDescriptor::getLabels);
        final List<Installable> installables = new LinkedList<>();

        if (labelDescriptors.isPresent()) {
            for (final LabelDescriptor labelDescriptor : labelDescriptors.get()) {
                installables.add(new LabelInstallable(labelDescriptor));
            }
        }

        return installables;
    }

    @Override
    public boolean shouldProcess(final ComponentType componentType) {
        return componentType == ComponentType.LABEL;
    }

    @Override
    public void uninstall(final DigitalExchangeJobComponent component) {
        log.info("Removing Label {}", component.getName());
        engineService.deleteLabel(component.getName());
    }

    public class LabelInstallable extends Installable<LabelDescriptor> {

        private LabelInstallable(final LabelDescriptor labelDescriptor) {
            super(labelDescriptor);
        }

        @Override
        public CompletableFuture install() {
            return CompletableFuture.runAsync(() -> {
                log.info("Registering Label {}", representation.getKey());
                engineService.registerLabel(representation);
            });
        }

        @Override
        public ComponentType getComponentType() {
            return ComponentType.LABEL;
        }

        @Override
        public String getName() {
            return representation.getKey();
        }

    }
}
