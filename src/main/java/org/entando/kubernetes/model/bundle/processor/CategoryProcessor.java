package org.entando.kubernetes.model.bundle.processor;

import static org.entando.kubernetes.service.digitalexchange.BundleUtilities.removeProtocolAndGetBundleId;

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
import org.entando.kubernetes.model.bundle.descriptor.CategoryDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.installable.CategoryInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.reportable.EntandoEngineReportableProcessor;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.springframework.stereotype.Service;

/**
 * Processor to create Groups.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryProcessor extends BaseComponentProcessor<CategoryDescriptor>
        implements EntandoEngineReportableProcessor {

    private final EntandoCoreClient engineService;

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.CATEGORY;
    }

    @Override
    public Class<CategoryDescriptor> getDescriptorClass() {
        return CategoryDescriptor.class;
    }

    @Override
    public Optional<Function<ComponentSpecDescriptor, List<String>>> getComponentSelectionFn() {
        return Optional.of(ComponentSpecDescriptor::getCategories);
    }

    @Override
    public boolean doesComponentDscriptorContainMoreThanOneSingleEntity() {
        return true;
    }

    @Override
    public List<Installable<CategoryDescriptor>> process(BundleReader bundleReader) {
        return this.process(bundleReader, InstallAction.CREATE, new InstallPlan());
    }

    @Override
    public List<Installable<CategoryDescriptor>> process(BundleReader bundleReader, InstallAction conflictStrategy,
            InstallPlan installPlan) {

        List<Installable<CategoryDescriptor>> installables = new LinkedList<>();

        try {
            final List<String> descriptorList = getDescriptorList(bundleReader);
            final String bundleId = removeProtocolAndGetBundleId(bundleReader.getBundleUrl());

            for (String fileName : descriptorList) {
                List<CategoryDescriptor> categoryDescriptorList = bundleReader
                        .readListOfDescriptorFile(fileName, CategoryDescriptor.class);
                for (CategoryDescriptor cd : categoryDescriptorList) {
                    InstallAction action = extractInstallAction(cd.getCode(), conflictStrategy, installPlan);
                    replaceBundleIdPlaceholder(bundleId, cd);
                    installables.add(new CategoryInstallable(engineService, cd, action));
                }
            }

        } catch (IOException e) {
            throw makeMeaningfulException(e);
        }

        return installables;
    }

    @Override
    public List<Installable<CategoryDescriptor>> process(List<EntandoBundleComponentJobEntity> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == getSupportedComponentType())
                .map(c -> new CategoryInstallable(engineService, this.buildDescriptorFromComponentJob(c),
                        c.getAction()))
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component) {
        return CategoryDescriptor.builder()
                .code(component.getComponentId())
                .build();
    }

    private void replaceBundleIdPlaceholder(String bundleId, CategoryDescriptor descriptor) {
        ProcessorHelper.applyBundleIdPlaceholderReplacement(bundleId, descriptor::getCode, descriptor::setCode);
        ProcessorHelper.applyBundleIdPlaceholderReplacement(bundleId, descriptor::getParentCode, descriptor::setParentCode);
    }
}
