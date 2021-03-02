package org.entando.kubernetes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.entando.kubernetes.TestEntitiesGenerator.getTestBundle;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import java.io.IOException;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import org.entando.kubernetes.config.AppConfiguration;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.DbmsVendor;
import org.entando.kubernetes.model.bundle.BundleType;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPluginSpec;
import org.entando.kubernetes.model.plugin.ExpectedRole;
import org.entando.kubernetes.model.plugin.Permission;
import org.entando.kubernetes.model.plugin.PluginSecurityLevel;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.stubhelper.BundleStubHelper;
import org.entando.kubernetes.stubhelper.PluginStubHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

@Tag("unit")
public class EntandoBundleUtilitiesTest {

    private BundleReader bundleReader;
    private Path bundleFolder;

    @BeforeEach
    public void readNpmPackage() throws IOException {
        bundleFolder = new ClassPathResource("bundle").getFile().toPath();
        bundleReader = new BundleReader(bundleFolder);
    }

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
    @Disabled("Adapt and enable when we will manage this situation")
    void shouldThrowExceptionIfPodDeploymentBaseNameLengthExceeds32Chars() {

        String imageName = PluginStubHelper.TEST_DESCRIPTOR_IMAGE + "abcdefghilmnopqrst";

        PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV2();
        descriptor.setDeploymentBaseName(PluginStubHelper.TEST_DESCRIPTOR_IMAGE + "abcdefghilmnopqrst");

        String expectedMex = String.format(BundleUtilities.DEPLOYMENT_BASE_NAME_MAX_LENGHT_EXCEEDED_ERROR,
                imageName.toLowerCase().replaceAll("[\\/\\.\\:]", "-"),
                BundleUtilities.MAX_ENTANDO_K8S_POD_NAME_LENGTH,
                BundleUtilities.DEPLOYMENT_BASE_NAME_MAX_LENGHT_ERROR_DEPLOYMENT_SUFFIX);

        genericShouldThrowExceptionIfPodDeploymentBaseNameLengthExceeds32Chars(descriptor, expectedMex);
    }


    @Test
    @Disabled("Adapt and enable when we will manage this situation")
    void shouldThrowExceptionIfPodDeploymentBaseNameLengthFromDockerImageExceeds32Chars() {

        String imageName = PluginStubHelper.TEST_DESCRIPTOR_IMAGE + "abcdefghilmnopqrst";

        PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV2();
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
        PluginDescriptor descriptorV2 = PluginStubHelper.stubPluginDescriptorV2();
        descriptorV2.setDeploymentBaseName(deploymentBaseName);
        assertThat(BundleUtilities.extractNameFromDescriptor(descriptorV2)).isEqualTo(deploymentBaseName.toLowerCase());

        // descriptor v2
        PluginDescriptor descriptorV1 = PluginStubHelper.stubPluginDescriptorV1();
        descriptorV1.setDeploymentBaseName(deploymentBaseName);
        assertThat(BundleUtilities.extractNameFromDescriptor(descriptorV1)).isEqualTo(deploymentBaseName.toLowerCase());
    }


    @Test
    void startingFromAValidDockerImageShouldReturnTheK8SCompatibleFormat() {

        // given a valid docker image in a valid descriptor
        String imageName = "organiz/imagename:1.0.0";
        PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV2();
        descriptor.setImage(imageName);
        // without the DeploymentBaseName property
        descriptor.setDeploymentBaseName(null);

        // shoudl return a compatible k8s format of the image
        String expectedImageName = "organiz-imagename";
        assertThat(BundleUtilities.extractNameFromDescriptor(descriptor)).isEqualTo(expectedImageName.toLowerCase());
    }

    @Test
    void withPluginDescriptorV1ShouldCreateACorrectEntandoPlugin() throws IOException {

        // given a plugin descriptor V1
        PluginDescriptor descriptor = bundleReader
                .readDescriptorFile("plugins/todomvcV1.yaml", PluginDescriptor.class);

        // should generate the right populated EntandoPlugin
        EntandoPlugin entandoPlugin = BundleUtilities.generatePluginFromDescriptorV1(descriptor);

        assertOnEntandoPlugin(entandoPlugin, "entando-todomvcv1", DbmsVendor.MYSQL,
                "entando/todomvcV1:1.0.0", "/entando/todomvcv1/1-0-0", "/api/v1/todos",
                getRolesForTodoMvc1(), Collections.emptyList(), this::assertOnLabelsForTodoMvc1,
                PluginSecurityLevel.forName("strict"));
    }

