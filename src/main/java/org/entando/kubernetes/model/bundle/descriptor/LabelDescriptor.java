package org.entando.kubernetes.model.bundle.descriptor;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelDescriptor implements Descriptor {

    private String key;
    private Map<String, String> titles;

}
