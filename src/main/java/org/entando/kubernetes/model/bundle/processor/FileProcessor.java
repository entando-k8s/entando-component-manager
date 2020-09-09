package org.entando.kubernetes.model.bundle.processor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.BundleProperty;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;
import org.entando.kubernetes.model.bundle.installable.FileInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
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
public class FileProcessor implements ComponentProcessor<FileDescriptor> {

    private final EntandoCoreClient engineService;

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.ASSET;
    }

    @Override
    public List<Installable<FileDescriptor>> process(BundleReader npr) {
        final List<Installable<FileDescriptor>> installables = new LinkedList<>();

        try {
            if (npr.containsResourceFolder()) {
                final String componentFolder = "/" + npr.getBundleCode();

                List<String> resourceFiles = npr.getResourceFiles().stream().sorted().collect(Collectors.toList());
                for (final String resourceFile : resourceFiles) {
                    final FileDescriptor fileDescriptor = npr.getResourceFileAsDescriptor(resourceFile);

                    Path fileFolder = Paths.get(BundleProperty.RESOURCES_FOLDER_PATH.getValue())
                            .relativize(Paths.get(fileDescriptor.getFolder()));
                    String folder = Paths.get(componentFolder).resolve(fileFolder).toString();
                    fileDescriptor.setFolder(folder);
                    installables.add(new FileInstallable(engineService, fileDescriptor));
                }
            }
        } catch (IOException e) {
            throw new EntandoComponentManagerException("Error reading bundle", e);
        }

        return installables;
    }

    @Override
    public List<Installable<FileDescriptor>> process(List<EntandoBundleComponentJobEntity> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == ComponentType.ASSET)
                .map(c ->  new FileInstallable(engineService, this.buildDescriptorFromComponentJob(c)))
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
}
