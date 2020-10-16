package org.entando.kubernetes.model.bundle.descriptor.plugin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.entando.kubernetes.model.bundle.descriptor.Descriptor;
import org.entando.kubernetes.model.bundle.descriptor.DockerImage;
import org.springframework.util.StringUtils;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PluginDescriptor implements Descriptor {

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
}
