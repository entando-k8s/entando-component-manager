package org.entando.kubernetes.model.bundle.descriptor;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.util.ObjectUtils;

@AllArgsConstructor
@Getter
public enum DescriptorVersion {

    V1("v1"),
    V2("v2"),
    V3("v3"),
    V4("v4"),
    V5("v5");

    private String version;

    private static final Map<String, DescriptorVersion> versionToDescriptorVersion;

    static {
        Map<String, DescriptorVersion> map = new ConcurrentHashMap<>();
        for (DescriptorVersion instance : DescriptorVersion.values()) {
            map.put(instance.getVersion().toLowerCase(),instance);
        }
        versionToDescriptorVersion = Collections.unmodifiableMap(map);
    }

    public static DescriptorVersion fromVersion(String version) {
        if (ObjectUtils.isEmpty(version)) {
            return null;
        }
        return versionToDescriptorVersion.get(version.toLowerCase());
    }
}
