package org.entando.kubernetes.model.bundle.descriptor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BundleDescriptor implements Descriptor {

    private String code;
    private String description;
    private String bundleType;

    private ComponentSpecDescriptor components;

    @Override
    public ComponentKey getComponentKey() {
        return new ComponentKey(code);
    }
}
