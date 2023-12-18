package org.entando.kubernetes.model.bundle.descriptor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssetDescriptor implements Descriptor {

    private String correlationCode;
    private String type;
    private String name;
    private String description;
    private String group;
    private String[] categories;

    @Override
    public ComponentKey getComponentKey() {
        return new ComponentKey(correlationCode);
    }
}
