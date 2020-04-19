package org.entando.kubernetes.model.bundle.processor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.bundle.BundleProperty;
import org.entando.kubernetes.model.bundle.BundleReader;
import org.entando.kubernetes.model.bundle.descriptor.ComponentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;
import org.entando.kubernetes.model.bundle.installable.AssetInstallable;
import org.entando.kubernetes.model.bundle.installable.DirectoryInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleJob;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleComponentJob;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.service.digitalexchange.job.EntandoBundleUninstallService;
import org.springframework.stereotype.Service;

/**
 * Processor to handle Static files to be stored by Entando.
 * Commonly used for js, images and css.
 *
 * This processor will also create the folders.
 *
 * @author Sergio Marcelino
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetProcessor implements ComponentProcessor {

    private final EntandoCoreClient engineService;

    @Override
    public List<Installable> process(final EntandoBundleJob job, final BundleReader npr,
                                               final ComponentDescriptor descriptor) throws IOException {

        final List<Installable> installables = new LinkedList<>();

        if (npr.containsResourceFolder()) {
            final String componentFolder = "/" + job.getComponentId();
            installables.add(new DirectoryInstallable(engineService, componentFolder));

            List<String> resourceFolders = npr.getResourceFolders().stream().sorted().collect(Collectors.toList());
            for (final String resourceFolder : resourceFolders) {
                Path fileFolder = Paths.get(BundleProperty.RESOURCES_FOLDER_PATH.getValue()).relativize(Paths.get(resourceFolder));
                String folder = Paths.get(componentFolder).resolve(fileFolder).toString();
                installables.add(new DirectoryInstallable(engineService, folder));
            }

            List<String> resourceFiles = npr.getResourceFiles().stream().sorted().collect(Collectors.toList());
            for (final String resourceFile : resourceFiles) {
                final FileDescriptor fileDescriptor = npr.getResourceFileAsDescriptor(resourceFile);

                Path fileFolder = Paths.get(BundleProperty.RESOURCES_FOLDER_PATH.getValue()).relativize(Paths.get(fileDescriptor.getFolder()));
                String folder = Paths.get(componentFolder).resolve(fileFolder).toString();
                fileDescriptor.setFolder(folder);
                installables.add(new AssetInstallable(engineService, fileDescriptor));
            }
        }

        return installables;
    }

    /**
     * This process will be hard coded in the {@link EntandoBundleUninstallService}
     * @param componentType The component type being processed
     * @return always false
     */
    @Override
    public boolean shouldProcess(final ComponentType componentType) {
        return false;
    }

    @Override
    public void uninstall(final EntandoBundleComponentJob component) {
        // Not necessary
    }

}
