package org.entando.kubernetes.model.bundle.downloader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.bundle.EntandoComponentBundle;
import org.entando.kubernetes.model.bundle.EntandoComponentBundleVersion;

@Slf4j
public abstract class BundleDownloader {

    protected Path targetPath;

    public enum Type {
        NPM,
        GIT
    }

    public BundleDownloader() {

    }

    public Path saveBundleLocally(EntandoComponentBundle bundle, EntandoComponentBundleVersion version) {
        log.info("Downloading bundle " + bundle.getMetadata().getName() + "@" + version.getVersion() + " locally");
        try {
            createTargetDirectory();
            saveBundleStrategy(bundle, version, targetPath);
            log.info("Bundle downloaded locally at path " + targetPath.toAbsolutePath());
        } catch (BundleDownloaderException | IOException e) {
            log.error("An error occurred while during download operation", e);
            throw new BundleDownloaderException(e);
        }
        return this.targetPath;
    }

    protected abstract Path saveBundleStrategy(EntandoComponentBundle bundle, EntandoComponentBundleVersion version, Path targetPath);

    public Path getTargetPath() {
        return this.targetPath;
    }

    public Path createTargetDirectory() throws IOException {
        targetPath = Files.createTempDirectory(null);
        return targetPath;
    }

    public void cleanTargetDirectory() {
        if (targetPath != null && targetPath.toFile().exists()) {
            try (Stream<Path> localFiles = Files.walk(targetPath)) {
                localFiles
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                throw new BundleDownloaderException(
                        "An error occurred while cleaning up environment post bundle install", e);
            }
        }
    }

    public static class BundleDownloaderException extends RuntimeException {

        public BundleDownloaderException() {
            super();
        }

        public BundleDownloaderException(String message) {
            super(message);
        }

        public BundleDownloaderException(String message, Throwable cause) {
            super(message, cause);
        }

        public BundleDownloaderException(Throwable cause) {
            super(cause);
        }

        protected BundleDownloaderException(String message, Throwable cause, boolean enableSuppression,
                boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }

    public static BundleDownloader getForType(String type) {
        if (type == null) {
            return new GitBundleDownloader();
        }

        switch (type.toLowerCase()) {
            case "npm":
                return new NpmBundleDownloader();
            case "git":
            default:
                return new GitBundleDownloader();
        }
    }
}
