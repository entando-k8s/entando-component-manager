package org.entando.kubernetes.model.bundle;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BundleStatus {

    NOT_FOUND("NOT_FOUND"),
    DEPLOYED("DEPLOYED"),
    INSTALLED("INSTALLED"),
    INSTALLED_NOT_DEPLOYED("INSTALLED_NOT_DEPLOYED"),
    UNKNOWN("UNKNOWN");

    private final String status;

    private static final Map<String, BundleStatus> statusToBundleType;

    static {
        Map<String, BundleStatus> map = new ConcurrentHashMap<>();
        for (BundleStatus instance : BundleStatus.values()) {
            map.put(instance.getStatus().toLowerCase(),instance);
        }
        statusToBundleType = Collections.unmodifiableMap(map);
    }

    public static BundleStatus fromStatus(String type) {
        return statusToBundleType.get(type.toLowerCase());
    }
}
