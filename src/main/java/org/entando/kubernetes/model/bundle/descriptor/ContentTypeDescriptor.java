package org.entando.kubernetes.model.bundle.descriptor;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter@Setter
public class ContentTypeDescriptor implements Descriptor {

    private String code;
    private String name;
    private String status;

    private List<ContentTypeAttribute> attributes;

}
