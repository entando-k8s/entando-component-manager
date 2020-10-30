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
import org.entando.kubernetes.controller.digitalexchange.job.model.AnalysisReport;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallActionsByComponentType;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallRequest.InstallAction;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.AssetDescriptor;
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
    public List<Installable<CategoryDescriptor>> process(BundleReader bundleReader) {
        return this.process(bundleReader, InstallAction.CREATE, new InstallActionsByComponentType(),
                new AnalysisReport());
    }

    @Override
    public List<Installable<CategoryDescriptor>> process(BundleReader bundleReader, InstallAction conflictStrategy,
            InstallActionsByComponentType actions, AnalysisReport report) {
        try {
            BundleDescriptor descriptor = bundleReader.readBundleDescriptor();

            List<String> categoryDescriptorFiles = ofNullable(descriptor.getComponents())
                    .map(ComponentSpecDescriptor::getCategories)
                    .orElse(Collections.emptyList());

            List<Installable<CategoryDescriptor>> installables = new LinkedList<>();

            for (String fileName : categoryDescriptorFiles) {
                List<CategoryDescriptor> categoryDescriptorList = bundleReader.readListOfDescriptorFile(fileName, CategoryDescriptor.class);
                for (CategoryDescriptor cd: categoryDescriptorList) {
                    installables.add(new CategoryInstallable(engineService, cd));
                }
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
