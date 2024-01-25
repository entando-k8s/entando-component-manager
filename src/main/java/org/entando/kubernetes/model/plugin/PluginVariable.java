package org.entando.kubernetes.model.plugin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class PluginVariable {

    private String apiClaimName;
    private String name;
    private String value;

}
