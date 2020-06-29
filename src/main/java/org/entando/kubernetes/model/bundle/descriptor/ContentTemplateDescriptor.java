package org.entando.kubernetes.model.bundle.descriptor;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentTemplateDescriptor implements Descriptor {

    private String id;
    private String contentType;
    private String description;
    private String contentShape;
    private String contentShapePath;

}
