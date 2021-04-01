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
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.contenttype.ContentTypeDescriptor;
import org.entando.kubernetes.model.bundle.installable.ContentTypeInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.reportable.EntandoCMSReportableProcessor;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.springframework.stereotype.Service;

/**
 * Processor to handle Bundles with CMS ContentTypes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentTypeProcessor extends BaseComponentProcessor<ContentTypeDescriptor>
        implements EntandoCMSReportableProcessor {

    private final EntandoCoreClient engineService;

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.CONTENT_TYPE;
    }

    @Override
    public Class<ContentTypeDescriptor> getDescriptorClass() {
        return ContentTypeDescriptor.class;
    }

    @Override
    public Optional<Function<ComponentSpecDescriptor, List<String>>> getComponentSelectionFn() {
        return Optional.of(ComponentSpecDescriptor::getContentTypes);
    }

    @Override
    public List<Installable<ContentTypeDescriptor>> process(BundleReader bundleReader) {
        return this.process(bundleReader, InstallAction.CREATE, new InstallPlan());
    }

    @Override
    public List<Installable<ContentTypeDescriptor>> process(BundleReader bundleReader, InstallAction conflictStrategy,
            InstallPlan installPlan) {

        List<Installable<ContentTypeDescriptor>> installables = new LinkedList<>();

        try {
            final List<String> descriptorList = getDescriptorList(bundleReader);

            for (String fileName : descriptorList) {
                ContentTypeDescriptor contentTypeDescriptor = bundleReader
                        .readDescriptorFile(fileName, ContentTypeDescriptor.class);
                InstallAction action = extractInstallAction(contentTypeDescriptor.getCode(), conflictStrategy,
                        installPlan);
                installables.add(new ContentTypeInstallable(engineService, contentTypeDescriptor, action));
            }

        } catch (IOException e) {
            throw makeMeaningfulException(e);
        }

        return installables;
    }

    @Override
    public List<Installable<ContentTypeDescriptor>> process(List<EntandoBundleComponentJobEntity> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == getSupportedComponentType())
                .map(c -> new ContentTypeInstallable(engineService, this.buildDescriptorFromComponentJob(c),
                        c.getAction()))
                .collect(Collectors.toList());
    }

    @Override
    public ContentTypeDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component) {
        return ContentTypeDescriptor.builder()
                .code(component.getComponentId())
                .build();
    }

}
