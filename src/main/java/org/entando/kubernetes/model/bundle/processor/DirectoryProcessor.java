package org.entando.kubernetes.model.bundle.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.BundleProperty;
import org.entando.kubernetes.model.bundle.BundleReader;
import org.entando.kubernetes.model.bundle.descriptor.DirectoryDescriptor;
import org.entando.kubernetes.model.bundle.installable.DirectoryInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleComponentJob;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleJob;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Processor to handle Static files to be stored by Entando. Commonly used for js, images and css.
 * <p>
 * This processor will also create the folders.
 *
 * @author Sergio Marcelino
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DirectoryProcessor implements ComponentProcessor {

    private final EntandoCoreClient engineService;

    @Override
    public ComponentType getComponentType() {
        return ComponentType.DIRECTORY;
    }

    @Override
    public List<Installable> process(EntandoBundleJob job, BundleReader npr) {
        final List<Installable> installables = new LinkedList<>();

        try {
            if (npr.containsResourceFolder()) {
                final String componentFolder = "/" + npr.getBundleCode();
                installables.add(new DirectoryInstallable(engineService, new DirectoryDescriptor(componentFolder)));

                List<String> resourceFolders = npr.getResourceFolders().stream().sorted().collect(Collectors.toList());
                for (final String resourceFolder : resourceFolders) {
                    Path fileFolder = Paths.get(BundleProperty.RESOURCES_FOLDER_PATH.getValue())
                            .relativize(Paths.get(resourceFolder));
                    String folder = Paths.get(componentFolder).resolve(fileFolder).toString();
                    installables.add(new DirectoryInstallable(engineService, new DirectoryDescriptor(folder)));
                }
            }
        } catch (IOException e) {
            throw new EntandoComponentManagerException("Error reading bundle", e);
        }

        return installables;
    }

    @Override
    public List<Installable> process(List<EntandoBundleComponentJob> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == ComponentType.DIRECTORY)
                .map(c -> new DirectoryInstallable(engineService, this.buildDescriptorFromComponentJob(c)))
                .collect(Collectors.toList());
    }

    @Override
    public DirectoryDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJob component) {
        return new DirectoryDescriptor(component.getName());
    }
}
