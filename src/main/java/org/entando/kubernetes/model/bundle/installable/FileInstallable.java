package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallRequest.InstallAction;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;

@Slf4j
public class FileInstallable extends Installable<FileDescriptor> {

    private final EntandoCoreClient engineService;

    public FileInstallable(EntandoCoreClient engineService, FileDescriptor fileDescriptor, InstallAction action) {
        super(fileDescriptor, action);
        this.engineService = engineService;
    }


    @Override
    public CompletableFuture<Void> install() {
        return CompletableFuture.runAsync(() -> {

            logConflictStrategyAction();

            if (shouldSkip()) {
                return; //Do nothing
            }

            if (shouldCreate()) {
                engineService.createFile(representation);
            } else {
                engineService.updateFile(representation);
            }
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
