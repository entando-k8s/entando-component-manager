package org.entando.kubernetes.service.digitalexchange.installable;

import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.service.digitalexchange.entandocore.EntandoEngineService;
import org.entando.kubernetes.service.digitalexchange.job.model.FileDescriptor;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class AssetInstallable extends Installable<FileDescriptor> {

    private final EntandoEngineService engineService;

    public AssetInstallable(final FileDescriptor fileDescriptor, final EntandoEngineService engineService) {
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
        return ComponentType.DEPLOYMENT;
    }

    @Override
    public String getName() {
        return representation.getFilename();
    }

}
