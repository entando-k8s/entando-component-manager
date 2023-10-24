package org.entando.kubernetes.model.bundle.descriptor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
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
public class LabelDescriptor implements Descriptor {

    private String key;
    private Map<String, String> titles;

    @Override
    public ComponentKey getComponentKey() {
        return new ComponentKey(key);
    }
}
