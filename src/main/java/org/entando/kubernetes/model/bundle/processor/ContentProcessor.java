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
import org.entando.kubernetes.model.bundle.descriptor.content.ContentDescriptor;
import org.entando.kubernetes.model.bundle.installable.ContentInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.reportable.EntandoCMSReportableProcessor;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.springframework.stereotype.Service;

/**
 * Processor to handle Bundles with CMS Content.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentProcessor extends BaseComponentProcessor<ContentDescriptor>
        implements EntandoCMSReportableProcessor {

    private final EntandoCoreClient engineService;

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.CONTENT;
    }


    @Override
    public Class<ContentDescriptor> getDescriptorClass() {
        return ContentDescriptor.class;
    }

    @Override
    public Optional<Function<ComponentSpecDescriptor, List<String>>> getComponentSelectionFn() {
        return Optional.of(ComponentSpecDescriptor::getContents);
    }

    @Override
    public List<Installable<ContentDescriptor>> process(BundleReader bundleReader) {
        return this.process(bundleReader, InstallAction.CREATE, new InstallPlan());
    }

    @Override
    public List<Installable<ContentDescriptor>> process(BundleReader bundleReader, InstallAction conflictStrategy,
            InstallPlan installPlan) {

        List<Installable<ContentDescriptor>> installables = new LinkedList<>();

        try {
            final List<String> descriptorList = getDescriptorList(bundleReader);

            for (String fileName : descriptorList) {
                ContentDescriptor contentDescriptor = bundleReader
                        .readDescriptorFile(fileName, ContentDescriptor.class);
                InstallAction action = extractInstallAction(contentDescriptor.getId(), conflictStrategy, installPlan);
                installables.add(new ContentInstallable(engineService, contentDescriptor, action));
            }

        } catch (IOException e) {
            throw makeMeaningfulException(e);
        }

        return installables;
    }

    @Override
    public List<Installable<ContentDescriptor>> process(List<EntandoBundleComponentJobEntity> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == getSupportedComponentType())
                .map(c -> new ContentInstallable(engineService, this.buildDescriptorFromComponentJob(c), c.getAction()))
                .collect(Collectors.toList());
    }

    @Override
    public ContentDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component) {
        return ContentDescriptor.builder()
                .id(component.getComponentId())
                .build();
    }
}
