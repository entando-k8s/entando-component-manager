package org.entando.kubernetes.model.bundle.processor;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
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
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.installable.PageInitializationInstallable;
import org.entando.kubernetes.model.bundle.installable.PagePopulationInstallable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.reportable.EntandoEngineReportableProcessor;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.springframework.stereotype.Service;

/**
 * Processor to handle Pages.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PageProcessor extends BaseComponentProcessor<PageDescriptor> implements EntandoEngineReportableProcessor {

    private final EntandoCoreClient engineService;

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.PAGE;
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
                new AnalysisReport());
    }

    @Override
    public List<Installable<PageDescriptor>> process(BundleReader bundleReader, InstallAction conflictStrategy,
            InstallActionsByComponentType actions, AnalysisReport report) {
        try {
            final List<String> descriptorList = getDescriptorList(bundleReader);

            List<Installable<PageDescriptor>> installables = new LinkedList<>();

            for (String fileName : descriptorList) {
                PageDescriptor pageDescriptor = bundleReader.readDescriptorFile(fileName, PageDescriptor.class);
                InstallAction action = extractInstallAction(pageDescriptor.getCode(), actions, conflictStrategy,
                        report);

                // add an installable for the page initialization
                installables.add(createPageInitializationInstallable(pageDescriptor, action));
                // add an installable for the page population
                installables.add(new PagePopulationInstallable(engineService, pageDescriptor, action));
            }

            return installables;
        } catch (IOException e) {
            throw new EntandoComponentManagerException("Error reading bundle", e);
        }
    }


    private Installable<PageDescriptor> createPageInitializationInstallable(PageDescriptor pageDescriptor,
            InstallAction action) {

        // create a descriptor containing only a subset of infos about the page
        PageDescriptor initDescriptor = PageDescriptor.builder()
                .code(pageDescriptor.getCode())
                .titles(pageDescriptor.getTitles().entrySet().stream()
                        // add the text " - STUB" to each title in order to ease the finding process
                        .map(entry -> new SimpleEntry<>(entry.getKey(),
                                entry.getValue() + " - STUB"))
                        .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue)))
                .build();

        return new PagePopulationInstallable(engineService, initDescriptor, action);
    }


    @Override
    public List<Installable<PageDescriptor>> process(List<EntandoBundleComponentJobEntity> components) {
        return components.stream()
                // we can manage pages in one single flow during uninstall?
                .filter(c -> c.getComponentType() == ComponentType.PAGE)
                .map(c -> new PageInitializationInstallable(engineService, this.buildDescriptorFromComponentJob(c),
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