    @Test
    void withPluginDescriptorV1WithImageNameTooLongShouldTruncateAndCreateACorrectEntandoPlugin() throws IOException {

        AppConfiguration.truncatePluginBaseNameIfLonger = true;

        // given a plugin descriptor V1
        PluginDescriptor descriptor = bundleReader
                .readDescriptorFile("plugins/todomvcV1_docker_image_too_long.yaml", PluginDescriptor.class);

        // should generate the right populated EntandoPlugin
        EntandoPlugin entandoPlugin = BundleUtilities.generatePluginFromDescriptorV1(descriptor);

        assertOnEntandoPlugin(entandoPlugin, "entando-helloworld-plugin-v1-nam", DbmsVendor.MYSQL,
                "entando/helloworld-plugin-v1-name-too-looong:1.0.0",
                "/entando/helloworld-plugin-v1-name-too-looong/1-0-0", "/api/v1/todos",
                getRolesForTodoMvc1(), Collections.emptyList(), this::assertOnLabelsForTodoMvc1LongName,
                PluginSecurityLevel.forName("strict"));
    }

    @Test
    void withACompletePluginDescriptorV2ShouldCreateACorrectEntandoPlugin() throws IOException {

        // given a complete plugin descriptor V2
        PluginDescriptor descriptor = bundleReader
                .readDescriptorFile("plugins/todomvcV2_complete.yaml", PluginDescriptor.class);

        // should generate the right populated EntandoPlugin
        EntandoPlugin entandoPlugin = BundleUtilities.generatePluginFromDescriptorV2(descriptor);

        assertOnEntandoPlugin(entandoPlugin, "custombasename", DbmsVendor.MYSQL, "entando/todomvcV2:1.0.0",
                "/myhostname.io/entando-plugin", "/api/v1/todos",
                getRolesForTodoMvc2CompleteBundle(), getPermissionsForTodoMvc2CompleteBundle(),
                this::assertOnLabelsForTodoMvc2, PluginSecurityLevel.forName("lenient"));
    }

    @Test
    void withPluginDescriptorV2ShouldCreateACorrectEntandoPlugin() throws IOException {

        // given a minimum filled plugin descriptor V2
        PluginDescriptor descriptor = bundleReader
                .readDescriptorFile("plugins/todomvcV2.yaml", PluginDescriptor.class);

        // should generate the right populated EntandoPlugin
        EntandoPlugin entandoPlugin = BundleUtilities.generatePluginFromDescriptorV2(descriptor);

        assertOnEntandoPlugin(entandoPlugin, "entando-todomvcv2", DbmsVendor.MYSQL,
                "entando/todomvcV2:1.0.0", "/entando/todomvcv2/1-0-0", "/api/v1/todos",
                Collections.emptyList(), Collections.emptyList(), this::assertOnLabelsForTodoMvc2, null);
    }


    @Test
    void shouldExtractTheBundleTypeIsPresent() {

        EntandoDeBundle entandoDeBundle = BundleStubHelper.stubEntandoDeBundle();
        BundleType bundleType = BundleUtilities.extractBundleTypeFromBundle(entandoDeBundle);
        assertThat(bundleType).isEqualTo(BundleType.SYSTEM_LEVEL_BUNDLE);
    }

    @Test
    void shouldReturnStandardBundleTypeIfTheBundleTypeIsNOTPresentOrVariablesAreNull() {

        EntandoDeBundle entandoDeBundle = new EntandoDeBundle();
        BundleType bundleType = BundleUtilities.extractBundleTypeFromBundle(entandoDeBundle);
        assertThat(bundleType).isEqualTo(BundleType.STANDARD_BUNDLE);

        ObjectMeta metadata = new ObjectMeta();
        entandoDeBundle.setMetadata(metadata);
        bundleType = BundleUtilities.extractBundleTypeFromBundle(entandoDeBundle);
        assertThat(bundleType).isEqualTo(BundleType.STANDARD_BUNDLE);

        metadata.setLabels(Map.of("widgets", "true"));
        bundleType = BundleUtilities.extractBundleTypeFromBundle(entandoDeBundle);
        assertThat(bundleType).isEqualTo(BundleType.STANDARD_BUNDLE);
    }



