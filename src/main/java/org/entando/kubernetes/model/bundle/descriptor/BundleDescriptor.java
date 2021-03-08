package org.entando.kubernetes.model.bundle.descriptor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.entando.kubernetes.model.bundle.BundleType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BundleDescriptor implements Descriptor {

    private String code;
    private String description;
    @JsonProperty("bundle-type")
    private BundleType bundleType;

    private ComponentSpecDescriptor components;

    @Override
    public ComponentKey getComponentKey() {
        return new ComponentKey(code);
    }
}
