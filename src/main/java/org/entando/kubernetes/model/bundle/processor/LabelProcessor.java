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
import org.entando.kubernetes.model.bundle.descriptor.LabelDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.installable.LabelInstallable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.reportable.EntandoEngineReportableProcessor;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.springframework.stereotype.Service;

/**
 * Processor to create Labels.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LabelProcessor extends BaseComponentProcessor<LabelDescriptor> implements
        EntandoEngineReportableProcessor {

    private final EntandoCoreClient engineService;

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.LABEL;
    }

    @Override
    public Class<LabelDescriptor> getDescriptorClass() {
        return LabelDescriptor.class;
    }

    @Override
    public Optional<Function<ComponentSpecDescriptor, List<String>>> getComponentSelectionFn() {
        return Optional.of(ComponentSpecDescriptor::getLabels);
    }

    @Override
    public boolean doesComponentDscriptorContainMoreThanOneSingleEntity() {
        return true;
    }

    @Override
    public List<Installable<LabelDescriptor>> process(BundleReader bundleReader) {
        return this.process(bundleReader, InstallAction.CREATE, new InstallActionsByComponentType(),
                new InstallPlan());
    }

    @Override
    public List<Installable<LabelDescriptor>> process(BundleReader bundleReader, InstallAction conflictStrategy,
            InstallActionsByComponentType actions, InstallPlan report) {

        final List<Installable<LabelDescriptor>> installables = new LinkedList<>();

        try {
            final List<String> descriptorList = getDescriptorList(bundleReader);

            for (String ldf : descriptorList) {
                List<LabelDescriptor> labelDescriptorList = bundleReader
                        .readListOfDescriptorFile(ldf, LabelDescriptor.class);
                for (LabelDescriptor ld : labelDescriptorList) {
                    InstallAction action = extractInstallAction(ld.getKey(), actions, conflictStrategy, report);
                    installables.add(new LabelInstallable(engineService, ld, action));
                }
            }

        } catch (IOException e) {
            throw makeMeaningfulException(e);
        }

        return installables;
    }

    @Override
    public List<Installable<LabelDescriptor>> process(List<EntandoBundleComponentJobEntity> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == getSupportedComponentType())
                .map(c -> new LabelInstallable(engineService, this.buildDescriptorFromComponentJob(c), c.getAction()))
                .collect(Collectors.toList());
    }

    @Override
    public LabelDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component) {
        return LabelDescriptor.builder()
                .key(component.getComponentId())
                .build();
    }

}
