package org.entando.kubernetes.model.bundle.processor;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.BundleReader;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.installable.PageInstallable;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.job.EntandoBundleComponentJob;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Processor to handle Pages
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Setter
public class PageProcessor implements ComponentProcessor<PageDescriptor> {

    private final EntandoCoreClient engineService;

    @Value("${entando.componentManager.processor.page.enabled}")
    private boolean isEnabled = false;

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.PAGE;
    }

    @Override
    public List<Installable<PageDescriptor>> process(BundleReader npr) {
        try {
            BundleDescriptor descriptor = npr.readBundleDescriptor();
            List<String> pageDescriptorList = ofNullable(descriptor.getComponents())
                    .map(ComponentSpecDescriptor::getPages)
                    .orElse(Collections.emptyList());

            List<Installable<PageDescriptor>> installables = new LinkedList<>();

            if (!isEnabled) {
                log.warn("Bundle page processing skipped due to configuration. Check application.properties and restart");
                return installables;
            }

            for (String fileName : pageDescriptorList) {
                PageDescriptor pageDescriptor = npr.readDescriptorFile(fileName, PageDescriptor.class);
                installables.add(new PageInstallable(engineService, pageDescriptor));
            }

            return installables;
        } catch (IOException e) {
            throw new EntandoComponentManagerException("Error reading bundle", e);
        }
    }

    @Override
    public List<Installable<PageDescriptor>> process(List<EntandoBundleComponentJob> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == ComponentType.PAGE)
                .map(c -> new PageInstallable(engineService, this.buildDescriptorFromComponentJob(c)))
                .collect(Collectors.toList());
    }

    @Override
    public PageDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJob component) {
        return PageDescriptor.builder()
                .code(component.getComponentId())
                .build();
    }
}
