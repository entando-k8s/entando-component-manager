package org.entando.kubernetes.model.bundle.processor;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.model.bundle.NpmBundleReader;
import org.entando.kubernetes.model.bundle.descriptor.ComponentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJobComponent;
import org.entando.kubernetes.service.digitalexchange.entandocore.EntandoCoreService;
import org.entando.kubernetes.service.digitalexchange.job.DigitalExchangeUninstallService;
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

    private final EntandoCoreService engineService;

    @Override
    public List<Installable> process(final DigitalExchangeJob job, final NpmBundleReader npr,
                                               final ComponentDescriptor descriptor) throws IOException {

        final List<Installable> installables = new LinkedList<>();

        if (npr.containsResourceFolder()) {
            final String componentFolder = "/" + npr.getBundleId();
            installables.add(new DirectoryInstallable(componentFolder));

            final List<String> resourceFolders = npr.getResourceFolders();
            for (final String resourceFolder : resourceFolders) {
                installables.add(new DirectoryInstallable(componentFolder + "/" + resourceFolder));
            }

            final List<String> resourceFiles = npr.getResourceFiles();
            for (final String resourceFile : resourceFiles) {
                final FileDescriptor fileDescriptor = npr.readFileAsDescriptor(resourceFile);
                final String folder = StringUtils.isEmpty(fileDescriptor.getFolder())
                        ? componentFolder
                        : componentFolder + "/" + fileDescriptor.getFolder();
                fileDescriptor.setFolder(folder);
                installables.add(new AssetInstallable(fileDescriptor));
            }
        }

        return installables;
    }

    /**
     * This process will be hard coded in the {@link DigitalExchangeUninstallService}
     * @param componentType The component type being processed
     * @return always false
     */
    @Override
    public boolean shouldProcess(final ComponentType componentType) {
        return false;
    }

    @Override
    public void uninstall(final DigitalExchangeJobComponent component) {
        // Not necessary
    }

    public class AssetInstallable extends Installable<FileDescriptor> {

        private AssetInstallable(final FileDescriptor fileDescriptor) {
            super(fileDescriptor);
        }

        @Override
        public CompletableFuture install() {
            return CompletableFuture.runAsync(() -> {
                log.info("Uploading file {}", representation.getFilename());
                engineService.uploadFile(representation);
            });
        }

        @Override
        public ComponentType getComponentType() {
            return ComponentType.RESOURCE;
        }

        @Override
        public String getName() {
            return representation.getFolder() + "/" + representation.getFilename();
        }

    }

    public class DirectoryInstallable extends Installable<String> {

        private DirectoryInstallable(final String directory) {
            super(directory);
        }

        @Override
        public CompletableFuture install() {
            return CompletableFuture.runAsync(() -> {
                log.info("Creating directory {}", representation);
                engineService.createFolder(representation);
            });
        }

        @Override
        public ComponentType getComponentType() {
            return ComponentType.RESOURCE;
        }

        @Override
        public String getName() {
            return representation;
        }

    }
}
