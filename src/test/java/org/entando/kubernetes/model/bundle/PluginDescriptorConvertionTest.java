package org.entando.kubernetes.model.bundle;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.entando.kubernetes.model.bundle.descriptor.DockerImage;
import org.entando.kubernetes.model.bundle.descriptor.PluginDescriptor;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class PluginDescriptorConvertionTest {

    @Test
    public void shouldGenerateCorrectDockerImage() {
        PluginDescriptor descriptor = getTestDescriptor();
        DockerImage dockerImage = descriptor.getDockerImage();
        assertThat(dockerImage.getName()).isEqualTo("the-lucas");
        assertThat(dockerImage.getOrganization()).isEqualTo("entando");
        assertThat(dockerImage.getVersion()).isEqualTo("0.0.1-SNAPSHOT");
    }

    @Test
    public void shouldGenerateKuberntesCompatibleNameFromDescriptor() {
        PluginDescriptor descriptor = getTestDescriptor();
        String name = BundleUtilities.extractNameFromDescriptor(descriptor);
        assertThat(name).isEqualTo("entando-the-lucas-0-0-1-snapshot");
    }

    @Test
    public void shouldGenerateKuberntesCompatibleIngressPathFromDescriptor() {
        PluginDescriptor descriptor = getTestDescriptor();
        String name = BundleUtilities.extractIngressPathFromDescriptor(descriptor);
        assertThat(name).isEqualTo("/entando/the-lucas/0-0-1-snapshot");
    }

    private PluginDescriptor getTestDescriptor() {
        return PluginDescriptor.builder()
                .image("entando/the-lucas:0.0.1-SNAPSHOT")
                .roles(Arrays.asList("thelucas-admin", "thelucas-user"))
                .healthCheckPath("/management/health")
                .dbms("postgresql")
                .build();
    }

}
