package org.entando.kubernetes.model.bundle.descriptor.widget;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.entando.kubernetes.model.bundle.descriptor.DescriptorVersion;
import org.springframework.util.ObjectUtils;

@AllArgsConstructor
@Getter
public enum WidgetDescriptorVersion implements DescriptorVersion {

    V1(DescriptorVersion.V1),
    V5("v5");

    private String version;

    private static final Map<String, WidgetDescriptorVersion> versionToDescriptorVersion;

    static {
        Map<String, WidgetDescriptorVersion> map = new ConcurrentHashMap<>();
        for (WidgetDescriptorVersion instance : WidgetDescriptorVersion.values()) {
            map.put(instance.getVersion().toLowerCase(),instance);
        }
        versionToDescriptorVersion = Collections.unmodifiableMap(map);
    }

    public static WidgetDescriptorVersion fromVersion(String version) {
        if (ObjectUtils.isEmpty(version)) {
            return null;
        }
        return versionToDescriptorVersion.get(version.toLowerCase());
    }
}
