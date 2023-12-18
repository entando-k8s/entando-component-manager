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
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CategoryDescriptor implements Descriptor {

    private String code;
    private String parentCode;
    private Map<String, String> titles;

    @Override
    public ComponentKey getComponentKey() {
        return new ComponentKey(code);
    }
}
