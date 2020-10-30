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
import org.entando.kubernetes.controller.digitalexchange.job.model.AnalysisReport;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallActionsByComponentType;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallRequest.InstallAction;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.AssetDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ContentTemplateDescriptor;
import org.entando.kubernetes.model.bundle.installable.ContentTemplateInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.springframework.stereotype.Service;

/**
 * Processor to handle Bundles with CMS ContentTemplates.
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
    public List<Installable<ContentTemplateDescriptor>> process(BundleReader bundleReader) {
        return this.process(bundleReader, InstallAction.CREATE, new InstallActionsByComponentType(),
                new AnalysisReport());
    }

    @Override
    public List<Installable<ContentTemplateDescriptor>> process(BundleReader bundleReader, InstallAction conflictStrategy,
            InstallActionsByComponentType actions, AnalysisReport report) {
        try {
            BundleDescriptor descriptor = bundleReader.readBundleDescriptor();
            List<String> contentModelsDescriptor = ofNullable(descriptor.getComponents())
                    .map(ComponentSpecDescriptor::getContentTemplates)
                    .orElse(new ArrayList<>());

            List<Installable<ContentTemplateDescriptor>> installables = new LinkedList<>();
            for (String fileName : contentModelsDescriptor) {
                ContentTemplateDescriptor contentTemplateDescriptor = bundleReader.readDescriptorFile(fileName,
                        ContentTemplateDescriptor.class);

                if (contentTemplateDescriptor.getContentShapePath() != null) {
                    String csPath = getRelativePath(fileName, contentTemplateDescriptor.getContentShapePath());
                    contentTemplateDescriptor.setContentShape(bundleReader.readFileAsString(csPath));
                }

                installables.add(new ContentTemplateInstallable(engineService, contentTemplateDescriptor));
            }

            return installables;
        } catch (IOException e) {
            throw new EntandoComponentManagerException("Error reading bundle", e);
        }
    }

    @Override
    public List<Installable<ContentTemplateDescriptor>> process(List<EntandoBundleComponentJobEntity> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == ComponentType.CONTENT_TEMPLATE)
                .map(c -> new ContentTemplateInstallable(engineService, this.buildDescriptorFromComponentJob(c)))
                .collect(Collectors.toList());
    }

    @Override
    public ContentTemplateDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component) {
        return ContentTemplateDescriptor.builder()
                .id(component.getComponentId())
                .build();
    }
}
