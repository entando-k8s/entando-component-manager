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
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.GroupDescriptor;
import org.entando.kubernetes.model.bundle.installable.GroupInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.springframework.stereotype.Service;

/**
 * Processor to create Groups.
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupProcessor implements ComponentProcessor<GroupDescriptor> {

    private final EntandoCoreClient engineService;

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.GROUP;
    }

    @Override
    public List<Installable<GroupDescriptor>> process(BundleReader npr) {
        try {
            BundleDescriptor descriptor = npr.readBundleDescriptor();

            List<String> groupDescriptors = ofNullable(descriptor.getComponents())
                    .map(ComponentSpecDescriptor::getGroups)
                    .orElse(Collections.emptyList());

            List<Installable<GroupDescriptor>> installables = new LinkedList<>();

            for (String fileName : groupDescriptors) {
                GroupDescriptor groupDescriptor = npr.readDescriptorFile(fileName, GroupDescriptor.class);
                installables.add(new GroupInstallable(engineService, groupDescriptor));
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
                .map(c -> new GroupInstallable(engineService, this.buildDescriptorFromComponentJob(c)))
                .collect(Collectors.toList());
    }

    @Override
    public GroupDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component) {
        return GroupDescriptor.builder()
                .code(component.getComponentId())
                .build();
    }

}
