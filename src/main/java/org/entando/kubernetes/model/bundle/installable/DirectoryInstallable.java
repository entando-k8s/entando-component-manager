package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleComponentJob;

@Slf4j
public class DirectoryInstallable extends Installable<String> {

    private final EntandoCoreClient engineService;

    public DirectoryInstallable(EntandoCoreClient engineService, String directory) {
        super(directory);
        this.engineService = engineService;
    }

    public DirectoryInstallable(EntandoCoreClient engineService, EntandoBundleComponentJob component) {
        super(component);
        this.engineService = engineService;
    }

    @Override
    public CompletableFuture<Void> install() {
        return CompletableFuture.runAsync(() -> {
            log.info("Creating directory {}", getName());
            engineService.createFolder(representation);
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
        return ComponentType.DIRECTORY;
    }

    @Override
    public String getName() {
        return representation;
    }

    @Override
    public String representationFromComponent(EntandoBundleComponentJob component) {
        return component.getName();
    }

    @Override
    public InstallPriority getInstallPriority() {
        return InstallPriority.DIRECTORY;
    }
}
