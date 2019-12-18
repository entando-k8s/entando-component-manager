package org.entando.kubernetes.model.bundle.descriptor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComponentDescriptor implements Descriptor {

    private String code;
    private String description;

    private ComponentSpecDescriptor components;

}
