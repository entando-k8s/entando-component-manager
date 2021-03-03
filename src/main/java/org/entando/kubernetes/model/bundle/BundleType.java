package org.entando.kubernetes.model.bundle;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BundleType {

    STANDARD_BUNDLE("standard-bundle"),
    SYSTEM_LEVEL_BUNDLE("system-level-bundle");

    private final String type;
}
