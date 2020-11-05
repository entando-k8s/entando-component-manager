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
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageTemplateDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.installable.PageTemplateInstallable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.reportable.EntandoEngineReportableProcessor;
import org.entando.kubernetes.model.bundle.reportable.ReportableComponentProcessor;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.springframework.stereotype.Service;

/**
 * Processor to handle Page Templates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PageTemplateProcessor extends BaseComponentProcessor<PageTemplateDescriptor> implements EntandoEngineReportableProcessor {

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
        return this.process(bundleReader, InstallAction.CREATE, new InstallActionsByComponentType(),
                new AnalysisReport());
    }

    @Override
    public List<Installable<PageTemplateDescriptor>> process(BundleReader bundleReader, InstallAction conflictStrategy,
            InstallActionsByComponentType actions, AnalysisReport report) {
        try {
            final List<String> descriptorList = getDescriptorList(bundleReader);

            List<Installable<PageTemplateDescriptor>> installables = new LinkedList<>();

            for (String fileName : descriptorList) {
                PageTemplateDescriptor pageTemplateDescriptor = bundleReader.readDescriptorFile(fileName, PageTemplateDescriptor.class);
                if (pageTemplateDescriptor.getTemplatePath() != null) {
                    String tp = getRelativePath(fileName, pageTemplateDescriptor.getTemplatePath());
                    pageTemplateDescriptor.setTemplate(bundleReader.readFileAsString(tp));
                }
                InstallAction action = extractInstallAction(pageTemplateDescriptor.getCode(), actions, conflictStrategy, report);
                installables.add(new PageTemplateInstallable(engineService, pageTemplateDescriptor, action));
            }

            return installables;
        } catch (IOException e) {
            throw new EntandoComponentManagerException("Error reading bundle", e);
        }
    }

    @Override
    public List<Installable<PageTemplateDescriptor>> process(List<EntandoBundleComponentJobEntity> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == ComponentType.PAGE_TEMPLATE)
                .map(c -> new PageTemplateInstallable(engineService, this.buildDescriptorFromComponentJob(c), c.getAction()))
                .collect(Collectors.toList());
    }

    @Override
    public PageTemplateDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component) {
        return PageTemplateDescriptor.builder()
                .code(component.getComponentId())
                .build();
    }

}
