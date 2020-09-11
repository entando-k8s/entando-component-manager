package org.entando.kubernetes.model.bundle;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.entando.kubernetes.model.DbmsVendor;
import org.entando.kubernetes.model.bundle.descriptor.DockerImage;
import org.entando.kubernetes.model.bundle.descriptor.PluginDescriptor;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.ExpectedRole;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class PluginDescriptorConvertionTest {

    public static final String EXPECTED_PLUGIN_NAME = "entando-the-lucas-0-0-1-snapshot";
    public static final String EXPECTED_INGRESS_PATH = "/entando/the-lucas/0-0-1-snapshot";
    public static final String TEST_DESCRIPTOR_IMAGE = "entando/the-lucas:0.0.1-SNAPSHOT";
    public static final String TEST_DESCRIPTOR_ADMIN_ROLE = "thelucas-admin";
    public static final String TEST_DESCRIPTOR_USER_ROLE = "thelucas-user";
    public static final String TEST_DESCRIPTOR_HEALTH_PATH = "/management/health";
    public static final String TEST_DESCRIPTOR_DBMS = "postgresql";

    @Test
    public void shouldGenerateCorrectDockerImage() {
        PluginDescriptor descriptor = getTestDescriptor();
        DockerImage dockerImage = descriptor.getDockerImage();
        assertThat(dockerImage.getName()).isEqualTo("the-lucas");
        assertThat(dockerImage.getOrganization()).isEqualTo("entando");
        assertThat(dockerImage.getVersion()).isEqualTo("0.0.1-SNAPSHOT");
    }

    @Test
    public void shouldGenerateKubernetesCompatibleNameFromDescriptor() {
        PluginDescriptor descriptor = getTestDescriptor();
        String name = BundleUtilities.extractNameFromDescriptor(descriptor);
        assertThat(name).isEqualTo(EXPECTED_PLUGIN_NAME);
    }

    @Test
    public void shouldGenerateKubernetesCompatibleIngressPathFromDescriptor() {
        PluginDescriptor descriptor = getTestDescriptor();
        String name = BundleUtilities.extractIngressPathFromDescriptor(descriptor);
        assertThat(name).isEqualTo(EXPECTED_INGRESS_PATH);
    }

    @Test
    public void shouldConvertDescriptorToEntandoPlugin() {
        PluginDescriptor d = getTestDescriptor();
        EntandoPlugin p = BundleUtilities.generatePluginFromDescriptor(d);

        assertThat(p.getMetadata().getName()).isEqualTo(EXPECTED_PLUGIN_NAME);
        assertThat(p.getSpec().getImage()).isEqualTo(TEST_DESCRIPTOR_IMAGE);
        assertThat(p.getSpec().getIngressPath()).isEqualTo(EXPECTED_INGRESS_PATH);
        assertThat(p.getSpec().getHealthCheckPath()).isEqualTo(TEST_DESCRIPTOR_HEALTH_PATH);

        Map<String, String> lbls = new HashMap<>();
        lbls.put("organization", "entando");
        lbls.put("name", "the-lucas");
        lbls.put("version", "0.0.1-SNAPSHOT");
        assertThat(p.getMetadata().getLabels()).containsAllEntriesOf(lbls);

        List<ExpectedRole> expectedRoles = p.getSpec().getRoles();
        List<String> expectedRolesCodes = Arrays.asList(TEST_DESCRIPTOR_ADMIN_ROLE, TEST_DESCRIPTOR_USER_ROLE);
        assertThat(expectedRoles.size()).isEqualTo(2);
        assertThat(expectedRoles).allMatch(role ->
                expectedRolesCodes.contains(role.getCode()) && role.getCode().equals(role.getName()));
        assertThat(p.getSpec().getRoles().stream()
                .map(ExpectedRole::getCode)
                .collect(Collectors.toList()))
                .containsExactly(TEST_DESCRIPTOR_ADMIN_ROLE, TEST_DESCRIPTOR_USER_ROLE);
        assertThat(p.getSpec().getRoles().stream()
                .map(ExpectedRole::getName)
                .collect(Collectors.toList()))
                .containsExactly(TEST_DESCRIPTOR_ADMIN_ROLE, TEST_DESCRIPTOR_USER_ROLE);

        assertThat(p.getSpec().getDbms().isPresent()).isTrue();
        assertThat(p.getSpec().getDbms().get()).isEqualTo(DbmsVendor.POSTGRESQL);
    }

    private PluginDescriptor getTestDescriptor() {
        return PluginDescriptor.builder()
                .image(TEST_DESCRIPTOR_IMAGE)
                .roles(Arrays.asList(TEST_DESCRIPTOR_ADMIN_ROLE, TEST_DESCRIPTOR_USER_ROLE))
                .healthCheckPath(TEST_DESCRIPTOR_HEALTH_PATH)
                .dbms(TEST_DESCRIPTOR_DBMS)
                .build();
    }

}
