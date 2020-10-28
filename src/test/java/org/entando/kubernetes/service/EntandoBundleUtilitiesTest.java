package org.entando.kubernetes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.entando.kubernetes.TestEntitiesGenerator.getTestBundle;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.utils.TestInstallUtils;
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

        EntandoDeBundle testBundle = getTestBundle();

        Exception ex = assertThrows(EntandoComponentManagerException.class,
                () -> BundleUtilities.getBundleVersionOrFail(testBundle, "first"));

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
    void shouldThrowExceptionIfPodDeploymentBaseNameLengthExceeds32Chars() {

        String imageName = TestInstallUtils.TEST_DESCRIPTOR_IMAGE + "abcdefghilmnopqrst";

        PluginDescriptor descriptor = TestInstallUtils.getTestDescriptor();
        descriptor.setDeploymentBaseName(TestInstallUtils.TEST_DESCRIPTOR_IMAGE + "abcdefghilmnopqrst");

        String expectedMex = String.format(BundleUtilities.DEPLOYMENT_BASE_NAME_MAX_LENGHT_EXCEEDED_ERROR,
                imageName.toLowerCase().replaceAll("[\\/\\.\\:]", "-"),
                BundleUtilities.MAX_ENTANDO_K8S_POD_NAME_LENGTH,
                BundleUtilities.DEPLOYMENT_BASE_NAME_MAX_LENGHT_ERROR_DEPLOYMENT_SUFFIX);

        genericShouldThrowExceptionIfPodDeploymentBaseNameLengthExceeds32Chars(descriptor, expectedMex);
    }


    @Test
    void shouldThrowExceptionIfPodDeploymentBaseNameLengthFromDockerImageExceeds32Chars() {

        String imageName = TestInstallUtils.TEST_DESCRIPTOR_IMAGE + "abcdefghilmnopqrst";

        PluginDescriptor descriptor = TestInstallUtils.getTestDescriptor();
        descriptor.setImage(imageName);
        descriptor.setDeploymentBaseName(null);

        String expectedMex = String.format(BundleUtilities.DEPLOYMENT_BASE_NAME_MAX_LENGHT_EXCEEDED_ERROR,
                imageName.toLowerCase().replaceAll("[\\/\\.\\:]", "-"),
                BundleUtilities.MAX_ENTANDO_K8S_POD_NAME_LENGTH,
                BundleUtilities.DEPLOYMENT_BASE_NAME_MAX_LENGHT_ERROR_DOCKER_IMAGE_SUFFIX);

        genericShouldThrowExceptionIfPodDeploymentBaseNameLengthExceeds32Chars(descriptor, expectedMex);
    }


    private void genericShouldThrowExceptionIfPodDeploymentBaseNameLengthExceeds32Chars(PluginDescriptor descriptor,
            String expectedMex) {

        EntandoComponentManagerException exception = assertThrows(
                EntandoComponentManagerException.class,
                () -> BundleUtilities.extractNameFromDescriptor(descriptor),
                "Expected extractNameFromDescriptor() to throw, but it didn't"
        );

        assertThat(exception.getMessage()).isEqualTo(expectedMex);
    }


    @Test
    void ifPresentShouldUseDeploymentBaseNameOverDockerImage() {

        String deploymentBaseName = "testDeploymentName";

        // descriptor v2
        PluginDescriptor descriptorV2 = TestInstallUtils.getTestDescriptor();
        descriptorV2.setDeploymentBaseName(deploymentBaseName);
        assertThat(BundleUtilities.extractNameFromDescriptor(descriptorV2)).isEqualTo(deploymentBaseName.toLowerCase());

        // descriptor v2
        PluginDescriptor descriptorV1 = TestInstallUtils.getTestDescriptorVersion1();
        descriptorV1.setDeploymentBaseName(deploymentBaseName);
        assertThat(BundleUtilities.extractNameFromDescriptor(descriptorV1)).isEqualTo(deploymentBaseName.toLowerCase());
    }
}