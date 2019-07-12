package org.entando.kubernetes.model;

import lombok.Data;
import org.entando.kubernetes.model.plugin.ExpectedRole;
import org.entando.kubernetes.model.plugin.Permission;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

@Data
public class EntandoPluginDeploymentRequest {

    @NotEmpty private String image;
    @NotEmpty private String plugin;
    @NotEmpty private String ingressPath;
    @NotEmpty private String healthCheckPath;
    @NotEmpty private String dbms;

    @Valid private List<ExpectedRole> roles = new ArrayList<>();
    @Valid private List<Permission> permissions = new ArrayList<>();

}
