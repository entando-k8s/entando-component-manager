package org.entando.kubernetes.model.bundle;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BundleType {

    @JsonProperty("standard-bundle")
    STANDARD_BUNDLE("standard-bundle"),
    @JsonProperty("system-level-bundle")
    SYSTEM_LEVEL_BUNDLE("system-level-bundle");

    private final String type;

    private static final Map<String, BundleType> ENUM_MAP;

    static {
        Map<String, BundleType> map = new ConcurrentHashMap<>();
        for (BundleType instance : BundleType.values()) {
            map.put(instance.getType().toLowerCase(),instance);
        }
        ENUM_MAP = Collections.unmodifiableMap(map);
    }

    public static BundleType getFromType(String type) {
        return ENUM_MAP.get(type.toLowerCase());
    }
}
