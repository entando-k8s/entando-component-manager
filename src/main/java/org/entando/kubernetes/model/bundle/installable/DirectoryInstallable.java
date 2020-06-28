package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.model.bundle.descriptor.DirectoryDescriptor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleComponentJob;

@Slf4j
public class DirectoryInstallable extends Installable<DirectoryDescriptor> {

    private final EntandoCoreClient engineService;

    public DirectoryInstallable(EntandoCoreClient engineService, DirectoryDescriptor directory) {
        super(directory);
        this.engineService = engineService;
    }

    @Override
    public CompletableFuture<Void> install() {
        return CompletableFuture.runAsync(() -> {
            log.info("Creating directory {}", getName());
            engineService.createFolder(representation.getName());
        });
    }

    @Override
    public CompletableFuture<Void> uninstall() {
        return CompletableFuture.runAsync(() -> {
            if (this.representation.isRoot()) {
                engineService.deleteFolder(this.representation.getName());
            }
        });
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.DIRECTORY;
    }

    @Override
    public String getName() {
        return representation.getName();
    }


    @Override
    public InstallPriority getInstallPriority() {
        return InstallPriority.DIRECTORY;
    }
}
