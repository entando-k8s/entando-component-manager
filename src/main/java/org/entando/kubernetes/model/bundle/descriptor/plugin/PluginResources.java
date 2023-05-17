package org.entando.kubernetes.model.bundle.descriptor.plugin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class PluginResources {

    private String storage;
    private String memory;
    private String cpu;
}
