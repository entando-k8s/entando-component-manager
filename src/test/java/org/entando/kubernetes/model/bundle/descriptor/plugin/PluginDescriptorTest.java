package org.entando.kubernetes.model.bundle.descriptor.plugin;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import org.entando.kubernetes.stubhelper.PluginStubHelper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class PluginDescriptorTest {

    @Test
    void shouldKeepDeploymentFullNameNullEvenInTheAllArgsContructor() { // because it is set inside the PluginProcessor
        PluginDescriptor descriptor = PluginDescriptor.builder()
                .image("image")
                .roles(Arrays.asList("role1", "role2"))
                .healthCheckPath("healtcheak")
                .dbms("mysql")
                .deploymentBaseName("deplBaseName")
                .build();
        assertNull(descriptor.getDescriptorMetadata());
    }

    @Test
    void testV5_shouldKeepDeploymentFullNameNullEvenInTheAllArgsContructor() { // because it is set inside the
        // PluginProcessor
        PluginDescriptor descriptor = PluginDescriptor.builder()
                .image("image")
                .roles(Arrays.asList("role1", "role2"))
                .healthCheckPath("healtcheak")
                .dbms("mysql")
                .name("test-name")
                .build();
        assertNull(descriptor.getDescriptorMetadata());
    }
}
