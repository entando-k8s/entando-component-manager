package org.entando.kubernetes.model.bundle.installable;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleComponentJob;

@Slf4j
public class AssetInstallable extends Installable<FileDescriptor> {
    private final EntandoCoreClient engineService;

    public AssetInstallable(EntandoCoreClient engineService, FileDescriptor fileDescriptor) {
        super(fileDescriptor);
        this.engineService = engineService;
    }

    public AssetInstallable(EntandoCoreClient engineService, EntandoBundleComponentJob component) {
        super(component);
        this.engineService = engineService;
    }

    @Override
    public CompletableFuture<Void> install() {
        return CompletableFuture.runAsync(() -> {
            log.info("Uploading file {}", representation.getFilename());
            engineService.uploadFile(representation);
        });
    }

    @Override
    public CompletableFuture<Void> uninstall() {
        return CompletableFuture.runAsync(() -> {
            //Do nothing since Assets shouldn't be removed during uninstall
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

    @Override
    public FileDescriptor representationFromComponent(EntandoBundleComponentJob component) {
        List<String> split = Arrays.asList(component.getName().split("/"));
        String folder = String.join("/", split.subList(0, split.size() - 1)); //all others except last
        String filename = String.join("/", split.subList(split.size() -1, split.size())); //last

        return FileDescriptor.builder()
                .folder(folder)
                .filename(filename)
                .build();
    }

    @Override
    public InstallPriority getInstallPriority() {
        return InstallPriority.ASSET;
    }
}
