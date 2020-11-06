package org.entando.kubernetes.model.bundle.downloader;

import java.util.EnumMap;
import java.util.function.Supplier;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloader.Type;

public class BundleDownloaderFactory {

    EnumMap<Type, Supplier<BundleDownloader>> downloaderSuppliers;
    Supplier<BundleDownloader> defaultSupplier;

    public BundleDownloaderFactory() {
        this.downloaderSuppliers = new EnumMap<>(Type.class);
    }

    public void registerSupplier(Type type, Supplier<BundleDownloader> downloaderSupplier) {
        this.downloaderSuppliers.put(type, downloaderSupplier);
    }

    public BundleDownloader newDownloader(Type type) {
        return this.downloaderSuppliers.getOrDefault(type, this.getDefaultSupplier()).get();
    }

    public BundleDownloader newDownloader() {
        return this.getDefaultSupplier().get();
    }

    public Supplier<BundleDownloader> getDefaultSupplier() {
        return this.defaultSupplier;
    }

    public void setDefaultSupplier(Supplier<BundleDownloader> defaultSupplier) {
        this.defaultSupplier = defaultSupplier;
    }

}
