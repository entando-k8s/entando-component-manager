package org.entando.kubernetes.model.bundle.downloader;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@AllArgsConstructor
@Getter
public enum BundleDownloaderType {
    NPM(BundleDownloaderConstants.CODE_TYPE_NPM),
    GIT(BundleDownloaderConstants.CODE_TYPE_GIT),
    DOCKER(BundleDownloaderConstants.CODE_TYPE_DOCKER);

    private String code;
    private static final Map<String, BundleDownloaderType> codeToBundleDownloaderType;

    static {
        Map<String, BundleDownloaderType> map = new ConcurrentHashMap<>();
        for (BundleDownloaderType instance : BundleDownloaderType.values()) {
            map.put(instance.getCode().toLowerCase(), instance);
        }
        codeToBundleDownloaderType = Collections.unmodifiableMap(map);
    }

    public BundleDownloaderType fromCode(String code) {
        if (StringUtils.isBlank(code)) {
            throw new IllegalArgumentException(String.format("code '%s' is not valid", code));
        } else {
            return codeToBundleDownloaderType.get(code);
        }
    }

    public static final class BundleDownloaderConstants {

        public static final String DOCKER_PROTOCOL = "docker://";
        public static final String CODE_TYPE_DOCKER = "docker";
        public static final String CODE_TYPE_GIT = "git";
        public static final String CODE_TYPE_NPM = "npm";

        private BundleDownloaderConstants() {
        }

    }
}
