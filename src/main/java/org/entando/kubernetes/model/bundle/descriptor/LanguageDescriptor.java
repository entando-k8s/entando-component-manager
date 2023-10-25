package org.entando.kubernetes.model.bundle.descriptor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LanguageDescriptor implements Descriptor {

    private String code;
    private String description;
    private boolean isActive;

    public boolean getIsActive() {
        return isActive;
    }

    @Override
    public ComponentKey getComponentKey() {
        return new ComponentKey(code);
    }
}
