package org.entando.kubernetes.model.entandocore;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EntandoCoreComponentUsage {
    private String type;
    private String code;
    private int usage;
}
