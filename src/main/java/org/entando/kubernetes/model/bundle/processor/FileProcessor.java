package org.entando.kubernetes.model.bundle.processor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
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
        return this.process(bundleReader, InstallAction.CREATE, new InstallPlan());
    }

    @Override
    public List<Installable<FileDescriptor>> process(BundleReader bundleReader, InstallAction conflictStrategy,
            InstallPlan installPlan) {

        final List<Installable<FileDescriptor>> installables = new LinkedList<>();

        try {
            if (bundleReader.isBundleV1() && bundleReader.containsResourceFolder()) {
                installables.addAll(
                        processFiles(bundleReader, bundleReader.getResourceFiles(),
                                BundleProperty.RESOURCES_FOLDER_PATH, conflictStrategy, installPlan));
            } else if (bundleReader.containsWidgetFolder()) {
                processWidgetFiles(installables, bundleReader, conflictStrategy, installPlan);
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


    private List<Installable<FileDescriptor>> processWidgetFiles(List<Installable<FileDescriptor>> installables,
            BundleReader bundleReader, InstallAction conflictStrategy, InstallPlan installPlan) throws IOException {

        for (String folder : bundleReader.getWidgetsBaseFolders()) {
            final List<String> resourceOfType = bundleReader.getResourceOfType(folder, Files::isRegularFile);
            installables.addAll(
                    processFiles(bundleReader, resourceOfType,
                            BundleProperty.WIDGET_FOLDER_PATH, conflictStrategy, installPlan));
        }

        return installables;
    }

    /**
     * for each file in the resourceFilelist list, generate the relative descriptor and Installable and return them.
     *
     * @param bundleReader     the BundleReader to use to parse the descriptors
     * @param resourceFilelist the list of files to process
     * @param folderProp       the BundleProperty identifying files type
     * @param conflictStrategy the Conflict Strategy to apply
     * @param installPlan      the Install Plan to use against the received conflict strategy
     * @return the generated Installable list
     * @throws IOException in case of read error
     */
    private List<Installable<FileDescriptor>> processFiles(BundleReader bundleReader, List<String> resourceFilelist,
            BundleProperty folderProp, InstallAction conflictStrategy, InstallPlan installPlan)
            throws IOException {

        final List<Installable<FileDescriptor>> installables = new LinkedList<>();

        List<String> resourceFiles = resourceFilelist.stream().sorted().collect(Collectors.toList());
        final String bundleId = BundleUtilities.removeProtocolAndGetBundleId(bundleReader.getBundleUrl());

        for (final String resourceFile : resourceFiles) {
            final FileDescriptor fileDescriptor = bundleReader.getResourceFileAsDescriptor(resourceFile);

            String folder = BundleUtilities.buildFullBundleResourcePath(bundleReader, folderProp,
                    fileDescriptor.getFolder(), bundleId);
            fileDescriptor.setFolder(folder);

            String filename = Paths.get(folder, fileDescriptor.getFilename()).toString();
            InstallAction action = extractInstallAction(filename, conflictStrategy, installPlan);
            installables.add(new FileInstallable(engineService, fileDescriptor, action));
        }

        return installables;
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

        List<String> reportableIdList = new ArrayList<>();

        try {
            final String bundleId = BundleUtilities.removeProtocolAndGetBundleId(bundleReader.getBundleUrl());
            BundleProperty bundleProp = bundleReader.isBundleV1()
                    ? BundleProperty.RESOURCES_FOLDER_PATH
                    : BundleProperty.WIDGET_FOLDER_PATH;

            List<String> fileList = bundleReader.isBundleV1()
                    ? bundleReader.getResourceFiles()
                    : bundleReader.getWidgetsFiles();

            List<String> idList = fileList.stream().sorted().collect(Collectors.toList());
            for (String file : idList) {
                final String fileId = BundleUtilities.buildFullBundleResourcePath(bundleReader, bundleProp, file,
                        bundleId);
                reportableIdList.add(fileId);
            }

            return new Reportable(componentProcessor.getSupportedComponentType(), reportableIdList,
                    this.getReportableRemoteHandler());

        } catch (IOException e) {
            throw new EntandoComponentManagerException(String.format("Error generating Reportable for %s type",
                    componentProcessor.getSupportedComponentType().getTypeName()), e);
        }
    }
}
