package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.service.digitalexchange.entandocore.EntandoCoreService;

@Slf4j
public class AssetInstallable extends Installable<FileDescriptor> {


    public EntandoCoreService engineService;

    public AssetInstallable(EntandoCoreService engineService,
            FileDescriptor fileDescriptor) {
        super(fileDescriptor);
        this.engineService = engineService;
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
