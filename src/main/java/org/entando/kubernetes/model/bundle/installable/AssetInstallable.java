package org.entando.kubernetes.model.bundle.installable;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.AssetDescriptor;

@Slf4j
public class AssetInstallable extends Installable<AssetDescriptor> {

    private final File file;
    private final EntandoCoreClient engineService;

    public AssetInstallable(EntandoCoreClient service, AssetDescriptor assetDescriptor, InstallAction action) {
        this(service, assetDescriptor, null, action);
    }

    public AssetInstallable(EntandoCoreClient service, AssetDescriptor assetDescriptor, File file,
            InstallAction action) {
        super(assetDescriptor, action);
        this.file = file;
        this.engineService = service;
    }

    @Override
    public CompletableFuture<Void> install() {
        return CompletableFuture.runAsync(() -> {

            logConflictStrategyAction();

            if (shouldSkip()) {
                return; //Do nothing
            }

            if (shouldCreate()) {
                engineService.createAsset(representation, file);
            } else {
                engineService.updateAsset(representation, file);
            }
        });
    }

    @Override
    public CompletableFuture<Void> uninstallFromEcr() {
        return CompletableFuture.runAsync(() -> log.info("Removing Asset {}", getName()));
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.ASSET;
    }

    @Override
    public String getName() {
        return representation.getCorrelationCode();
    }

}
