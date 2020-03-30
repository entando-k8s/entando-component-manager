package org.entando.kubernetes.model.bundle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.entando.kubernetes.exception.job.JobExecutionException;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;

public abstract class BundleDownloader {

    protected Path targetPath;

    public BundleDownloader() {

    }

    abstract public Path saveBundleLocally(EntandoDeBundleTag tag);

    public Path createTargetDirectory() throws IOException {
        targetPath = Files.createTempDirectory(null);
        return targetPath;
    }

    public void cleanTargetDirectory() {
        try {
            if (targetPath != null && targetPath.toFile().exists()) {
                Files.walk(targetPath)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (IOException e) {
            throw new BundleDownloaderException("An error occurred while cleaning up environment post bundle install", e);
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
}
