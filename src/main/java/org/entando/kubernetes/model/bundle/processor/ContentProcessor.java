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
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.content.ContentDescriptor;
import org.entando.kubernetes.model.bundle.installable.ContentInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.springframework.stereotype.Service;

/**
 * Processor to handle Bundles with CMS Content.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentProcessor implements ComponentProcessor<ContentDescriptor> {

    private final EntandoCoreClient engineService;

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.CONTENT;
    }

    @Override
    public List<Installable<ContentDescriptor>> process(BundleReader npr) {
        try {
            BundleDescriptor descriptor = npr.readBundleDescriptor();
            List<String> contentDescriptorList = ofNullable(descriptor.getComponents())
                    .map(ComponentSpecDescriptor::getContents)
                    .orElse(new ArrayList<>());

            List<Installable<ContentDescriptor>> installables = new LinkedList<>();

            for (String fileName : contentDescriptorList) {
                ContentDescriptor contentDescriptor = npr
                        .readDescriptorFile(fileName, ContentDescriptor.class);
                installables.add(new ContentInstallable(engineService, contentDescriptor));
            }

            return installables;
        } catch (IOException e) {
            throw new EntandoComponentManagerException("Error reading bundle", e);
        }
    }

    @Override
    public List<Installable<ContentDescriptor>> process(List<EntandoBundleComponentJobEntity> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == ComponentType.CONTENT)
                .map(c -> new ContentInstallable(engineService, this.buildDescriptorFromComponentJob(c)))
                .collect(Collectors.toList());
    }

    @Override
    public ContentDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component) {
        return ContentDescriptor.builder()
                .id(component.getComponentId())
                .build();
    }
}