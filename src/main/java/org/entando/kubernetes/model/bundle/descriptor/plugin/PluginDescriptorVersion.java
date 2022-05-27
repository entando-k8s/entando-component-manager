package org.entando.kubernetes.model.bundle.descriptor.plugin;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.entando.kubernetes.model.bundle.descriptor.DescriptorVersion;
import org.springframework.util.ObjectUtils;

@AllArgsConstructor
@Getter
public enum PluginDescriptorVersion implements DescriptorVersion {

    V1(DescriptorVersion.V1),
    V2("v2"),
    V3("v3"),
    V4("v4");

    private String version;

    private static final Map<String, PluginDescriptorVersion> versionToDescriptorVersion;

    static {
        Map<String, PluginDescriptorVersion> map = new ConcurrentHashMap<>();
        for (PluginDescriptorVersion instance : PluginDescriptorVersion.values()) {
            map.put(instance.getVersion().toLowerCase(),instance);
        }
        versionToDescriptorVersion = Collections.unmodifiableMap(map);
    }

    public static PluginDescriptorVersion fromVersion(String version) {
        if (ObjectUtils.isEmpty(version)) {
            return null;
        }
        return versionToDescriptorVersion.get(version.toLowerCase());
    }
}
