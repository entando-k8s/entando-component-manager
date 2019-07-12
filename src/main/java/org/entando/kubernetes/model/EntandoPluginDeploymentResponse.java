package org.entando.kubernetes.model;

import lombok.Data;
import org.springframework.hateoas.ResourceSupport;

import java.util.List;
import java.util.Map;

@Data
public class EntandoPluginDeploymentResponse extends ResourceSupport {

    private String plugin;
    private String image;
    private String path;
    private int replicas;
    private boolean online;
    private String deploymentPhase;

    private PluginServiceStatus serverStatus;
    private List<PluginServiceStatus> externalServiceStatuses;

    /**
     * Those must only be the configurable ones
     */
    private Map<String, String> envVariables;

}
