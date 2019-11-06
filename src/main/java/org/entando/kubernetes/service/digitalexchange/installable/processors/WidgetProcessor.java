package org.entando.kubernetes.service.digitalexchange.installable.processors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJobComponent;
import org.entando.kubernetes.model.digitalexchange.InstallableInstallResult;
import org.entando.kubernetes.service.digitalexchange.entandocore.EntandoEngineService;
import org.entando.kubernetes.service.digitalexchange.installable.ComponentProcessor;
import org.entando.kubernetes.service.digitalexchange.installable.Installable;
import org.entando.kubernetes.service.digitalexchange.job.ZipReader;
import org.entando.kubernetes.service.digitalexchange.job.model.ComponentDescriptor;
import org.entando.kubernetes.service.digitalexchange.job.model.ComponentSpecDescriptor;
import org.entando.kubernetes.service.digitalexchange.job.model.WidgetDescriptor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Optional.ofNullable;

/**
 * Processor to create Widgets, can handle descriptors
 * with custom UI embedded or a separate custom UI file.
 *
 * @author Sergio Marcelino
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WidgetProcessor implements ComponentProcessor {

    private final EntandoEngineService engineService;

    @Override
    public List<? extends Installable> process(final DigitalExchangeJob job, final ZipReader zipReader,
                                               final ComponentDescriptor descriptor) throws IOException {

        final Optional<List<String>> widgetsDescriptor = ofNullable(descriptor.getComponents()).map(ComponentSpecDescriptor::getWidgets);
        final List<Installable> installables = new LinkedList<>();

        if (widgetsDescriptor.isPresent()) {
            for (final String fileName : widgetsDescriptor.get()) {
                final WidgetDescriptor widgetDescriptor = zipReader.readDescriptorFile(fileName, WidgetDescriptor.class);
                if (widgetDescriptor.getCustomUiPath() != null) {
                    widgetDescriptor.setCustomUi(zipReader.readFileAsString(getFolder(fileName), widgetDescriptor.getCustomUiPath()));
                }
                installables.add(new WidgetInstallable(widgetDescriptor));
            }
        }

        return installables;
    }

    @Override
    public boolean shouldProcess(final ComponentType componentType) {
        return componentType == ComponentType.WIDGET;
    }

    @Override
    public void uninstall(final DigitalExchangeJobComponent component) {
        log.info("Removing Widget {}", component.getName());
        engineService.deleteWidget(component.getName());
    }

    public class WidgetInstallable extends Installable<WidgetDescriptor> {

        private WidgetInstallable(final WidgetDescriptor widgetDescriptor) {
            super(widgetDescriptor);
        }

        @Override
        public CompletableFuture<InstallableInstallResult> install() {
            return CompletableFuture.supplyAsync(() -> {
                log.info("Registering Widget {}", representation.getCode());
                return wrap(() -> engineService.registerWidget(representation));
            });
        }

        @Override
        public ComponentType getComponentType() {
            return ComponentType.WIDGET;
        }

        @Override
        public String getName() {
            return representation.getCode();
        }

    }
}
