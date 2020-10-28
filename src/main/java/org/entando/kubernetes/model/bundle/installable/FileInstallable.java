package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;

@Slf4j
public class FileInstallable extends Installable<FileDescriptor> {
    private final EntandoCoreClient engineService;

    public FileInstallable(EntandoCoreClient engineService, FileDescriptor fileDescriptor) {
        super(fileDescriptor);
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
            //Do nothing since Directories and Assets are uninstalled in a different way
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
