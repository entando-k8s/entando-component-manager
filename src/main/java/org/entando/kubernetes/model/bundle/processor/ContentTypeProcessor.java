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
import org.entando.kubernetes.model.bundle.descriptor.ContentTypeDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.Descriptor;
import org.entando.kubernetes.model.bundle.installable.ContentTypeInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleComponentJob;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleJob;
import org.springframework.stereotype.Service;

import javax.mail.internet.ContentType;

/**
 * Processor to handle Bundles with CMS ContentTypes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentTypeProcessor implements ComponentProcessor {

    private final EntandoCoreClient engineService;

    @Override
    public ComponentType getComponentType() {
        return ComponentType.CONTENT_TYPE;
    }

    @Override
    public List<Installable> process(EntandoBundleJob job, BundleReader npr) {
        try {
            ComponentDescriptor descriptor = npr.readBundleDescriptor();
            List<String> contentTypesDescriptor = ofNullable(descriptor.getComponents())
                    .map(ComponentSpecDescriptor::getContentTypes)
                    .orElse(new ArrayList<>());

            List<Installable> installables = new LinkedList<>();

            for (String fileName : contentTypesDescriptor) {
                ContentTypeDescriptor contentTypeDescriptor = npr
                        .readDescriptorFile(fileName, ContentTypeDescriptor.class);
                installables.add(new ContentTypeInstallable(engineService, contentTypeDescriptor));
            }

            return installables;
        } catch (IOException e) {
            throw new EntandoComponentManagerException("Error reading bundle", e);
        }
    }

    @Override
    public List<Installable> process(List<EntandoBundleComponentJob> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == ComponentType.CONTENT_TYPE)
                .map(c -> new ContentTypeInstallable(engineService, this.buildDescriptorFromComponentJob(c)))
                .collect(Collectors.toList());
    }

    @Override
    public ContentTypeDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJob component) {
        return ContentTypeDescriptor.builder()
                .code(component.getName())
                .build();
    }

}
