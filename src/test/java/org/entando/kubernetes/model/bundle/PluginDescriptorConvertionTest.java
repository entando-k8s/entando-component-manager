package org.entando.kubernetes.model.bundle;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.entando.kubernetes.model.DbmsVendor;
import org.entando.kubernetes.model.bundle.descriptor.DockerImage;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptorVersion;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.ExpectedRole;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.stubhelper.PluginStubHelper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class PluginDescriptorConvertionTest {

    @Test
    void shouldGenerateCorrectDockerImageFromVersion1() {
        PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV1();
        DockerImage dockerImage = descriptor.getDockerImage();
        assertThat(dockerImage.getName()).isEqualTo("the-lucas");
        assertThat(dockerImage.getOrganization()).isEqualTo("entando");
        assertThat(dockerImage.getVersion()).isEqualTo("0.0.1-SNAPSHOT");
    }

    @Test
    void shouldGenerateCorrectDockerImageFromVersionMajorThan1() {

        Stream.of(PluginStubHelper.stubPluginDescriptorV2(), PluginStubHelper.stubPluginDescriptorV3())
                .forEach(pluginDescriptor -> {
                    DockerImage dockerImage = pluginDescriptor.getDockerImage();
                    assertThat(dockerImage.getName()).isEqualTo("the-lucas");
                    assertThat(dockerImage.getOrganization()).isEqualTo("entando");
                    assertThat(dockerImage.getVersion()).isEqualTo("0.0.1-SNAPSHOT");
                });
    }


    @Test
    void shouldGenerateKubernetesCompatibleIngressPathFromDescriptorVersionMinorThan3() {
        Stream.of((PluginDescriptor) PluginStubHelper.stubPluginDescriptorV1()
                                .setDescriptorVersion(PluginDescriptorVersion.V1.getVersion()),
                  (PluginDescriptor) PluginStubHelper.stubPluginDescriptorV2()
                                .setDescriptorVersion(PluginDescriptorVersion.V2.getVersion()))
                .forEach(pluginDescriptor -> {
                    String ingress = BundleUtilities.extractIngressPathFromDescriptor(pluginDescriptor);
                    assertThat(ingress).isEqualTo(PluginStubHelper.EXPECTED_INGRESS_PATH_V_MINOR_THAN_3);
                });
    }

    @Test
    void shouldGenerateKubernetesCompatibleIngressPathFromDescriptorVersion3() {
        PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV3();
        String name = BundleUtilities.extractIngressPathFromDescriptor(descriptor);
        assertThat(name).isEqualTo(PluginStubHelper.EXPECTED_INGRESS_PATH_V_EQUAL_OR_MAJOR_THAN_3);
    }

    @Test
    void shouldConvertDescriptorToEntandoPluginVersionMajorThan1() {
        PluginDescriptor d = (PluginDescriptor) PluginStubHelper.stubPluginDescriptorV2()
                .setDescriptorMetadata(PluginStubHelper.BUNDLE_ID,
                        PluginStubHelper.EXPECTED_PLUGIN_NAME,
                        PluginStubHelper.EXPECTED_PLUGIN_NAME_FROM_DEP_BASE_NAME)
                .setDescriptorVersion(PluginDescriptorVersion.V2.getVersion());
        EntandoPlugin p = BundleUtilities.generatePluginFromDescriptor(d);

        assertOnConvertedEntandoPlugin(p, PluginStubHelper.EXPECTED_PLUGIN_NAME_FROM_DEP_BASE_NAME);
    }


    @Test
    void shouldConvertDescriptorToEntandoPluginVersion1() {
        PluginDescriptor d = (PluginDescriptor) PluginStubHelper.stubPluginDescriptorV1()
                .setDescriptorMetadata(PluginStubHelper.BUNDLE_ID,
                        PluginStubHelper.EXPECTED_PLUGIN_NAME,
                        PluginStubHelper.EXPECTED_PLUGIN_NAME)
                .setDescriptorVersion(PluginDescriptorVersion.V2.getVersion());
        EntandoPlugin p = BundleUtilities.generatePluginFromDescriptor(d);

        assertOnConvertedEntandoPlugin(p, PluginStubHelper.EXPECTED_PLUGIN_NAME);
    }

    private void assertOnConvertedEntandoPlugin(EntandoPlugin p, String expectedPluginName) {
        assertThat(p.getMetadata().getName()).isEqualTo(expectedPluginName);
        assertThat(p.getSpec().getImage()).isEqualTo(PluginStubHelper.TEST_DESCRIPTOR_IMAGE);
        assertThat(p.getSpec().getIngressPath()).isEqualTo(PluginStubHelper.EXPECTED_INGRESS_PATH_V_MINOR_THAN_3);
        assertThat(p.getSpec().getHealthCheckPath()).isEqualTo(PluginStubHelper.TEST_DESCRIPTOR_HEALTH_PATH);

        Map<String, String> lbls = new HashMap<>();
        lbls.put("organization", "entando");
        lbls.put("name", "the-lucas");
        lbls.put("version", "0.0.1-SNAPSHOT");
        assertThat(p.getMetadata().getLabels()).containsAllEntriesOf(lbls);

        List<ExpectedRole> expectedRoles = p.getSpec().getRoles();
        List<String> expectedRolesCodes = Arrays
                .asList(PluginStubHelper.TEST_DESCRIPTOR_ADMIN_ROLE, PluginStubHelper.TEST_DESCRIPTOR_USER_ROLE);
        assertThat(expectedRoles).hasSize(2);
        assertThat(expectedRoles).allMatch(role ->
                expectedRolesCodes.contains(role.getCode()) && role.getCode().equals(role.getName()));
        assertThat(p.getSpec().getRoles().stream()
                .map(ExpectedRole::getCode)
                .collect(Collectors.toList()))
                .containsExactly(PluginStubHelper.TEST_DESCRIPTOR_ADMIN_ROLE,
                        PluginStubHelper.TEST_DESCRIPTOR_USER_ROLE);
        assertThat(p.getSpec().getRoles().stream()
                .map(ExpectedRole::getName)
                .collect(Collectors.toList()))
                .containsExactly(PluginStubHelper.TEST_DESCRIPTOR_ADMIN_ROLE,
                        PluginStubHelper.TEST_DESCRIPTOR_USER_ROLE);

        assertThat(p.getSpec().getDbms()).isPresent();
        assertThat(p.getSpec().getDbms().get()).isEqualTo(DbmsVendor.POSTGRESQL);
    }
}
