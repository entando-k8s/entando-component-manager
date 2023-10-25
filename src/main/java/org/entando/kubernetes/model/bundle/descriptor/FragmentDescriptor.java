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
public class FragmentDescriptor implements Descriptor {

    private String code;
    private String guiCode;
    private String guiCodePath;

    @Override
    public ComponentKey getComponentKey() {
        return new ComponentKey(code);
    }
}
