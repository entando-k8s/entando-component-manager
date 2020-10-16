package org.entando.kubernetes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.entando.kubernetes.TestEntitiesGenerator.getTestBundle;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.descriptor.DockerImage;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.utils.TestInstallUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class EntandoBundleUtilitiesTest {

    private final String imgOrganization = "1234567890";
    private final String imgName = "12345678901234567890";
    private final String imgVersion = "1.0.0";


    @Test
    public void shouldReturnVersionDirectly() {
        String version = BundleUtilities.getBundleVersionOrFail(getTestBundle(), "1.0.0");
        assertThat(version).isEqualTo("1.0.0");
    }

    @Test
    public void shouldThrowAnErrorAsVersionIsNotDefined() {
        Exception ex = assertThrows(RuntimeException.class, () -> {
            BundleUtilities.getBundleVersionOrFail(getTestBundle(), "first");
        });

        assertThat(ex.getMessage()).isEqualTo("Invalid version 'first' for bundle 'my-bundle'");
    }

    @Test
    public void shouldReturnLatestVersion() {
        String version = BundleUtilities.getBundleVersionOrFail(getTestBundle(), "latest");
        assertThat(version).isEqualTo("0.0.1");
    }

    @Test
    public void shouldVerifySemVersion() {
        assertThat(BundleUtilities.isSemanticVersion("v0.0.1")).isTrue();
        assertThat(BundleUtilities.isSemanticVersion("0.1.10-SNAPSHOT")).isTrue();
        assertThat(BundleUtilities.isSemanticVersion("my-great-version")).isFalse();
    }


    @Test
    void shouldThrowExceptionIfPodPrefixNameLengthExceeds32Chars() {

        String imageName = TestInstallUtils.TEST_DESCRIPTOR_IMAGE + "abcdefghilmnopqrst";

        PluginDescriptor descriptor = TestInstallUtils.getTestDescriptor();
        descriptor.setImage(imageName);

        EntandoComponentManagerException exception = assertThrows(
                EntandoComponentManagerException.class,
                () -> BundleUtilities.extractNameFromDescriptor(descriptor),
                "Expected extractNameFromDescriptor() to throw, but it didn't"
        );

        String expectedMex = String.format("The prefix \"%s\" of the pod that is about to be created is longer than %d. The prefix is "
                + "created using this format: "
                + "[docker-organization]-[docker-image-name]-[docker-image-version]",
                imageName.toLowerCase().replaceAll("[\\/\\.\\:]", "-"),
                BundleUtilities.MAX_ENTANDO_K8S_POD_NAME_LENGTH);

        assertThat(exception.getMessage()).isEqualTo(expectedMex);
    }
}