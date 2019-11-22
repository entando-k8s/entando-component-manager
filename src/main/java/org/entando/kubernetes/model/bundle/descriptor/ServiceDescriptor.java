package org.entando.kubernetes.model.bundle.descriptor;

import lombok.Data;
import org.entando.kubernetes.model.plugin.ExpectedRole;
import org.entando.kubernetes.model.plugin.Permission;

import java.util.List;
import org.entando.kubernetes.model.plugin.ExpectedRole;

@Data
public class ServiceDescriptor {

    private String image;
    private String ingressPath;
    private String healthCheckPath;
    private String dbms;

    private List<ExpectedRole> roles;
    private List<Permission> permissions;

}
