package org.entando.kubernetes.model.bundle.processor;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.DescriptorMetadata;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.installable.WidgetInstallable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.reportable.EntandoEngineReportableProcessor;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.service.digitalexchange.templating.WidgetTemplateGeneratorService;
import org.entando.kubernetes.validator.descriptor.WidgetDescriptorValidator;
import org.springframework.stereotype.Service;

/**
 * Processor to create Widgets, can handle descriptors with custom UI embedded or a separate custom UI file.
 *
 * @author Sergio Marcelino
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WidgetProcessor extends BaseComponentProcessor<WidgetDescriptor> implements
        EntandoEngineReportableProcessor {

    private final EntandoCoreClient engineService;
    private final WidgetTemplateGeneratorService templateGeneratorService;
    private final WidgetDescriptorValidator descriptorValidator;
    @Setter
    private Map<String, String> pluginIngressPathMap;

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
        return this.process(bundleReader, InstallAction.CREATE, new InstallPlan());
    }

    @Override
    public List<Installable<WidgetDescriptor>> process(BundleReader bundleReader, InstallAction conflictStrategy,
            InstallPlan installPlan) {

        final List<Installable<WidgetDescriptor>> installables = new LinkedList<>();

        try {
            BundleDescriptor descriptor = bundleReader.readBundleDescriptor();

            final List<String> descriptorList = getDescriptorList(bundleReader);

            for (final String fileName : descriptorList) {
                final WidgetDescriptor widgetDescriptor = bundleReader
                        .readDescriptorFile(fileName, WidgetDescriptor.class);
                widgetDescriptor.setDescriptorMetadata(new DescriptorMetadata(pluginIngressPathMap));
                descriptorValidator.validateOrThrow(widgetDescriptor);

                setWidgetCode(bundleReader, widgetDescriptor);
                setCustomUi(widgetDescriptor, fileName, bundleReader);

                widgetDescriptor.setBundleId(descriptor.getCode());
                InstallAction action = extractInstallAction(widgetDescriptor.getCode(), conflictStrategy, installPlan);
                installables.add(new WidgetInstallable(engineService, widgetDescriptor, action));
            }

        } catch (IOException e) {
            throw makeMeaningfulException(e);
        }

        return installables;
    }

    @Override
    public List<Installable<WidgetDescriptor>> process(List<EntandoBundleComponentJobEntity> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == getSupportedComponentType())
                .map(c -> new WidgetInstallable(engineService, this.buildDescriptorFromComponentJob(c), c.getAction()))
                .collect(Collectors.toList());
    }

    /**
     * depending of descriptor version => read or generate the ftl custom ui.
     *
     * @param widgetDescriptor the widget descriptor on which operate on
     * @param fileName         the filename with path of the widget descriptor
     * @param bundleReader     the bundle reader used to access the bundle files
     * @throws IOException if any file access error occurs
     */
    private void setCustomUi(WidgetDescriptor widgetDescriptor, String fileName, BundleReader bundleReader)
            throws IOException {

        if (widgetDescriptor.isVersion1()) {
            if (widgetDescriptor.getCustomUiPath() != null) {
                String widgetUiPath = getRelativePath(fileName, widgetDescriptor.getCustomUiPath());
                widgetDescriptor.setCustomUi(bundleReader.readFileAsString(widgetUiPath));
            }
        } else {
            String ftl = templateGeneratorService.generateWidgetTemplate(fileName, widgetDescriptor, bundleReader);
            widgetDescriptor.setCustomUi(ftl);
        }
    }

    /**
     * set the widget code depending on the widget descriptor version. ensure that the widget code is signed with the
     * bundle id hash
     *
     * @param bundleReader     the bundle reader to use to read the bundle id
     * @param widgetDescriptor the widget descriptor to get widget info
     */
    protected void setWidgetCode(BundleReader bundleReader, WidgetDescriptor widgetDescriptor) {
        String code = widgetDescriptor.getCode();
        String name = widgetDescriptor.getName();
        String bundleIdHash = BundleUtilities.removeProtocolAndGetBundleId(bundleReader.getBundleUrl());

        if (widgetDescriptor.isVersion1()) {
            if (!code.endsWith(bundleIdHash)) {
                code = BundleUtilities.appendBundleUrlHash(bundleReader, code);
            }
        } else {
            code = BundleUtilities.appendBundleUrlHash(bundleReader, name);
        }
        widgetDescriptor.setCode(code);
    }

    @Override
    public WidgetDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component) {
        return WidgetDescriptor.builder()
                .code(component.getComponentId())
                .build();
    }

}
