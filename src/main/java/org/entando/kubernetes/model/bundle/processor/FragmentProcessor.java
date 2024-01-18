package org.entando.kubernetes.model.bundle.processor;

import static org.entando.kubernetes.service.digitalexchange.BundleUtilities.removeProtocolAndGetBundleId;

import java.io.IOException;
import java.util.ArrayList;
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
import org.entando.kubernetes.model.bundle.descriptor.FragmentDescriptor;
import org.entando.kubernetes.model.bundle.installable.FragmentInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.reportable.EntandoEngineReportableProcessor;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FragmentProcessor extends BaseComponentProcessor<FragmentDescriptor> implements
        EntandoEngineReportableProcessor {

    private final EntandoCoreClient engineService;

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.FRAGMENT;
    }

    @Override
    public Class<FragmentDescriptor> getDescriptorClass() {
        return FragmentDescriptor.class;
    }

    @Override
    public Optional<Function<ComponentSpecDescriptor, List<String>>> getComponentSelectionFn() {
        return Optional.of(ComponentSpecDescriptor::getFragments);
    }

    @Override
    public List<Installable<FragmentDescriptor>> process(BundleReader bundleReader) {
        return this.process(bundleReader, InstallAction.CREATE, new InstallPlan());
    }

    @Override
    public List<Installable<FragmentDescriptor>> process(BundleReader bundleReader, InstallAction conflictStrategy,
            InstallPlan installPlan) {

        List<Installable<FragmentDescriptor>> installableList = new ArrayList<>();

        try {
            final List<String> descriptorList = getDescriptorList(bundleReader);
            final String bundleId = removeProtocolAndGetBundleId(bundleReader.getBundleUrl());

            for (String fileName : descriptorList) {
                FragmentDescriptor frDesc = bundleReader.readDescriptorFile(fileName, FragmentDescriptor.class);
                if (frDesc.getGuiCodePath() != null) {
                    String gcp = getRelativePath(fileName, frDesc.getGuiCodePath());
                    frDesc.setGuiCode(bundleReader.readFileAsString(gcp));
                }
                replaceBundleIdPlaceholder(bundleId, frDesc);
                InstallAction action = extractInstallAction(frDesc.getCode(), conflictStrategy, installPlan);
                installableList.add(new FragmentInstallable(engineService, frDesc, action));
            }
        } catch (IOException e) {
            throw makeMeaningfulException(e);
        }

        return installableList;
    }

    @Override
    public List<Installable<FragmentDescriptor>> process(List<EntandoBundleComponentJobEntity> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == getSupportedComponentType())
                .map(c -> new FragmentInstallable(engineService, this.buildDescriptorFromComponentJob(c),
                        c.getAction()))
                .collect(Collectors.toList());
    }

    @Override
    public FragmentDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component) {
        return FragmentDescriptor.builder()
                .code(component.getComponentId())
                .build();
    }

    private void replaceBundleIdPlaceholder(String bundleId, FragmentDescriptor descriptor) {
        ProcessorHelper.applyBundleIdPlaceholderReplacement(bundleId, descriptor::getCode, descriptor::setCode);
        ProcessorHelper.applyBundleIdPlaceholderReplacement(bundleId, descriptor::getGuiCode, descriptor::setGuiCode);
        ProcessorHelper.applyBundleIdPlaceholderReplacement(bundleId, descriptor::getGuiCodePath, descriptor::setGuiCodePath);
    }

}
