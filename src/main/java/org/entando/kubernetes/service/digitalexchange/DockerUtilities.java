package org.entando.kubernetes.service.digitalexchange;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderType.BundleDownloaderConstants;

@UtilityClass
@Slf4j
public class DockerUtilities {

    public static String removeDockerProtocol(String image) {
        return image != null && image.startsWith(BundleDownloaderConstants.DOCKER_PROTOCOL)
                ? image.substring(BundleDownloaderConstants.DOCKER_PROTOCOL.length())
                : image;
    }
}
