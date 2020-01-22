package org.entando.kubernetes.model.bundle.descriptor;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContentTypeDescriptor implements Descriptor {

    private String code;
    private String name;
    private String status;

    private List<ContentTypeAttribute> attributes;

}
