package org.entando.kubernetes.model.bundle.downloader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;

@Slf4j
public abstract class BundleDownloader {

    protected Path targetPath;

    public BundleDownloader() {

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

    public Path saveBundleLocally(EntandoDeBundle bundle, EntandoDeBundleTag tag) {
        log.info("Downloading bundle " + bundle.getMetadata().getName() + "@" + tag.getVersion() + " locally");
        try {
            createTargetDirectory();
            saveBundleStrategy(tag, targetPath);
            log.info("Bundle downloaded locally at path " + targetPath.toAbsolutePath());
        } catch (BundleDownloaderException | IOException e) {
            log.error("An error occurred during download operation", e);
            throw new BundleDownloaderException(e);
        }
        return this.targetPath;
    }

    public Path saveBundleLocally(String url) {
        log.info("Downloading bundle " + url + " locally");
        try {
            createTargetDirectory();
            saveBundleStrategy(url, targetPath);
            log.info("Bundle downloaded locally at path " + targetPath.toAbsolutePath());
        } catch (BundleDownloaderException | IOException e) {
            log.error("An error occurred during download operation", e);
            throw new BundleDownloaderException(e);
        }
        return this.targetPath;
    }

    protected abstract Path saveBundleStrategy(EntandoDeBundleTag tag, Path targetPath);

    protected abstract Path saveBundleStrategy(String url, Path targetPath);

    public abstract List<String> fetchRemoteTags(String repoUrl);

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

    public enum Type {
        NPM,
        GIT
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
}
