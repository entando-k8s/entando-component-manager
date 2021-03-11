package org.entando.kubernetes.model.bundle.processor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.AnalysisReport;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallActionsByComponentType;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallRequest.InstallAction;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.BundleProperty;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;
import org.entando.kubernetes.model.bundle.installable.FileInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.reportable.EntandoEngineReportableProcessor;
import org.entando.kubernetes.model.bundle.reportable.Reportable;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.springframework.stereotype.Service;

/**
 * Processor to handle Static files to be stored by Entando. Commonly used for js, images and css.
 *
 * <p>This processor will also create the folders.
 *
 * @author Sergio Marcelino
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileProcessor extends BaseComponentProcessor<FileDescriptor> implements EntandoEngineReportableProcessor {

    private final EntandoCoreClient engineService;

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.RESOURCE;
    }

    @Override
    public Class<FileDescriptor> getDescriptorClass() {
        return FileDescriptor.class;
    }

    @Override
    public Optional<Function<ComponentSpecDescriptor, List<String>>> getComponentSelectionFn() {
        return Optional.empty();
    }

    @Override
    public List<Installable<FileDescriptor>> process(BundleReader bundleReader) {
        return this.process(bundleReader, InstallAction.CREATE, new InstallActionsByComponentType(),
                new AnalysisReport());
    }

    @Override
    public List<Installable<FileDescriptor>> process(BundleReader bundleReader, InstallAction conflictStrategy,
            InstallActionsByComponentType actions, AnalysisReport report) {
        final List<Installable<FileDescriptor>> installables = new LinkedList<>();

        try {
            if (bundleReader.containsResourceFolder()) {

                final String resourceFolder = BundleUtilities.determineBundleResourceRootFolder(bundleReader);

                List<String> resourceFiles = bundleReader.getResourceFiles().stream().sorted()
                        .collect(Collectors.toList());
                for (final String resourceFile : resourceFiles) {
                    final FileDescriptor fileDescriptor = bundleReader.getResourceFileAsDescriptor(resourceFile);

                    Path fileFolder = Paths.get(BundleProperty.RESOURCES_FOLDER_PATH.getValue())
                            .relativize(Paths.get(fileDescriptor.getFolder()));
                    String folder = Paths.get(resourceFolder).resolve(fileFolder).toString();
                    fileDescriptor.setFolder(folder);
                    String filename = folder + "/" + fileDescriptor.getFilename();
                    InstallAction action = extractInstallAction(filename, actions, conflictStrategy, report);
                    installables.add(new FileInstallable(engineService, fileDescriptor, action));
                }
            }
        } catch (IOException e) {
            throw makeMeaningfulException(e);
        }

        return installables;
    }

    @Override
    public List<Installable<FileDescriptor>> process(List<EntandoBundleComponentJobEntity> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == getSupportedComponentType())
                .map(c -> new FileInstallable(engineService, this.buildDescriptorFromComponentJob(c), c.getAction()))
                .collect(Collectors.toList());
    }

    @Override
    public FileDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component) {
        File file = FileUtils.getFile(component.getComponentId());
        return FileDescriptor.builder()
                .folder(file.getParent())
                .filename(file.getName())
                .build();
    }


    @Override
    public Reportable getReportable(BundleReader bundleReader, ComponentProcessor<?> componentProcessor) {

        List<String> idList = new ArrayList<>();

        try {
            if (bundleReader.containsResourceFolder()) {
                final String resourceFolder = BundleUtilities.determineBundleResourceRootFolder(bundleReader);

                List<String> resourceFiles = bundleReader.getResourceFiles().stream().sorted()
                        .collect(Collectors.toList());
                for (final String resourceFile : resourceFiles) {

                    Path fileFolder = Paths.get(BundleProperty.RESOURCES_FOLDER_PATH.getValue())
                            .relativize(Paths.get(resourceFile));

                    String file = Paths.get(resourceFolder).resolve(fileFolder).toString();
                    idList.add(file);
                }
            }

            return new Reportable(componentProcessor.getSupportedComponentType(), idList,
                    this.getReportableRemoteHandler());

        } catch (IOException e) {
            throw new EntandoComponentManagerException(String.format("Error generating Reportable for %s type",
                    componentProcessor.getSupportedComponentType().getTypeName()), e);
        }
    }
}
