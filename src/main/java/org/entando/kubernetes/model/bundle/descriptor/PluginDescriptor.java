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

    String name;
    String image;
    String healthCheckPath;
    String dbms;
    List<String> roles;

    DockerImage dockerImage;

    public DockerImage getDockerImage() {
        if (dockerImage == null) {
            this.dockerImage = DockerImage.fromString(this.image);
        }
        return this.dockerImage;
    }

}
