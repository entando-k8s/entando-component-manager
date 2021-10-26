package org.entando.kubernetes.model.bundle.descriptor.plugin;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.util.ObjectUtils;

@AllArgsConstructor
@Getter
public enum PluginDescriptorVersion {

    V1("v1"),
    V2("v2"),
    V3("v3"),
    V4("v4");

    private String version;

    private static final Map<String, PluginDescriptorVersion> versionToPluginDescriptorVersion;

    static {
        Map<String, PluginDescriptorVersion> map = new ConcurrentHashMap<>();
        for (PluginDescriptorVersion instance : PluginDescriptorVersion.values()) {
            map.put(instance.getVersion().toLowerCase(),instance);
        }
        versionToPluginDescriptorVersion = Collections.unmodifiableMap(map);
    }

    public static PluginDescriptorVersion fromVersion(String version) {
        if (ObjectUtils.isEmpty(version)) {
            return null;
        }
        return versionToPluginDescriptorVersion.get(version.toLowerCase());
    }
}
