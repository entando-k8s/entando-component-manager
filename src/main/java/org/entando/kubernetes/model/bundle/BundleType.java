package org.entando.kubernetes.model.bundle;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BundleType {

    STANDARD_BUNDLE,
    SYSTEM_LEVEL_BUNDLE;
}