    private void assertOnEntandoPlugin(EntandoPlugin entandoPlugin, String name, DbmsVendor dbmsVendor, String image,
            String ingressPath, String healthCheckPath, List<ExpectedRole> roleList, List<Permission> permissionList,
            Consumer<Map<String, String>> labelsAssertionFn, PluginSecurityLevel securityLevel) {

        ObjectMeta metadata = entandoPlugin.getMetadata();
        assertThat(metadata.getName()).isEqualTo(name);
        labelsAssertionFn.accept(metadata.getLabels());

        EntandoPluginSpec spec = entandoPlugin.getSpec();
        assertThat(spec.getDbms()).contains(dbmsVendor);
        assertThat(spec.getImage()).isEqualTo(image);
        assertThat(spec.getIngressPath()).isEqualTo(ingressPath);
        assertThat(spec.getHealthCheckPath()).isEqualTo(healthCheckPath);
        if (null != securityLevel) {
            assertThat(spec.getSecurityLevel()).contains(securityLevel);
        } else {
            assertThat(spec.getSecurityLevel()).isEmpty();
        }
        assertOnExpectedRoles(spec.getRoles(), roleList);
        assertOnPermissionsForTodoMvc2CompleteBundle(spec.getPermissions(), permissionList);
        assertOnExpectedRoles(spec.getRoles(), roleList);
    }


    /*****************************************************************************************************
     * ROLES ASSERTIONS.
     ****************************************************************************************************/

    private List<ExpectedRole> getRolesForTodoMvc1() {

        return Arrays.asList(
                new ExpectedRole("theLucas-admin", "theLucas-admin"),
                new ExpectedRole("foo-admin", "foo-admin"));
    }

    private List<ExpectedRole> getRolesForTodoMvc2CompleteBundle() {

        return Arrays.asList(new ExpectedRole("user", "user"), new ExpectedRole("admin", "admin"));
    }

    private void assertOnExpectedRoles(List<ExpectedRole> actual, List<ExpectedRole> expected) {

        assertThat(actual).hasSize(expected.size());
        IntStream.range(0, expected.size())
                .forEach(i -> {
                    assertThat(actual.get(0).getCode()).isEqualTo(expected.get(0).getCode());
                    assertThat(actual.get(0).getName()).isEqualTo(expected.get(0).getName());
                });
    }

    /*****************************************************************************************************
     * PERMISSIONS ASSERTIONS.
     ****************************************************************************************************/


    private List<Permission> getPermissionsForTodoMvc2CompleteBundle() {

        return Arrays.asList(
                new Permission("realm-management", "manage-users"),
                new Permission("realm-management", "view-users"));
    }

    private void assertOnPermissionsForTodoMvc2CompleteBundle(List<Permission> actual, List<Permission> expected) {

        assertThat(actual).hasSize(expected.size());
        IntStream.range(0, expected.size())
                .forEach(i -> {
                    assertThat(actual.get(0).getClientId()).isEqualTo(expected.get(0).getClientId());
                    assertThat(actual.get(0).getRole()).isEqualTo(expected.get(0).getRole());
                });
    }

    /*****************************************************************************************************
     * LABELS ASSERTIONS.
     ****************************************************************************************************/

    private void assertOnLabelsForTodoMvc1(Map<String, String> labelMap) {

        assertOnLabels(
                labelMap,
                new AbstractMap.SimpleEntry<>("organization", "entando"),
                new AbstractMap.SimpleEntry<>("name", "todomvcV1"),
                new AbstractMap.SimpleEntry<>("version", "1.0.0")
        );
    }

    private void assertOnLabelsForTodoMvc1LongName(Map<String, String> labelMap) {

        assertOnLabels(
                labelMap,
                new AbstractMap.SimpleEntry<>("organization", "entando"),
                new AbstractMap.SimpleEntry<>("name", "helloworld-plugin-v1-name-too-looong"),
                new AbstractMap.SimpleEntry<>("version", "1.0.0")
        );
    }

    private void assertOnLabelsForTodoMvc2(Map<String, String> labelMap) {

        assertOnLabels(
                labelMap,
                new AbstractMap.SimpleEntry("organization", "entando"),
                new AbstractMap.SimpleEntry("name", "todomvcV2"),
                new AbstractMap.SimpleEntry("version", "1.0.0")
        );
    }

    private void assertOnLabels(Map<String, String> labelMap, AbstractMap.SimpleEntry... expectedLabels) {

        assertThat(labelMap).containsExactly(expectedLabels);
    }
}
