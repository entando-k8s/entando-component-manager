package org.entando.kubernetes.model.bundle.processor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.AnalysisReport;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallActionsByComponentType;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallRequest.InstallAction;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.BundleProperty;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.AssetDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.DirectoryDescriptor;
import org.entando.kubernetes.model.bundle.installable.DirectoryInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
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
public class DirectoryProcessor implements ComponentProcessor<DirectoryDescriptor> {

    private final EntandoCoreClient engineService;

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.DIRECTORY;
    }

    @Override
    public List<Installable<DirectoryDescriptor>> process(BundleReader bundleReader) {
        return this.process(bundleReader, InstallAction.CREATE, new InstallActionsByComponentType(),
                new AnalysisReport());
    }

    @Override
    public List<Installable<DirectoryDescriptor>> process(BundleReader bundleReader, InstallAction conflictStrategy,
            InstallActionsByComponentType actions, AnalysisReport report) {
        final List<Installable<DirectoryDescriptor>> installables = new LinkedList<>();

        try {
            if (bundleReader.containsResourceFolder()) {
                final String componentFolder = "/" + bundleReader.getBundleCode();
                installables.add(new DirectoryInstallable(engineService, new DirectoryDescriptor(componentFolder, true)));

                List<String> resourceFolders = bundleReader.getResourceFolders().stream().sorted().collect(Collectors.toList());
                for (final String resourceFolder : resourceFolders) {
                    Path fileFolder = Paths.get(BundleProperty.RESOURCES_FOLDER_PATH.getValue())
                            .relativize(Paths.get(resourceFolder));
                    String folder = Paths.get(componentFolder).resolve(fileFolder).toString();
                    installables.add(new DirectoryInstallable(engineService, new DirectoryDescriptor(folder, false)));
                }
            }
        } catch (IOException e) {
            throw new EntandoComponentManagerException("Error reading bundle", e);
        }

        return installables;
    }

    @Override
    public List<Installable<DirectoryDescriptor>> process(List<EntandoBundleComponentJobEntity> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == ComponentType.DIRECTORY)
                .map(c -> new DirectoryInstallable(engineService, this.buildDescriptorFromComponentJob(c)))
                .collect(Collectors.toList());
    }

    @Override
    public DirectoryDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component) {
        Path dirPath = Paths.get(component.getComponentId());
        boolean isRoot = false;
        if (dirPath.getParent().equals(dirPath.getRoot())) {
            isRoot = true;
        }
        return new DirectoryDescriptor(component.getComponentId(), isRoot);
    }
}
