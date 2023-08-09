package org.entando.kubernetes.model.bundle.installable;

import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.config.tenant.thread.ContextCompletableFuture;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
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
        return ContextCompletableFuture.runAsync(() -> {

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
    public CompletableFuture<Void> uninstallFromEcr() {
        return CompletableFuture.runAsync(() -> {
            //Do nothing since Directories and Assets are uninstalled in a different way
        });
    }

    @Override
    public boolean shouldUninstallFromAppEngine() {
        log.debug("should delete:'false' element type:'RESOURCE' name:'{}'", getName());
        return false;
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.RESOURCE;
    }

    @Override
    public String getName() {
        return Paths.get(representation.getFolder(), representation.getFilename()).toString();
    }


}
