package org.entando.kubernetes.service.digitalexchange.installable.processors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.service.digitalexchange.entandocore.EntandoEngineService;
import org.entando.kubernetes.service.digitalexchange.installable.ComponentProcessor;
import org.entando.kubernetes.service.digitalexchange.installable.Installable;
import org.entando.kubernetes.service.digitalexchange.job.ZipReader;
import org.entando.kubernetes.service.digitalexchange.job.model.ComponentDescriptor;
import org.entando.kubernetes.service.digitalexchange.job.model.ComponentSpecDescriptor;
import org.entando.kubernetes.service.digitalexchange.job.model.LabelDescriptor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Optional.ofNullable;

/**
 * Processor to create Labels
 *
 * @author Sergio Marcelino
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LabelProcessor implements ComponentProcessor {

    private final EntandoEngineService engineService;

    @Override
    public List<? extends Installable> process(final DigitalExchangeJob job, final ZipReader zipReader,
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
