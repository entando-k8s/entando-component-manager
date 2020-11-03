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
import org.entando.kubernetes.model.bundle.descriptor.LanguageDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.installable.LanguageInstallable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.reportable.EntandoEngineReportableProcessor;
import org.entando.kubernetes.model.bundle.reportable.ReportableComponentProcessor;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Processor to create Languages.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LanguageProcessor implements ComponentProcessor<LanguageDescriptor>, EntandoEngineReportableProcessor {

    private final EntandoCoreClient engineService;

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.LANGUAGE;
    }

    @Override
    public Class<LanguageDescriptor> getDescriptorClass() {
        return LanguageDescriptor.class;
    }

    @Override
    public Optional<Function<ComponentSpecDescriptor, List<String>>> getComponentSelectionFn() {
        return Optional.of(ComponentSpecDescriptor::getLanguages);
    }

    @Override
    public List<Installable<LanguageDescriptor>> process(BundleReader bundleReader) {
        return this.process(bundleReader, InstallAction.CREATE, new InstallActionsByComponentType(),
                new AnalysisReport());
    }

    @Override
    public List<Installable<LanguageDescriptor>> process(BundleReader bundleReader, InstallAction conflictStrategy,
            InstallActionsByComponentType actions, AnalysisReport report) {
        try {
            final List<String> descriptorList = getDescriptorList(bundleReader);

            final List<Installable<LanguageDescriptor>> installables = new LinkedList<>();

            for (String ldf : descriptorList) {
                List<LanguageDescriptor> languageDescriptorList = bundleReader
                        .readListOfDescriptorFile(ldf, LanguageDescriptor.class);
                for (LanguageDescriptor ld : languageDescriptorList) {
                    if (StringUtils.isEmpty(ld.getCode())) {
                        throw new EntandoComponentManagerException(
                                "The bundle has a language with empty code. A code is mandatory for each language.");
                    }
                    installables.add(new LanguageInstallable(engineService, ld));
                }
            }

            return installables;
        } catch (IOException e) {
            throw new EntandoComponentManagerException("Error reading bundle", e);
        }
    }

    @Override
    public List<Installable<LanguageDescriptor>> process(List<EntandoBundleComponentJobEntity> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == ComponentType.LANGUAGE)
                .map(c -> new LanguageInstallable(engineService, this.buildDescriptorFromComponentJob(c)))
                .collect(Collectors.toList());
    }

    @Override
    public LanguageDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component) {
        return LanguageDescriptor.builder()
                .code(component.getComponentId())
                .build();
    }

}
