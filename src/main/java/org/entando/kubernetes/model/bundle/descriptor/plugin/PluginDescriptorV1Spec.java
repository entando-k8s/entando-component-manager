package org.entando.kubernetes.model.bundle.descriptor.plugin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class PluginDescriptorV1Spec {

    private String image;
    private String dbms;
    private String healthCheckPath;
    private String securityLevel;
    private List<PluginDescriptorV1Role> roles;
}
