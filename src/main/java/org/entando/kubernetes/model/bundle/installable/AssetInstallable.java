package org.entando.kubernetes.model.bundle.installable;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.AssetDescriptor;

@Slf4j
public class AssetInstallable extends Installable<AssetDescriptor> {

    private final File file;
    private final EntandoCoreClient engineService;

    public AssetInstallable(EntandoCoreClient service, AssetDescriptor assetDescriptor) {
        this(service, assetDescriptor, null);
    }

    public AssetInstallable(EntandoCoreClient service, AssetDescriptor assetDescriptor, File file) {
        super(assetDescriptor);
        this.file = file;
        this.engineService = service;
    }

    @Override
    public CompletableFuture<Void> install() {
        return CompletableFuture.runAsync(() -> {
            log.info("Registering CMS Asset {}", getName());
            engineService.createAsset(representation, file);
        });
    }

    @Override
    public CompletableFuture<Void> uninstall() {
        return CompletableFuture.runAsync(() -> {
            log.info("Removing Content {}", getName());
            engineService.deleteAsset("cc=" + getName());
        });
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
