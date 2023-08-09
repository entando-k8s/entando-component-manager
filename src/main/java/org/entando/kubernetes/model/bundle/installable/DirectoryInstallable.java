package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.config.tenant.thread.ContextCompletableFuture;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.DirectoryDescriptor;

@Slf4j
public class DirectoryInstallable extends Installable<DirectoryDescriptor> {

    public DirectoryInstallable(DirectoryDescriptor directory, InstallAction action) {
        super(directory, action);
    }

    @Override
    public CompletableFuture<Void> install() {
        return ContextCompletableFuture.runAsyncWithContext(() -> {
            //Do nothing
        });
    }

    @Override
    public boolean shouldUninstallFromAppEngine() {
        boolean shouldCallDelete = this.representation.isRoot() && shouldCreate();
        log.debug("should delete:'{}' element type:'DIRECTORY' name:'{}'", shouldCallDelete, getName());
        return shouldCallDelete;
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
