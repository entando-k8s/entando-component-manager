package org.entando.kubernetes.model.bundle.downloader;

import java.nio.file.Path;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@AllArgsConstructor
public class DownloadedBundle {

    /**
     * points to the local directory where the bundle has been saved.
     */
    private Path localBundlePath;

    /**
     * the digest of the bundle.
     */
    private String bundleDigest;
}
