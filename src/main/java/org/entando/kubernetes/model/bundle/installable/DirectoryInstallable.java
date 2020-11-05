package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallRequest.InstallAction;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.DirectoryDescriptor;

@Slf4j
public class DirectoryInstallable extends Installable<DirectoryDescriptor> {

    private final EntandoCoreClient engineService;

    public DirectoryInstallable(EntandoCoreClient engineService, DirectoryDescriptor directory, InstallAction action) {
        super(directory, action);
        this.engineService = engineService;
    }

    @Override
    public CompletableFuture<Void> install() {
        return CompletableFuture.runAsync(() -> {
            log.info("Creating directory {}", getName());
            if (!shouldCreate()) {
                return; //Do nothing
            }

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


}
