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
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.installable.PageConfigurationInstallable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.springframework.stereotype.Service;

/**
 * Processor to handle Pages.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PageConfigurationProcessor extends BaseComponentProcessor<PageDescriptor> {

    private final EntandoCoreClient engineService;

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.PAGE_CONFIGURATION;
    }

    @Override
    public Class<PageDescriptor> getDescriptorClass() {
        return PageDescriptor.class;
    }

    @Override
    public Optional<Function<ComponentSpecDescriptor, List<String>>> getComponentSelectionFn() {
        return Optional.of(ComponentSpecDescriptor::getPages);
    }

    @Override
    public List<Installable<PageDescriptor>> process(BundleReader bundleReader) {
        return this.process(bundleReader, InstallAction.CREATE, new InstallActionsByComponentType(),
                new InstallPlan());
    }

    @Override
    public List<Installable<PageDescriptor>> process(BundleReader bundleReader, InstallAction conflictStrategy,
            InstallActionsByComponentType actions, InstallPlan report) {

        List<Installable<PageDescriptor>> installables = new LinkedList<>();

        try {
            final List<String> descriptorList = getDescriptorList(bundleReader);

            for (String fileName : descriptorList) {
                PageDescriptor pageDescriptor = bundleReader
                        .readDescriptorFile(fileName, this.getDescriptorClass());
                InstallAction action = extractInstallAction(pageDescriptor.getCode(), actions, conflictStrategy,
                        report);

                installables.add(new PageConfigurationInstallable(engineService, pageDescriptor, action));
            }

        } catch (IOException e) {
            throw makeMeaningfulException(e);
        }

        return installables;
    }

    @Override
    public List<Installable<PageDescriptor>> process(List<EntandoBundleComponentJobEntity> components) {
        return components.stream()
                // we can manage pages in one single flow during uninstall?
                .filter(c -> c.getComponentType() == getSupportedComponentType())
                .map(c -> new PageConfigurationInstallable(engineService, this.buildDescriptorFromComponentJob(c),
                        c.getAction()))
                .collect(Collectors.toList());
    }

    @Override
    public PageDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component) {
        return PageDescriptor.builder()
                .code(component.getComponentId())
                .build();
    }
}
