package org.entando.kubernetes.model.bundle.processor;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.BundleReader;
import org.entando.kubernetes.model.bundle.descriptor.ComponentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageTemplateDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.installable.PageTemplateInstallable;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleComponentJob;
import org.springframework.stereotype.Service;

/**
 * Processor to handle Page Templates
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PageTemplateProcessor implements ComponentProcessor<PageTemplateDescriptor> {

    private final EntandoCoreClient engineService;

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.PAGE_TEMPLATE;
    }

    @Override
    public List<Installable<PageTemplateDescriptor>> process(BundleReader npr) {
        try {
            ComponentDescriptor descriptor = npr.readBundleDescriptor();
            List<String> pageTemplatesDescriptor = ofNullable(descriptor.getComponents())
                    .map(ComponentSpecDescriptor::getPageModels)
                    .orElse(Collections.emptyList());

            List<Installable<PageTemplateDescriptor>> installables = new LinkedList<>();

            for (String fileName : pageTemplatesDescriptor) {
                PageTemplateDescriptor pageTemplateDescriptor = npr.readDescriptorFile(fileName, PageTemplateDescriptor.class);
                if (pageTemplateDescriptor.getTemplatePath() != null) {
                    String tp = getRelativePath(fileName, pageTemplateDescriptor.getTemplatePath());
                    pageTemplateDescriptor.setTemplate(npr.readFileAsString(tp));
                }
                installables.add(new PageTemplateInstallable(engineService, pageTemplateDescriptor));
            }

            return installables;
        } catch (IOException e) {
            throw new EntandoComponentManagerException("Error reading bundle", e);
        }
    }

    @Override
    public List<Installable<PageTemplateDescriptor>> process(List<EntandoBundleComponentJob> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == ComponentType.PAGE_TEMPLATE)
                .map(c -> new PageTemplateInstallable(engineService, this.buildDescriptorFromComponentJob(c)))
                .collect(Collectors.toList());
    }

    @Override
    public PageTemplateDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJob component) {
        return PageTemplateDescriptor.builder()
                .code(component.getName())
                .build();
    }

}
