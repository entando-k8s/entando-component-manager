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
import org.entando.kubernetes.controller.digitalexchange.job.model.AnalysisReport;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallActionsByComponentType;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallRequest.InstallAction;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.GroupDescriptor;
import org.entando.kubernetes.model.bundle.installable.GroupInstallable;
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
public class GroupProcessor extends BaseComponentProcessor<GroupDescriptor> implements
        EntandoEngineReportableProcessor {

    private final EntandoCoreClient engineService;

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.GROUP;
    }

    @Override
    public Class<GroupDescriptor> getDescriptorClass() {
        return GroupDescriptor.class;
    }

    @Override
    public Optional<Function<ComponentSpecDescriptor, List<String>>> getComponentSelectionFn() {
        return Optional.of(ComponentSpecDescriptor::getGroups);
    }

    @Override
    public boolean doesComponentDscriptorContainMoreThanOneSingleEntity() {
        return true;
    }

    @Override
    public List<Installable<GroupDescriptor>> process(BundleReader bundleReader) {
        return this.process(bundleReader, InstallAction.CREATE, new InstallActionsByComponentType(),
                new AnalysisReport());
    }

    @Override
    public List<Installable<GroupDescriptor>> process(BundleReader bundleReader, InstallAction conflictStrategy,
            InstallActionsByComponentType actions, AnalysisReport report) {
        try {
            final List<String> descriptorList = getDescriptorList(bundleReader);

            List<Installable<GroupDescriptor>> installables = new LinkedList<>();

            for (String fileName : descriptorList) {
                List<GroupDescriptor> groupDescriptorList = bundleReader
                        .readListOfDescriptorFile(fileName, GroupDescriptor.class);
                for (GroupDescriptor gd : groupDescriptorList) {
                    InstallAction action = extractInstallAction(gd.getCode(), actions, conflictStrategy, report);
                    installables.add(new GroupInstallable(engineService, gd, action));
                }
            }

            return installables;
        } catch (IOException e) {
            throw new EntandoComponentManagerException("Error reading bundle", e);
        }
    }

    @Override
    public List<Installable<GroupDescriptor>> process(List<EntandoBundleComponentJobEntity> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == ComponentType.GROUP)
                .map(c -> new GroupInstallable(engineService, this.buildDescriptorFromComponentJob(c), c.getAction()))
                .collect(Collectors.toList());
    }

    @Override
    public GroupDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component) {
        return GroupDescriptor.builder()
                .code(component.getComponentId())
                .build();
    }

}
