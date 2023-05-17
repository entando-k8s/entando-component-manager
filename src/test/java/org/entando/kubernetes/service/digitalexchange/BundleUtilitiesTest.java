package org.entando.kubernetes.service.digitalexchange;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.entando.kubernetes.service.digitalexchange.BundleUtilities.determineComponentFqImageAddress;
import static org.entando.kubernetes.service.digitalexchange.BundleUtilities.extractImageAddressRegistry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.common.EntandoResourceRequirements;
import org.entando.kubernetes.stubhelper.PluginStubHelper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class BundleUtilitiesTest {

    @Test
    void test_extractImageAddressRegistry() {
        assertNull(extractImageAddressRegistry(null));
        assertNull(extractImageAddressRegistry(""));
        assertNull(extractImageAddressRegistry("/entando/my-plugin"));
        assertNull(extractImageAddressRegistry("entando/my-plugin"));
        assertNull(extractImageAddressRegistry("entando.org/my-plugin"));
        assertEquals(
                "test.my-registry.org",
                extractImageAddressRegistry("test.my-registry.org/entando/my-plugin")
        );
        assertEquals(
                "test.my-registry.org",
                extractImageAddressRegistry("test.my-registry.org/prod/entando/my-plugin")
        );
    }

    @Test
    void test_determineComponentFqImageAddress() {
        // ~ Registry provided
        // SUBCASE 1: bundle fallback available but ignored
        assertEquals(
                "test.my-registry.org/entando/my-plugin",
                determineComponentFqImageAddress(
                        "test.my-registry.org/entando/my-plugin",
                        "prod.my-registry.org/entando/my-bundle",
                        null
                )
        );
        // SUBCASE 2: env var fallback available but ignored
        assertEquals(
                "test.my-registry.org/entando/my-plugin",
                determineComponentFqImageAddress(
                        "test.my-registry.org/entando/my-plugin",
                        "prod.my-registry.org/entando/my-bundle",
                        "prod.my-registry.org"
                )
        );
        // ~ Registry inherited from bundle
        assertEquals(
                "registry.eng-entando.com:5000/entando/simple-ms:0.0.2-SNAPSHOT",
                determineComponentFqImageAddress(
                        "entando/simple-ms:0.0.2-SNAPSHOT",
                        "docker://registry.eng-entando.com:5000/new/entando-simple-ms-bundle",
                        null
                )
        );
        assertEquals(
                "prod.my-registry.org/entando/my-plugin",
                determineComponentFqImageAddress(
                        "entando/my-plugin",
                        "prod.my-registry.org/entando/my-bundle",
                        null
                )
        );
        // ~ Registry inherited from environment variable due to git bundle
        assertEquals(
                "prod.my-registry.org/entando/my-plugin",
                determineComponentFqImageAddress(
                        "entando/my-plugin",
                        "git://github.com/entando/my-bundle",
                        "prod.my-registry.org"
                )
        );
        assertEquals(
                "prod.my-registry.org/entando/my-plugin",
                determineComponentFqImageAddress(
                        "entando/my-plugin",
                        "https://github.com/entando/my-bundle",
                        "prod.my-registry.org"
                )
        );
        // ~ Registry null due to git bundle and no env var fallback
        assertEquals(
                "entando/my-plugin",
                determineComponentFqImageAddress(
                        "entando/my-plugin",
                        "git://github.com/entando/my-bundle",
                        ""
                )
        );
    }

    @Test
    void shouldGenerateResourceRequirementsFromDescriptor() {
        PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV6();
        final EntandoResourceRequirements resourceRequirements =
                BundleUtilities.generateResourceRequirementsFromDescriptor(descriptor);
        assertThat(resourceRequirements.getStorageRequest().get()).isEqualTo(PluginStubHelper.TEST_RES_STORAGE);
        assertThat(resourceRequirements.getMemoryRequest().get()).isEqualTo(PluginStubHelper.TEST_RES_MEMORY);
        assertThat(resourceRequirements.getCpuRequest().get()).isEqualTo(PluginStubHelper.TEST_RES_CPU);
    }

    @Test
    void shouldNotBreakWithNullEntandoResourceRequirements() {
        PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV5();
        final EntandoResourceRequirements resourceRequirements =
                BundleUtilities.generateResourceRequirementsFromDescriptor(descriptor);
        assertThat(resourceRequirements.getStorageRequest().isEmpty()).isTrue();
        assertThat(resourceRequirements.getMemoryRequest().isEmpty()).isTrue();
        assertThat(resourceRequirements.getCpuRequest().isEmpty()).isTrue();
    }
}
