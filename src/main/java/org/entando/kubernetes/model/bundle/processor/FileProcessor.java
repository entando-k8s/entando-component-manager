package org.entando.kubernetes.model.bundle.processor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.BundleProperty;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.DescriptorVersion;
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
            // bundle descriptor v1 and resource folder
            if (bundleReader.isBundleV1() && bundleReader.containsResourceFolder()) {

                collectResourceFilesV1(installables, bundleReader, conflictStrategy, installPlan);

                // otherwise if bundle descriptor v5 and widgets or resources folder
            } else if (bundleReader.readBundleDescriptor().isVersionEqualOrGreaterThan(DescriptorVersion.V5)
                    && (bundleReader.containsWidgetFolder() || bundleReader.containsResourceFolder())) {

                collectResourceFilesV5Onward(installables, bundleReader, conflictStrategy, installPlan);
                collectWidgetFiles(installables, bundleReader, conflictStrategy, installPlan);
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


    private void collectResourceFilesV1(List<Installable<FileDescriptor>> installables,
            BundleReader bundleReader, InstallAction conflictStrategy, InstallPlan installPlan) throws IOException {

        installables.addAll(processFilesBundleV1(bundleReader, conflictStrategy, installPlan));
    }

    private void collectWidgetFiles(List<Installable<FileDescriptor>> installables,
            BundleReader bundleReader, InstallAction conflictStrategy, InstallPlan installPlan) throws IOException {

        for (String folder : bundleReader.getWidgetsBaseFolders()) {
            final List<String> resourceOfType = bundleReader.getResourceOfType(folder, Files::isRegularFile);
            installables.addAll(
                    processFilesBundleV5Onward(bundleReader, resourceOfType,
                            BundleProperty.WIDGET_FOLDER_PATH, conflictStrategy, installPlan));
        }
    }


    private void collectResourceFilesV5Onward(List<Installable<FileDescriptor>> installables,
            BundleReader bundleReader, InstallAction conflictStrategy, InstallPlan installPlan) throws IOException {

        List<String> resourceFiles = bundleReader.getResourceFiles().stream().sorted().collect(Collectors.toList());
        installables.addAll(
                processFilesBundleV5Onward(bundleReader, resourceFiles,
                        BundleProperty.RESOURCES_FOLDER_PATH, conflictStrategy, installPlan));
    }

    /**
     * for each file in the resourceFile list, generate the relative descriptor and Installable and return them. this
     * method processes files of bundle V1
     *
     * @param bundleReader     the BundleReader to use to parse the descriptors
     * @param conflictStrategy the Conflict Strategy to apply
     * @param installPlan      the Install Plan to use against the received conflict strategy
     * @return the generated Installable list
     * @throws IOException in case of read error
     */
    private List<Installable<FileDescriptor>> processFilesBundleV1(BundleReader bundleReader,
            InstallAction conflictStrategy, InstallPlan installPlan)
            throws IOException {

        final List<Installable<FileDescriptor>> installables = new LinkedList<>();

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
            InstallAction action = extractInstallAction(filename, conflictStrategy, installPlan);
            installables.add(new FileInstallable(engineService, fileDescriptor, action));
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
    private List<Installable<FileDescriptor>> processFilesBundleV5Onward(BundleReader bundleReader,
            List<String> resourceFilelist,
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

        try {
            // bundle descriptor v1
            if (bundleReader.isBundleV1()) {

                return getReportableBundleV1(bundleReader, componentProcessor);
            } else {
                // bundle descriptor v5 onward
                return getReportableBundleV5Onward(bundleReader, componentProcessor);
            }

        } catch (IOException e) {
            throw new EntandoComponentManagerException(String.format("Error generating Reportable for %s type",
                    componentProcessor.getSupportedComponentType().getTypeName()), e);
        }
    }

    private Reportable getReportableBundleV1(BundleReader bundleReader, ComponentProcessor<?> componentProcessor)
            throws IOException {

        List<String> idList = new ArrayList<>();

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
    }

    private Reportable getReportableBundleV5Onward(BundleReader bundleReader, ComponentProcessor<?> componentProcessor)
            throws IOException {

        List<String> reportableIdList = new ArrayList<>();

        final String bundleId = BundleUtilities.removeProtocolAndGetBundleId(bundleReader.getBundleUrl());

        List<String> idList = bundleReader.getWidgetsFiles().stream().sorted().collect(Collectors.toList());
        for (String file : idList) {
            final String fileId = BundleUtilities.buildFullBundleResourcePath(bundleReader,
                    BundleProperty.WIDGET_FOLDER_PATH, file, bundleId);
            reportableIdList.add(fileId);
        }

        return new Reportable(componentProcessor.getSupportedComponentType(), reportableIdList,
                this.getReportableRemoteHandler());
    }
}
