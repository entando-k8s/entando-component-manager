package org.entando.kubernetes.model.bundle.descriptor.plugin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.entando.kubernetes.model.bundle.descriptor.ComponentKey;
import org.entando.kubernetes.model.bundle.descriptor.Descriptor;
import org.entando.kubernetes.model.bundle.descriptor.DockerImage;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.springframework.util.StringUtils;

@Getter
@Setter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
public class PluginDescriptor implements Descriptor {
    /**
     * identifies the plugin descriptor version.
     */
    private String descriptorVersion;

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
    private String bundleId;
    private String fullDeploymentName;


    public PluginDescriptor() {
    }

    // About the NOSONAR:
    // a constructor with more than 7 parameters is discouraged, but we require it to fulfill lombok builder requests
    // and for the same reason we can't remove the 2 unused parameters fullDeploymentName and bundleId
    public PluginDescriptor(String descriptorVersion,
            DockerImage dockerImage, PluginDescriptorV1Spec spec, String name, String deploymentBaseName,
            String image, String healthCheckPath, String dbms, List<String> roles,
            List<PluginPermission> permissions, String ingressPath, String securityLevel,
            List<EnvironmentVariable> environmentVariables,
            String fullDeploymentName, String bundleId) { // NOSONAR
        this.descriptorVersion = descriptorVersion;
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
        this.fullDeploymentName = null; // force the private variable to be null when the descriptor is read
        this.bundleId = null; // force the private variable to be null when the descriptor is read
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

    public boolean isVersion1() {
        return StringUtils.isEmpty(image);
    }

    public boolean isVersionLowerThan3() {
        final PluginDescriptorVersion pluginDescriptorVersion = PluginDescriptorVersion.fromVersion(descriptorVersion);
        return pluginDescriptorVersion == PluginDescriptorVersion.V1
                || pluginDescriptorVersion == PluginDescriptorVersion.V2;
    }

    @Override
    public ComponentKey getComponentKey() {
        return new ComponentKey(BundleUtilities.composeDeploymentBaseName(this));
    }
}
