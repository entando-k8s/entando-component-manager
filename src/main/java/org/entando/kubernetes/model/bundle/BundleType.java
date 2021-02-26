package org.entando.kubernetes.model.bundle;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BundleType {

    @JsonProperty("standard-bundle")
    STANDARD_BUNDLE,
    @JsonProperty("system-level-bundle")
    SYSTEM_LEVEL_BUNDLE;
}
