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
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallActionsByComponentType;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ContentTemplateDescriptor;
import org.entando.kubernetes.model.bundle.installable.ContentTemplateInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.reportable.EntandoCMSReportableProcessor;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.springframework.stereotype.Service;

/**
 * Processor to handle Bundles with CMS ContentTemplates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentTemplateProcessor extends BaseComponentProcessor<ContentTemplateDescriptor>
        implements EntandoCMSReportableProcessor {

    private final EntandoCoreClient engineService;

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.CONTENT_TEMPLATE;
    }

    @Override
    public Class<ContentTemplateDescriptor> getDescriptorClass() {
        return ContentTemplateDescriptor.class;
    }

    @Override
    public Optional<Function<ComponentSpecDescriptor, List<String>>> getComponentSelectionFn() {
        return Optional.of(ComponentSpecDescriptor::getContentTemplates);
    }

    @Override
    public List<Installable<ContentTemplateDescriptor>> process(BundleReader bundleReader) {
        return this.process(bundleReader, InstallAction.CREATE, new InstallActionsByComponentType(),
                new InstallPlan());
    }

    @Override
    public List<Installable<ContentTemplateDescriptor>> process(BundleReader bundleReader,
            InstallAction conflictStrategy,
            InstallActionsByComponentType actions, InstallPlan report) {

        List<Installable<ContentTemplateDescriptor>> installables = new LinkedList<>();

        try {
            final List<String> descriptorList = getDescriptorList(bundleReader);

            for (String fileName : descriptorList) {
                ContentTemplateDescriptor contentTemplateDescriptor = bundleReader.readDescriptorFile(fileName,
                        ContentTemplateDescriptor.class);

                if (contentTemplateDescriptor.getContentShapePath() != null) {
                    String csPath = getRelativePath(fileName, contentTemplateDescriptor.getContentShapePath());
                    contentTemplateDescriptor.setContentShape(bundleReader.readFileAsString(csPath));
                }

                InstallAction action = extractInstallAction(contentTemplateDescriptor.getId(), actions,
                        conflictStrategy, report);
                installables.add(new ContentTemplateInstallable(engineService, contentTemplateDescriptor, action));
            }

        } catch (IOException e) {
            throw makeMeaningfulException(e);
        }

        return installables;
    }

    @Override
    public List<Installable<ContentTemplateDescriptor>> process(List<EntandoBundleComponentJobEntity> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == getSupportedComponentType())
                .map(c -> new ContentTemplateInstallable(engineService, this.buildDescriptorFromComponentJob(c),
                        c.getAction()))
                .collect(Collectors.toList());
    }

    @Override
    public ContentTemplateDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component) {
        return ContentTemplateDescriptor.builder()
                .id(component.getComponentId())
                .build();
    }
}
