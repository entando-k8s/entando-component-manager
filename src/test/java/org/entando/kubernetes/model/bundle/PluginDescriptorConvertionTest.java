package org.entando.kubernetes.model.bundle;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.entando.kubernetes.model.DbmsVendor;
import org.entando.kubernetes.model.bundle.descriptor.DockerImage;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.ExpectedRole;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.utils.TestInstallUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class PluginDescriptorConvertionTest {

    @Test
    void shouldGenerateCorrectDockerImageFromVersion1() {
        PluginDescriptor descriptor = TestInstallUtils.getTestDescriptorVersion1();
        DockerImage dockerImage = descriptor.getDockerImage();
        assertThat(dockerImage.getName()).isEqualTo("the-lucas");
        assertThat(dockerImage.getOrganization()).isEqualTo("entando");
        assertThat(dockerImage.getVersion()).isEqualTo("0.0.1-SNAPSHOT");
    }

    @Test
    void shouldGenerateCorrectDockerImage() {
        PluginDescriptor descriptor = TestInstallUtils.getTestDescriptor();
        DockerImage dockerImage = descriptor.getDockerImage();
        assertThat(dockerImage.getName()).isEqualTo("the-lucas");
        assertThat(dockerImage.getOrganization()).isEqualTo("entando");
        assertThat(dockerImage.getVersion()).isEqualTo("0.0.1-SNAPSHOT");
    }


    @Test
    void shouldGenerateKubernetesCompatibleNameFromDescriptor() {
        PluginDescriptor descriptor = TestInstallUtils.getTestDescriptor();
        String name = BundleUtilities.extractNameFromDescriptor(descriptor);
        assertThat(name).isEqualTo(TestInstallUtils.EXPECTED_PLUGIN_NAME);
    }

    @Test
    void shouldGenerateKubernetesCompatibleNameFromDescriptorVersion1() {
        PluginDescriptor descriptor = TestInstallUtils.getTestDescriptorVersion1();
        String name = BundleUtilities.extractNameFromDescriptor(descriptor);
        assertThat(name).isEqualTo(TestInstallUtils.EXPECTED_PLUGIN_NAME);
    }

    @Test
    void shouldGenerateKubernetesCompatibleIngressPathFromDescriptor() {
        PluginDescriptor descriptor = TestInstallUtils.getTestDescriptor();
        String name = BundleUtilities.extractIngressPathFromDescriptor(descriptor);
        assertThat(name).isEqualTo(TestInstallUtils.EXPECTED_INGRESS_PATH);
    }

    @Test
    void shouldGenerateKubernetesCompatibleIngressPathFromDescriptorVersion1() {
        PluginDescriptor descriptor = TestInstallUtils.getTestDescriptorVersion1();
        String name = BundleUtilities.extractIngressPathFromDescriptor(descriptor);
        assertThat(name).isEqualTo(TestInstallUtils.EXPECTED_INGRESS_PATH);
    }

    @Test
    void shouldConvertDescriptorToEntandoPlugin() {
        PluginDescriptor d = TestInstallUtils.getTestDescriptor();
        EntandoPlugin p = BundleUtilities.generatePluginFromDescriptor(d);

        assertOnConvertedEntandoPlugin(p);
    }


    @Test
    void shouldConvertDescriptorToEntandoPluginVersion1() {
        PluginDescriptor d = TestInstallUtils.getTestDescriptorVersion1();
        EntandoPlugin p = BundleUtilities.generatePluginFromDescriptor(d);

        assertOnConvertedEntandoPlugin(p);
    }

    private void assertOnConvertedEntandoPlugin(EntandoPlugin p) {
        assertThat(p.getMetadata().getName()).isEqualTo(TestInstallUtils.EXPECTED_PLUGIN_NAME);
        assertThat(p.getSpec().getImage()).isEqualTo(TestInstallUtils.TEST_DESCRIPTOR_IMAGE);
        assertThat(p.getSpec().getIngressPath()).isEqualTo(TestInstallUtils.EXPECTED_INGRESS_PATH);
        assertThat(p.getSpec().getHealthCheckPath()).isEqualTo(TestInstallUtils.TEST_DESCRIPTOR_HEALTH_PATH);

        Map<String, String> lbls = new HashMap<>();
        lbls.put("organization", "entando");
        lbls.put("name", "the-lucas");
        lbls.put("version", "0.0.1-SNAPSHOT");
        assertThat(p.getMetadata().getLabels()).containsAllEntriesOf(lbls);

        List<ExpectedRole> expectedRoles = p.getSpec().getRoles();
        List<String> expectedRolesCodes = Arrays
                .asList(TestInstallUtils.TEST_DESCRIPTOR_ADMIN_ROLE, TestInstallUtils.TEST_DESCRIPTOR_USER_ROLE);
        assertThat(expectedRoles.size()).isEqualTo(2);
        assertThat(expectedRoles).allMatch(role ->
                expectedRolesCodes.contains(role.getCode()) && role.getCode().equals(role.getName()));
        assertThat(p.getSpec().getRoles().stream()
                .map(ExpectedRole::getCode)
                .collect(Collectors.toList()))
                .containsExactly(TestInstallUtils.TEST_DESCRIPTOR_ADMIN_ROLE,
                        TestInstallUtils.TEST_DESCRIPTOR_USER_ROLE);
        assertThat(p.getSpec().getRoles().stream()
                .map(ExpectedRole::getName)
                .collect(Collectors.toList()))
                .containsExactly(TestInstallUtils.TEST_DESCRIPTOR_ADMIN_ROLE,
                        TestInstallUtils.TEST_DESCRIPTOR_USER_ROLE);

        assertThat(p.getSpec().getDbms()).isPresent();
        assertThat(p.getSpec().getDbms().get()).isEqualTo(DbmsVendor.POSTGRESQL);
    }
}
