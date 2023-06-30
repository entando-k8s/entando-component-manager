package org.entando.kubernetes.model.entandocore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntandoCoreComponentUsageRequest {

    private String type;
    private String code;

}
