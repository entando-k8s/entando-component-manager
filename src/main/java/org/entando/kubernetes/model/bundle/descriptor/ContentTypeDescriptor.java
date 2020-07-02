package org.entando.kubernetes.model.bundle.descriptor;

import java.util.List;
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
public class ContentTypeDescriptor implements Descriptor {

    private String code;
    private String name;
    private String status;
    private String defaultContentModel;
    private String defaultContentModelList;

    private List<ContentTypeAttribute> attributes;

}
