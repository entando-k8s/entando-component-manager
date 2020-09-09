package org.entando.kubernetes.model.bundle.descriptor;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.entando.kubernetes.model.DbmsVendor;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginDescriptor implements Descriptor {

    List<String> roles;
    String image;
    String healthCheckPath;
    String dbms;

}
