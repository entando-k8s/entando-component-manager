package org.entando.kubernetes.model.bundle.processor;

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
import org.apache.commons.lang3.ObjectUtils;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.BundleProperty;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.DirectoryDescriptor;
import org.entando.kubernetes.model.bundle.installable.DirectoryInstallable;
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
public class DirectoryProcessor extends BaseComponentProcessor<DirectoryDescriptor>
        implements EntandoEngineReportableProcessor {

    private final EntandoCoreClient engineService;

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.DIRECTORY;
    }

    @Override
    public Class<DirectoryDescriptor> getDescriptorClass() {
        return DirectoryDescriptor.class;
    }

    @Override
    public Optional<Function<ComponentSpecDescriptor, List<String>>> getComponentSelectionFn() {
        return Optional.empty();
    }

    @Override
    public List<Installable<DirectoryDescriptor>> process(BundleReader bundleReader) {
        return this.process(bundleReader, InstallAction.CREATE, new InstallPlan());
    }

    @Override
    public List<Installable<DirectoryDescriptor>> process(BundleReader bundleReader, InstallAction conflictStrategy,
            InstallPlan installPlan) {
        final List<Installable<DirectoryDescriptor>> installables = new LinkedList<>();

        try {
            String bundleRootFolder = BundleUtilities.composeBundleResourceRootFolter(bundleReader);

            if (!ObjectUtils.isEmpty(bundleRootFolder)) {
                InstallAction rootDirectoryAction = extractInstallAction(bundleRootFolder, conflictStrategy,
                        installPlan);
                installables.add(new DirectoryInstallable(engineService,
                        new DirectoryDescriptor(bundleRootFolder, true), rootDirectoryAction));
            }
        } catch (IOException e) {
            throw makeMeaningfulException(e);
        }

        return installables;
    }

    @Override
    public List<Installable<DirectoryDescriptor>> process(List<EntandoBundleComponentJobEntity> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == getSupportedComponentType())
                .map(c -> new DirectoryInstallable(engineService, this.buildDescriptorFromComponentJob(c),
                        c.getAction()))
                .collect(Collectors.toList());
    }

    @Override
    public DirectoryDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component) {
        Path dirPath = Paths.get(component.getComponentId());
        boolean isRoot = dirPath.getParent().toString().equals(BundleUtilities.BUNDLES_FOLDER)
                || dirPath.getParent().equals(dirPath.getRoot());
        return new DirectoryDescriptor(component.getComponentId(), isRoot);
    }

    @Override
    public Reportable getReportable(BundleReader bundleReader, ComponentProcessor<?> componentProcessor) {

        List<String> idList;

        try {
            if (bundleReader.isBundleV1()) {
                idList = getReportableBundleV1(bundleReader);
            } else {
                idList = getReportableBundleV5(bundleReader, BundleProperty.WIDGET_FOLDER_PATH,
                        bundleReader.getWidgetsFolders());
            }

            return new Reportable(componentProcessor.getSupportedComponentType(), idList,
                    this.getReportableRemoteHandler());

        } catch (IOException e) {
            throw new EntandoComponentManagerException(String.format("Error generating Reportable for %s type",
                    componentProcessor.getSupportedComponentType().getTypeName()), e);
        }
    }

    private List<String> getReportableBundleV1(BundleReader bundleReader) throws IOException {

        List<String> idList = new ArrayList<>();

        final String resourceFolder = BundleUtilities.determineBundleResourceRootFolder(bundleReader);
        if (!resourceFolder.equals("/")) {
            idList.add(resourceFolder);
        }

        List<String> resourceFolders = bundleReader.getResourceFolders().stream().sorted()
                .collect(Collectors.toList());
        for (final String resourceDir : resourceFolders) {
            Path fileFolder = Paths.get(BundleProperty.RESOURCES_FOLDER_PATH.getValue())
                    .relativize(Paths.get(resourceDir));

            String folder = Paths.get(resourceFolder).resolve(fileFolder).toString();
            idList.add(folder);
        }

        idList.sort(null);
        return idList;
    }

    private List<String> getReportableBundleV5(BundleReader bundleReader, BundleProperty bundleProperty,
            List<String> folderList) throws IOException {

        List<String> list = new ArrayList<>();

        final String bundleRootFolder = BundleUtilities.composeBundleResourceRootFolter(bundleReader);
        if (!bundleRootFolder.equals("/")) {
            list.add(bundleRootFolder);
        }

        final String bundleId = BundleUtilities.removeProtocolAndGetBundleId(bundleReader.getBundleUrl());

        for (String resDir : folderList) {
            String path = BundleUtilities.buildFullBundleResourcePath(bundleReader, bundleProperty, resDir, bundleId);
            list.add(path);
        }
        list.sort(null);
        return list;
    }
}
