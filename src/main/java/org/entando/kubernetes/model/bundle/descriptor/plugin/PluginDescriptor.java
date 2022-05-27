package org.entando.kubernetes.model.bundle.descriptor.plugin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.descriptor.ComponentKey;
import org.entando.kubernetes.model.bundle.descriptor.DockerImage;
import org.entando.kubernetes.model.bundle.descriptor.VersionedDescriptor;
import org.springframework.util.ObjectUtils;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
public class PluginDescriptor extends VersionedDescriptor {

    @AllArgsConstructor
    @Getter
    public static class DescriptorMetadata {
        private final String bundleId;
        private final String pluginId;
        private final String fullDeploymentName;
    }

    /**
     * Cache of the processing of the image field.
     */
    private DockerImage dockerImage;

    // ------------------------------------------------------------
    // Version 1

    /**
     * Specification object that will be used on the {@link org.entando.kubernetes.model.plugin.EntandoPlugin} custom
     * resource.
     */
    private PluginDescriptorV1Spec spec;

    // ------------------------------------------------------------
    // Version 2
    /**
     * Name of the plugin.
     */
    private String name;
    /**
     * Base name of the plugin deployments.
     */
    private String deploymentBaseName;
    /**
     * The full qualified docker image name.
     */
    private String image;
    /**
     * The relative path of the standard kubernetes health check.
     */
    private String healthCheckPath;
    /**
     * Type of dbms to be supported on the plugin.
     */
    private String dbms;
    /**
     * List of roles to be assigned to the plugin.
     */
    private List<String> roles;
    /**
     * List of permissions to be assigned to the plugin.
     */
    private List<PluginPermission> permissions;
    /**
     * The ingress path to assign to the current plugin. If not specified it will be composed by the image name
     */
    private String ingressPath;
    /**
     * The security level to apply to the current plugin.
     */
    private String securityLevel;
    /**
     * The list of the environment variable to inject in the plugin pod.
     */
    private List<EnvironmentVariable> environmentVariables;

    /******************************************************************
     * private variables that can't be set from the yaml descriptor.
     *****************************************************************/
    private DescriptorMetadata descriptorMetadata;


    public PluginDescriptor() {
    }

    @Builder
    public PluginDescriptor(String descriptorVersion,
            DockerImage dockerImage, PluginDescriptorV1Spec spec, String name, String deploymentBaseName,
            String image, String healthCheckPath, String dbms, List<String> roles,
            List<PluginPermission> permissions, String ingressPath, String securityLevel,
            List<EnvironmentVariable> environmentVariables) {
        super.setDescriptorVersion(descriptorVersion);
        this.dockerImage = dockerImage;
        this.spec = spec;
        this.name = name;
        this.deploymentBaseName = deploymentBaseName;
        this.image = image;
        this.healthCheckPath = healthCheckPath;
        this.dbms = dbms;
        this.roles = roles;
        this.permissions = permissions;
        this.ingressPath = ingressPath;
        this.securityLevel = securityLevel;
        this.environmentVariables = environmentVariables;
    }

    public DockerImage getDockerImage() {
        if (dockerImage == null) {
            if (!isVersion1()) {
                this.dockerImage = DockerImage.fromString(this.image);
            } else {
                this.dockerImage = DockerImage.fromString(this.spec.getImage());
            }
        }
        return this.dockerImage;
    }

    @Override
    public boolean isVersion1() {
        return ObjectUtils.isEmpty(image);
    }

    public boolean isVersionLowerThan3() {
        final PluginDescriptorVersion pluginDescriptorVersion = PluginDescriptorVersion.fromVersion(
                super.getDescriptorVersion());
        return pluginDescriptorVersion == PluginDescriptorVersion.V1
                || pluginDescriptorVersion == PluginDescriptorVersion.V2;
    }

    @Override
    public ComponentKey getComponentKey() {
        return new ComponentKey(this.getDescriptorMetadata().getFullDeploymentName());
    }

    public PluginDescriptor setDescriptorMetadata(String bundleId, String pluginId, String fullDeploymentName) {
        if (ObjectUtils.isEmpty(bundleId)) {
            throw new EntandoComponentManagerException("Empty bundle id received as plugin metadata");
        }
        if (ObjectUtils.isEmpty(pluginId)) {
            throw new EntandoComponentManagerException("Empty plugin id received as plugin metadata");
        }
        if (ObjectUtils.isEmpty(fullDeploymentName)) {
            throw new EntandoComponentManagerException("Empty full deployment name received as plugin metadata");
        }
        this.descriptorMetadata = new DescriptorMetadata(bundleId, pluginId, fullDeploymentName);
        return this;
    }
}
