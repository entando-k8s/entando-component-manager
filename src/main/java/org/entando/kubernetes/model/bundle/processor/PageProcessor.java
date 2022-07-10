package org.entando.kubernetes.model.bundle.processor;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetConfigurationDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.installable.PageInstallable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.reportable.EntandoEngineReportableProcessor;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.validator.descriptor.PageDescriptorValidator;
import org.springframework.stereotype.Service;

/**
 * Processor to handle Pages.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PageProcessor extends BaseComponentProcessor<PageDescriptor> implements EntandoEngineReportableProcessor {
    
    private final EntandoCoreClient engineService;
    
    private final PageDescriptorValidator descriptorValidator;

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.PAGE;
    }

    @Override
    public Class<PageDescriptor> getDescriptorClass() {
        return PageDescriptor.class;
    }

    @Override
    public Optional<Function<ComponentSpecDescriptor, List<String>>> getComponentSelectionFn() {
        return Optional.of(ComponentSpecDescriptor::getPages);
    }

    @Override
    public List<Installable<PageDescriptor>> process(BundleReader bundleReader) {
        return this.process(bundleReader, InstallAction.CREATE, new InstallPlan());
    }

    @Override
    public List<Installable<PageDescriptor>> process(BundleReader bundleReader, InstallAction conflictStrategy,
            InstallPlan installPlan) {
        List<Installable<PageDescriptor>> installables = new LinkedList<>();
        try {
            final List<String> descriptorList = getDescriptorList(bundleReader);
            for (String fileName : descriptorList) {
                PageDescriptor pageDescriptor = bundleReader.readDescriptorFile(fileName, PageDescriptor.class);
                this.descriptorValidator.validateOrThrow(pageDescriptor);
                this.composeAndSetCode(pageDescriptor, bundleReader);
                Optional.ofNullable(pageDescriptor.getWidgets()).ifPresent(widgets -> {
                    widgets.stream().forEach(wd -> this.composeAndSetWidgetCode(wd, pageDescriptor, bundleReader));
                });
                InstallAction action = extractInstallAction(pageDescriptor.getCode(), conflictStrategy, installPlan);
                installables.add(new PageInstallable(engineService, pageDescriptor, action));
            }
        } catch (IOException e) {
            throw makeMeaningfulException(e);
        }
        return installables;
    }

    @Override
    public List<Installable<PageDescriptor>> process(List<EntandoBundleComponentJobEntity> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == getSupportedComponentType())
                .map(c -> new PageInstallable(engineService, this.buildDescriptorFromComponentJob(c), c.getAction()))
                .collect(Collectors.toList());
    }

    @Override
    public PageDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component) {
        return PageDescriptor.builder()
                .code(component.getComponentId())
                .build();
    }
    
    private void composeAndSetCode(PageDescriptor pageDescriptor, BundleReader bundleReader) {
        if (!pageDescriptor.isVersion1()) {
            // set the code and the parenCode
            if (StringUtils.isBlank(pageDescriptor.getCode())) {
                final String pageCode = BundleUtilities.composeDescriptorCode(pageDescriptor.getCode(),
                        pageDescriptor.getName(), pageDescriptor, bundleReader.getBundleUrl());
                pageDescriptor.setCode(pageCode);
            }
            if (StringUtils.isBlank(pageDescriptor.getParentCode())) {
                final String paretPageCode = BundleUtilities.composeDescriptorCode(pageDescriptor.getParentCode(),
                        pageDescriptor.getParentName(), pageDescriptor, bundleReader.getBundleUrl());
                pageDescriptor.setParentCode(paretPageCode);
            }
        }
    }
    
    private void composeAndSetWidgetCode(WidgetConfigurationDescriptor widgetDescriptor, PageDescriptor pageDescriptor, BundleReader bundleReader) {
        if (!pageDescriptor.isVersion1() && StringUtils.isBlank(widgetDescriptor.getCode())) {
            // set the code
            final String widgetCode = BundleUtilities.composeDescriptorCode(widgetDescriptor.getCode(),
                    widgetDescriptor.getName(), pageDescriptor, bundleReader.getBundleUrl());
            widgetDescriptor.setCode(widgetCode);
        }
    }
    
}
