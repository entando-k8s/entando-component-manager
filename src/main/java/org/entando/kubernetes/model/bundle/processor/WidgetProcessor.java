package org.entando.kubernetes.model.bundle.processor;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.AnalysisReport;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallActionsByComponentType;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallRequest.InstallAction;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.installable.WidgetInstallable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.reportable.EntandoEngineReportableProcessor;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.springframework.stereotype.Service;

/**
 * Processor to create Widgets, can handle descriptors with custom UI embedded or a separate custom UI file.
 *
 * @author Sergio Marcelino
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WidgetProcessor extends BaseComponentProcessor<WidgetDescriptor> implements EntandoEngineReportableProcessor {

    private final EntandoCoreClient engineService;

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.WIDGET;
    }

    @Override
    public Class<WidgetDescriptor> getDescriptorClass() {
        return WidgetDescriptor.class;
    }

    @Override
    public Optional<Function<ComponentSpecDescriptor, List<String>>> getComponentSelectionFn() {
        return Optional.of(ComponentSpecDescriptor::getWidgets);
    }

    @Override
    public List<Installable<WidgetDescriptor>> process(BundleReader bundleReader) {
        return this.process(bundleReader, InstallAction.CREATE, new InstallActionsByComponentType(),
                new AnalysisReport());
    }

    @Override
    public List<Installable<WidgetDescriptor>> process(BundleReader bundleReader, InstallAction conflictStrategy,
            InstallActionsByComponentType actions, AnalysisReport report) {
        try {
            BundleDescriptor descriptor = bundleReader.readBundleDescriptor();

            final List<String> descriptorList = getDescriptorList(bundleReader);
            final List<Installable<WidgetDescriptor>> installables = new LinkedList<>();

            for (final String fileName : descriptorList) {
                final WidgetDescriptor widgetDescriptor = bundleReader
                        .readDescriptorFile(fileName, WidgetDescriptor.class);
                if (widgetDescriptor.getCustomUiPath() != null) {
                    String widgetUiPath = getRelativePath(fileName, widgetDescriptor.getCustomUiPath());
                    widgetDescriptor.setCustomUi(bundleReader.readFileAsString(widgetUiPath));
                }
                widgetDescriptor.setBundleId(descriptor.getCode());
                InstallAction action = extractInstallAction(widgetDescriptor.getCode(), actions, conflictStrategy, report);
                installables.add(new WidgetInstallable(engineService, widgetDescriptor, action));
            }

            return installables;
        } catch (IOException e) {
            throw new EntandoComponentManagerException("Error reading bundle", e);
        }
    }

    @Override
    public List<Installable<WidgetDescriptor>> process(List<EntandoBundleComponentJobEntity> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == ComponentType.WIDGET)
                .map(c -> new WidgetInstallable(engineService, this.buildDescriptorFromComponentJob(c), c.getAction()))
                .collect(Collectors.toList());
    }

    @Override
    public WidgetDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component) {
        return WidgetDescriptor.builder()
                .code(component.getComponentId())
                .build();
    }

}
