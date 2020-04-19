package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.client.core.EntandoCoreClient;

@Slf4j
public class DirectoryInstallable extends Installable<String> {

    private EntandoCoreClient engineService;

    public DirectoryInstallable(EntandoCoreClient engineService, String directory) {
        super(directory);
        this.engineService = engineService;
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
