package org.entando.kubernetes.model.bundle;

import com.fasterxml.jackson.annotation.JsonProperty;
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
}
