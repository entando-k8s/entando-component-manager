package org.entando.kubernetes.model.bundle.downloader;

import java.util.EnumMap;
import java.util.Optional;
import java.util.function.Supplier;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderType.BundleDownloaderConstants;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;

public class BundleDownloaderFactory {

    EnumMap<BundleDownloaderType, Supplier<BundleDownloader>> downloaderSuppliers;
    Supplier<BundleDownloader> defaultSupplier;

    /**
     * Build the correct BundleDownloader implementation based on the input parameters.
     *
     * @param type                     the string must be a valid constant of BundleDownloaderConstants and is used to
     *                                 select the implementation
     * @param downloadTimeoutSeconds   the download timeout useful for DockerBundleDownloader
     * @param downloadRetries          the number of download attempts useful for DockerBundleDownloader
     * @param decompressTimeoutSeconds the decompress image timeout useful for DockerBundleDownloader
     * @return the correct BundleDownloader implementation
     */
    public static BundleDownloader getForType(String type, int downloadTimeoutSeconds, int downloadRetries,
            int decompressTimeoutSeconds) {
        if (type == null) {
            return new GitBundleDownloader();
        }

        switch (type.toLowerCase()) {
            case BundleDownloaderConstants.CODE_TYPE_NPM:
                return new NpmBundleDownloader();
            case BundleDownloaderConstants.CODE_TYPE_DOCKER:
                return new DockerBundleDownloader(downloadTimeoutSeconds, downloadRetries, decompressTimeoutSeconds);
            case BundleDownloaderConstants.CODE_TYPE_GIT:
            default:
                return new GitBundleDownloader();
        }
    }

    public BundleDownloaderFactory() {
        this.downloaderSuppliers = new EnumMap<>(BundleDownloaderType.class);
    }

    public void registerSupplier(BundleDownloaderType type, Supplier<BundleDownloader> downloaderSupplier) {
        this.downloaderSuppliers.put(type, downloaderSupplier);
    }

    public BundleDownloader newDownloader(BundleDownloaderType type) {
        return this.downloaderSuppliers.getOrDefault(type, this.getDefaultSupplier()).get();
    }

    /**
     * Returns a new BundleDownloader instance selected from the EntandoDeBundleTag tarball field.
     *
     * @param tag the EntandoDeBundleTag used to select the correct BundleDownloader implementation
     * @return a new BundleDownloader instance
     */
    public BundleDownloader newDownloader(EntandoDeBundleTag tag) {
        BundleDownloaderType type = getTypeFromTarball(tag);
        return Optional.ofNullable(this.downloaderSuppliers.get(type)).orElse(this.getDefaultSupplier()).get();
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

    private BundleDownloaderType getTypeFromTarball(EntandoDeBundleTag tag) {
        if (tag != null && tag.getTarball() != null && tag.getTarball().toLowerCase()
                .startsWith(BundleDownloaderConstants.DOCKER_PROTOCOL)) {
            return BundleDownloaderType.DOCKER;
        } else {
            // docker has explicit protocol in tarball other method no, npm is dismissing
            // so use git as default
            return BundleDownloaderType.GIT;
        }
    }
}
