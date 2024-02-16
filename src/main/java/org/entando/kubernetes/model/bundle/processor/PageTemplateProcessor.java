package org.entando.kubernetes.model.bundle.processor;

import static org.entando.kubernetes.service.digitalexchange.BundleUtilities.removeProtocolAndGetBundleId;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FrameDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageTemplateConfigurationDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageTemplateDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.installable.PageTemplateInstallable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.reportable.EntandoEngineReportableProcessor;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * Processor to handle Page Templates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PageTemplateProcessor extends BaseComponentProcessor<PageTemplateDescriptor> implements
        EntandoEngineReportableProcessor {

    private final EntandoCoreClient engineService;

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.PAGE_TEMPLATE;
    }

    @Override
    public Class<PageTemplateDescriptor> getDescriptorClass() {
        return PageTemplateDescriptor.class;
    }

    @Override
    public Optional<Function<ComponentSpecDescriptor, List<String>>> getComponentSelectionFn() {
        return Optional.of(ComponentSpecDescriptor::getPageTemplates);
    }

    @Override
    public List<Installable<PageTemplateDescriptor>> process(BundleReader bundleReader) {
        return this.process(bundleReader, InstallAction.CREATE, new InstallPlan());
    }

    @Override
    public List<Installable<PageTemplateDescriptor>> process(BundleReader bundleReader, InstallAction conflictStrategy,
            InstallPlan installPlan) {

        List<Installable<PageTemplateDescriptor>> installables = new LinkedList<>();

        try {
            final List<String> descriptorList = getDescriptorList(bundleReader);
            final String bundleId = removeProtocolAndGetBundleId(bundleReader.getBundleUrl());

            for (String fileName : descriptorList) {
                PageTemplateDescriptor pageTemplateDescriptor = bundleReader
                        .readDescriptorFile(fileName, PageTemplateDescriptor.class);
                if (pageTemplateDescriptor.getTemplatePath() != null) {
                    String tp = getRelativePath(fileName, pageTemplateDescriptor.getTemplatePath());
                    pageTemplateDescriptor.setTemplate(bundleReader.readFileAsString(tp));
                }
                replaceBundleIdPlaceholderInDescriptorProps(bundleId, pageTemplateDescriptor);
                InstallAction action = extractInstallAction(pageTemplateDescriptor.getCode(), conflictStrategy,
                        installPlan);
                installables.add(new PageTemplateInstallable(engineService, pageTemplateDescriptor, action));
            }

        } catch (IOException e) {
            throw makeMeaningfulException(e);
        }

        return installables;
    }

    @Override
    public List<Installable<PageTemplateDescriptor>> process(List<EntandoBundleComponentJobEntity> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == getSupportedComponentType())
                .map(c -> new PageTemplateInstallable(engineService, this.buildDescriptorFromComponentJob(c),
                        c.getAction()))
                .collect(Collectors.toList());
    }

    @Override
    public PageTemplateDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component) {
        return PageTemplateDescriptor.builder()
                .code(component.getComponentId())
                .build();
    }

    private void replaceBundleIdPlaceholderInDescriptorProps(String bundleId, PageTemplateDescriptor descriptor) {

        ProcessorHelper.replaceBundleIdPlaceholderInConsumer(bundleId, descriptor::getCode,
                descriptor::setCode);

        final PageTemplateConfigurationDescriptor configurationDesc = descriptor.getConfiguration();
        if (configurationDesc == null || CollectionUtils.isEmpty(configurationDesc.getFrames())) {
            return;
        }

        final List<FrameDescriptor> frames = configurationDesc.getFrames().stream()
                .filter(Objects::nonNull)
                .map(f -> {
                    if (f.getDefaultWidget() != null) {
                        ProcessorHelper.replaceBundleIdPlaceholderInConsumer(bundleId,
                                f.getDefaultWidget()::getCode, f.getDefaultWidget()::setCode);
                    }
                    return f;
                })
                .collect(Collectors.toList());

        configurationDesc.setFrames(frames);
    }
}
