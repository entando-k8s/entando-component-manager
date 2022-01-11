package org.entando.kubernetes.model.bundle.descriptor.plugin;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.entando.kubernetes.stubhelper.PluginStubHelper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class PluginDescriptorTest {

    @Test
    void shouldKeepDeploymentFullNameNullEvenInTheAllArgsContructor() { // because it is set inside the PluginProcessor
        PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV2();
        assertNull(descriptor.getFullDeploymentName());
    }
}
