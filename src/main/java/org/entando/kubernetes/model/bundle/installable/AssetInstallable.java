package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;

@Slf4j
public class AssetInstallable extends Installable<FileDescriptor> {


    public EntandoCoreClient engineService;

    public AssetInstallable(EntandoCoreClient engineService,
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
