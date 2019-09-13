package org.entando.kubernetes.service.digitalexchange.job.model;

import lombok.Data;
import org.entando.entando.kubernetes.controller.model.plugin.ExpectedRole;
import org.entando.entando.kubernetes.controller.model.plugin.Permission;

import java.util.List;

@Data
public class ServiceDescriptor {

    private String image;
    private String ingressPath;
    private String healthCheckPath;
    private String dbms;

    private List<ExpectedRole> roles;
    private List<Permission> permissions;

}
