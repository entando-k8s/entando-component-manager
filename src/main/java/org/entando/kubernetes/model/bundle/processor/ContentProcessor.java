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
import org.entando.kubernetes.controller.digitalexchange.job.model.AnalysisReport.Status;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallActionsByComponentType;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallRequest.InstallAction;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.content.ContentDescriptor;
import org.entando.kubernetes.model.bundle.installable.ContentInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.reportable.EntandoCMSReportableProcessor;
import org.entando.kubernetes.model.bundle.reportable.ReportableComponentProcessor;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.springframework.stereotype.Service;

/**
 * Processor to handle Bundles with CMS Content.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentProcessor implements ComponentProcessor<ContentDescriptor>, EntandoCMSReportableProcessor {

    private final EntandoCoreClient engineService;

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.CONTENT;
    }


    @Override
    public Class<ContentDescriptor> getDescriptorClass() {
        return ContentDescriptor.class;
    }

    @Override
    public Optional<Function<ComponentSpecDescriptor, List<String>>> getComponentSelectionFn() {
        return Optional.of(ComponentSpecDescriptor::getContents);
    }

    @Override
    public List<Installable<ContentDescriptor>> process(BundleReader bundleReader) {
        return this.process(bundleReader, InstallAction.CREATE, new InstallActionsByComponentType(),
                new AnalysisReport());
    }

    @Override
    public List<Installable<ContentDescriptor>> process(BundleReader bundleReader, InstallAction conflictStrategy,
            InstallActionsByComponentType actions, AnalysisReport report) {
        try {
            final List<String> descriptorList = getDescriptorList(bundleReader);

            List<Installable<ContentDescriptor>> installables = new LinkedList<>();

            for (String fileName : descriptorList) {
                ContentDescriptor contentDescriptor = bundleReader
                        .readDescriptorFile(fileName, ContentDescriptor.class);
                InstallAction action = extractInstallAction(contentDescriptor.getId(), actions, conflictStrategy, report);
                installables.add(new ContentInstallable(engineService, contentDescriptor, action));
            }

            return installables;
        } catch (IOException e) {
            throw new EntandoComponentManagerException("Error reading bundle", e);
        }
    }

    @Override
    public List<Installable<ContentDescriptor>> process(List<EntandoBundleComponentJobEntity> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == ComponentType.CONTENT)
                .map(c -> new ContentInstallable(engineService, this.buildDescriptorFromComponentJob(c)))
                .collect(Collectors.toList());
    }

    @Override
    public ContentDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component) {
        return ContentDescriptor.builder()
                .id(component.getComponentId())
                .build();
    }

    private InstallAction extractInstallAction(String contentId, InstallActionsByComponentType actions,
            InstallAction conflictStrategy, AnalysisReport report) {

        if (actions.getContents().containsKey(contentId)) {
            return actions.getContents().get(contentId);
        }

        if (isConflict(contentId, report)) {
            return conflictStrategy;
        }

        return InstallAction.CREATE;
    }

    private boolean isConflict(String contentId, AnalysisReport report) {
        return report.getContents().getConflict().contains(contentId);
    }
}