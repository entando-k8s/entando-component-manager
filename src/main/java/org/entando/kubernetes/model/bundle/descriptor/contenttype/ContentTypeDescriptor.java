package org.entando.kubernetes.model.bundle.descriptor.contenttype;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.entando.kubernetes.model.bundle.descriptor.ComponentKey;
import org.entando.kubernetes.model.bundle.descriptor.Descriptor;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentTypeDescriptor implements Descriptor {

    private String code;
    private String name;
    private String status;
    private String defaultContentModel;
    private String defaultContentModelList;
    private String viewPage;

    private List<ContentTypeAttribute> attributes;

    @Override
    public ComponentKey getComponentKey() {
        return new ComponentKey(code);
    }

}
