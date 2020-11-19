package org.entando.kubernetes.model.bundle.descriptor;

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
public class GroupDescriptor implements Descriptor {

    private String code;
    private String name;

    @Override
    public ComponentKey getComponentKey() {
        return new ComponentKey(code);
    }
}
