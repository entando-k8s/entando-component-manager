package org.entando.kubernetes.model.bundle.descriptor;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentTypeDescriptor implements Descriptor {

    private String code;
    private String name;
    private String status;

    private List<ContentTypeAttribute> attributes;

}
