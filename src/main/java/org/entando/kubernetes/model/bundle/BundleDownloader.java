package org.entando.kubernetes.model.bundle;

import java.nio.file.Path;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;

public interface BundleDownloader {

    Path saveBundleLocally(EntandoDeBundleTag tag, Path destination);

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
