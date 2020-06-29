package org.entando.kubernetes.model.bundle.processor;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.util.ArrayList;
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
import org.entando.kubernetes.model.bundle.descriptor.ContentTemplateDescriptor;
import org.entando.kubernetes.model.bundle.installable.ContentTemplateInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleComponentJob;
import org.springframework.stereotype.Service;

/**
 * Processor to handle Bundles with CMS ContentTemplates
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentTemplateProcessor implements ComponentProcessor<ContentTemplateDescriptor> {

    private final EntandoCoreClient engineService;

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.CONTENT_TEMPLATE;
    }

    @Override
    public List<Installable<ContentTemplateDescriptor>> process(BundleReader npr) {
        try {
            ComponentDescriptor descriptor = npr.readBundleDescriptor();
            List<String> contentModelsDescriptor = ofNullable(descriptor.getComponents())
                    .map(ComponentSpecDescriptor::getContentModels)
                    .orElse(new ArrayList<>());

            List<Installable<ContentTemplateDescriptor>> installables = new LinkedList<>();
            for (String fileName : contentModelsDescriptor) {
                ContentTemplateDescriptor contentTemplateDescriptor = npr.readDescriptorFile(fileName,
                        ContentTemplateDescriptor.class);

                if (contentTemplateDescriptor.getContentShapePath() != null) {
                    String csPath = getRelativePath(fileName, contentTemplateDescriptor.getContentShapePath());
                    contentTemplateDescriptor.setContentShape(npr.readFileAsString(csPath));
                }

                installables.add(new ContentTemplateInstallable(engineService, contentTemplateDescriptor));
            }

            return installables;
        } catch (IOException e) {
            throw new EntandoComponentManagerException("Error reading bundle", e);
        }
    }

    @Override
    public List<Installable<ContentTemplateDescriptor>> process(List<EntandoBundleComponentJob> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == ComponentType.CONTENT_TEMPLATE)
                .map(c -> new ContentTemplateInstallable(engineService, this.buildDescriptorFromComponentJob(c)))
                .collect(Collectors.toList());
    }

    @Override
    public ContentTemplateDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJob component) {
        return ContentTemplateDescriptor.builder()
                .id(component.getName())
                .build();
    }
}
