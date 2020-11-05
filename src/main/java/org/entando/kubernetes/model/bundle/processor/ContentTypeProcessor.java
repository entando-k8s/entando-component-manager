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
import org.entando.kubernetes.model.bundle.descriptor.contenttype.ContentTypeDescriptor;
import org.entando.kubernetes.model.bundle.installable.ContentTypeInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.reportable.EntandoCMSReportableProcessor;
import org.entando.kubernetes.model.bundle.reportable.ReportableComponentProcessor;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.springframework.stereotype.Service;

/**
 * Processor to handle Bundles with CMS ContentTypes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentTypeProcessor extends BaseComponentProcessor<ContentTypeDescriptor>
        implements EntandoCMSReportableProcessor {

    private final EntandoCoreClient engineService;

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.CONTENT_TYPE;
    }

    @Override
    public Class<ContentTypeDescriptor> getDescriptorClass() {
        return ContentTypeDescriptor.class;
    }

    @Override
    public Optional<Function<ComponentSpecDescriptor, List<String>>> getComponentSelectionFn() {
        return Optional.of(ComponentSpecDescriptor::getContentTypes);
    }

    @Override
    public List<Installable<ContentTypeDescriptor>> process(BundleReader bundleReader) {
        return this.process(bundleReader, InstallAction.CREATE, new InstallActionsByComponentType(),
                new AnalysisReport());
    }

    @Override
    public List<Installable<ContentTypeDescriptor>> process(BundleReader bundleReader, InstallAction conflictStrategy,
            InstallActionsByComponentType actions, AnalysisReport report) {
        try {
            final List<String> descriptorList = getDescriptorList(bundleReader);

            List<Installable<ContentTypeDescriptor>> installables = new LinkedList<>();

            for (String fileName : descriptorList) {
                ContentTypeDescriptor contentTypeDescriptor = bundleReader
                        .readDescriptorFile(fileName, ContentTypeDescriptor.class);
                InstallAction action = extractInstallAction(contentTypeDescriptor.getCode(), actions, conflictStrategy, report);
                installables.add(new ContentTypeInstallable(engineService, contentTypeDescriptor, action));
            }

            return installables;
        } catch (IOException e) {
            throw new EntandoComponentManagerException("Error reading bundle", e);
        }
    }

    @Override
    public List<Installable<ContentTypeDescriptor>> process(List<EntandoBundleComponentJobEntity> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == ComponentType.CONTENT_TYPE)
                .map(c -> new ContentTypeInstallable(engineService, this.buildDescriptorFromComponentJob(c), c.getAction()))
                .collect(Collectors.toList());
    }

    @Override
    public ContentTypeDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component) {
        return ContentTypeDescriptor.builder()
                .code(component.getComponentId())
                .build();
    }

}
