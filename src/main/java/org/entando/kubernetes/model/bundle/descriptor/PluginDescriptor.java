package org.entando.kubernetes.model.bundle.descriptor;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    DockerImage dockerImage;

    public DockerImage getDockerImage() {
        if (dockerImage == null) {
            this.dockerImage = DockerImage.fromString(this.image);
        }
        return this.dockerImage;
    }

}
