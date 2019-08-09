package org.entando.kubernetes.service.digitalexchange.installable;

import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.service.digitalexchange.entandocore.EntandoEngineService;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class DirectoryInstallable extends Installable<String> {

    private final EntandoEngineService engineService;

    public DirectoryInstallable(final String directory, final EntandoEngineService engineService) {
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
