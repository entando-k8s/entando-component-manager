package org.entando.kubernetes.model.bundle.descriptor;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContentModelDescriptor implements Descriptor {

    private String id;
    private String contentType;
    private String description;
    private String contentShape;
    private String contentShapePath;

}
