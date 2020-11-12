package org.entando.kubernetes.assertionhelper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptorV1Role;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptorV1Spec;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginPermission;

public class PluginAssertionHelper {


    public static void assertOnPluginDescriptorsV1(PluginDescriptor actual, PluginDescriptor expected) {
        assertOnPluginDescriptorV1Specs(actual.getSpec(), expected.getSpec());
    }


    public static void assertOnPluginDescriptorsV2(PluginDescriptor actual, PluginDescriptor expected) {

        assertThat(actual.getPermissions())
                .containsExactlyInAnyOrder(expected.getPermissions().toArray(PluginPermission[]::new));
        assertThat(actual.getIngressPath()).isEqualTo(expected.getIngressPath());
        assertThat(actual.getComponentKey().getKey()).isEqualTo(expected.getComponentKey().getKey());
        assertThat(actual.getDbms()).isEqualTo(expected.getDbms());
        assertThat(actual.getDeploymentBaseName()).isEqualTo(expected.getDeploymentBaseName());
        assertThat(actual.getHealthCheckPath()).isEqualTo(expected.getHealthCheckPath());
        assertThat(actual.getImage()).isEqualTo(expected.getImage());
        assertThat(actual.getName()).isEqualTo(expected.getName());
        assertThat(actual.getRoles()).containsExactlyInAnyOrder(expected.getRoles().toArray(String[]::new));
    }


    public static void assertOnPluginDescriptorV1Specs(PluginDescriptorV1Spec actual, PluginDescriptorV1Spec expected) {

        assertThat(actual.getImage()).isEqualTo(expected.getImage());
        assertThat(actual.getHealthCheckPath()).isEqualTo(expected.getHealthCheckPath());
        assertThat(actual.getDbms()).isEqualTo(expected.getDbms());
        assertOnPluginDescriptorV1Roles(actual.getRoles(), expected.getRoles());
    }

    public static void assertOnPluginDescriptorV1Roles(List<PluginDescriptorV1Role> actualList,
            List<PluginDescriptorV1Role> expectedList) {

        assertThat(actualList).hasSize(expectedList.size());

        IntStream.range(0, actualList.size())
                .forEach(i -> {
                    assertThat(actualList.get(i).getCode()).isEqualTo(expectedList.get(i).getCode());
                    assertThat(actualList.get(i).getName()).isEqualTo(expectedList.get(i).getName());
                });
    }
}
