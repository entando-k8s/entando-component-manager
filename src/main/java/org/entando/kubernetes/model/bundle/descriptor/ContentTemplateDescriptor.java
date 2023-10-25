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
public class ContentTemplateDescriptor implements Descriptor {

    private String id;
    private String contentType;
    private String description;
    private String contentShape;
    private String contentShapePath;

    @Override
    public ComponentKey getComponentKey() {
        return new ComponentKey(id);
    }
}
