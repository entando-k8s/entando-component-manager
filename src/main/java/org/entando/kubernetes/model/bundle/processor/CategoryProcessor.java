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
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.CategoryDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.installable.CategoryInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.springframework.stereotype.Service;

/**
 * Processor to create Groups.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryProcessor implements ComponentProcessor<CategoryDescriptor> {

    private final EntandoCoreClient engineService;

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.CATEGORY;
    }

    @Override
    public List<Installable<CategoryDescriptor>> process(BundleReader npr) {
        try {
            BundleDescriptor descriptor = npr.readBundleDescriptor();

            List<String> categoryDescriptors = ofNullable(descriptor.getComponents())
                    .map(ComponentSpecDescriptor::getCategories)
                    .orElse(Collections.emptyList());

            List<Installable<CategoryDescriptor>> installables = new LinkedList<>();

            for (String fileName : categoryDescriptors) {
                CategoryDescriptor categoryDescriptor = npr.readDescriptorFile(fileName, CategoryDescriptor.class);
                installables.add(new CategoryInstallable(engineService, categoryDescriptor));
            }

            return installables;
        } catch (IOException e) {
            throw new EntandoComponentManagerException("Error reading bundle", e);
        }
    }

    @Override
    public List<Installable<CategoryDescriptor>> process(List<EntandoBundleComponentJobEntity> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == ComponentType.CATEGORY)
                .map(c -> new CategoryInstallable(engineService, this.buildDescriptorFromComponentJob(c)))
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component) {
        return CategoryDescriptor.builder()
                .code(component.getComponentId())
                .build();
    }

}
