package org.entando.kubernetes.model.bundle.descriptor;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LabelDescriptor {

    private String key;
    private Map<String, String> titles;

}
