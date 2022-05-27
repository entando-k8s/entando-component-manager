package org.entando.kubernetes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.entando.kubernetes.TestEntitiesGenerator.getTestBundle;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.entando.kubernetes.TestEntitiesGenerator;
import org.entando.kubernetes.config.AppConfiguration;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.EntandoValidationException;
import org.entando.kubernetes.model.DbmsVendor;
import org.entando.kubernetes.model.bundle.BundleType;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptorVersion;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleBuilder;
import org.entando.kubernetes.model.debundle.EntandoDeBundleDetails;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpec;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPluginSpec;
import org.entando.kubernetes.model.plugin.ExpectedRole;
import org.entando.kubernetes.model.plugin.Permission;
import org.entando.kubernetes.model.plugin.PluginSecurityLevel;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.stubhelper.BundleInfoStubHelper;
import org.entando.kubernetes.stubhelper.BundleStubHelper;
import org.entando.kubernetes.stubhelper.PluginStubHelper;
import org.junit.jupiter.api.BeforeEach;
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
        bundleReader = new BundleReader(bundleFolder, BundleStubHelper.stubEntandoDeBundle());
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
    void shouldReturnLatestVersionFromDistTags() {
        EntandoDeBundle testBundle = getTestBundle();
        testBundle.getSpec().getDetails().getVersions().add("v0.0.6");
        String version = BundleUtilities.getBundleVersionOrFail(testBundle, "latest");
        assertThat(version).isEqualTo("0.0.15");
    }

    @Test
    void shouldReturnLatestVersionFromTheAvailableVersionListWithSemVersionRulesIfLatestIsNotExplicitlyDefined() {
        EntandoDeBundle testBundle = getTestBundle();
        testBundle.getSpec().getDetails().getDistTags().remove("latest");
        testBundle.getSpec().getDetails().getVersions().add("v0.0.6");
        String version = BundleUtilities.getBundleVersionOrFail(testBundle, "latest");
        assertThat(version).isEqualTo("v0.0.6");
    }

    @Test
    void shouldThrowExceptionIfNoVersionIsElegibleAsLatest() {
        EntandoDeBundle testBundle = getTestBundle();
        testBundle.getSpec().getDetails().getDistTags().remove("latest");
        testBundle.getSpec().getDetails().getVersions().clear();

        assertThrows(EntandoComponentManagerException.class,
                () -> BundleUtilities.getBundleVersionOrFail(testBundle, "latest"));
    }

    @Test
    void shouldThrowExceptionWithNullOrEmptyVersion() {

        final EntandoDeBundle testBundle = getTestBundle();
        String expectedMex = "Null or empty version property received";

        EntandoComponentManagerException ex = assertThrows(EntandoComponentManagerException.class,
                () -> BundleUtilities.getBundleVersionOrFail(testBundle, ""));
        assertThat(expectedMex).isEqualTo(ex.getMessage());

        ex = assertThrows(EntandoComponentManagerException.class,
                () -> BundleUtilities.getBundleVersionOrFail(testBundle, null));
        assertThat(expectedMex).isEqualTo(ex.getMessage());
    }

    @Test
    public void shouldVerifySemVersion() {
        assertThat(BundleUtilities.isSemanticVersion("v0.0.1")).isTrue();
        assertThat(BundleUtilities.isSemanticVersion("0.1.10-SNAPSHOT")).isTrue();
        assertThat(BundleUtilities.isSemanticVersion("my-great-version")).isFalse();
    }

    @Test
    void withPluginDescriptorV1ShouldCreateACorrectEntandoPlugin() throws IOException {

        // given a plugin descriptor V1
        PluginDescriptor descriptor = bundleReader
                .readDescriptorFile("plugins/todomvcV1.yaml", PluginDescriptor.class);
        descriptor.setDescriptorVersion(PluginDescriptorVersion.V1.getVersion());
        descriptor.setDescriptorMetadata(PluginStubHelper.BUNDLE_ID, PluginStubHelper.EXPECTED_PLUGIN_NAME, "entando-todomvcv1");

        // should generate the right populated EntandoPlugin
        EntandoPlugin entandoPlugin = BundleUtilities.generatePluginFromDescriptorV1(descriptor);

        assertOnEntandoPlugin(entandoPlugin, DbmsVendor.MYSQL,
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
        descriptor.setDescriptorVersion(PluginDescriptorVersion.V1.getVersion());
        descriptor.setDescriptorMetadata("entando", PluginStubHelper.EXPECTED_PLUGIN_NAME, "loooong-entando");

        // should generate the right populated EntandoPlugin
        EntandoPlugin entandoPlugin = BundleUtilities.generatePluginFromDescriptorV1(descriptor);

        assertOnEntandoPlugin(entandoPlugin, DbmsVendor.MYSQL,
                "entando/helloworld-plugin-v1-name-too-looonghelloworld-plugin-v1-name-too-looonghelloworld-plugi"
                        + "n-v1-name-too-looonghelloworld-plugin-v1-name-too-looonghelloworld-plugin-v1-name-too-looong"
                        + "helloworld-plugin-v1-name-too-looonghelloworld-plugin-v1-name-too-looong:1.0.0",
                "/entando/helloworld-plugin-v1-name-too-looonghelloworld-plugin-v1-name-too-looonghelloworl"
                        + "d-plugin-v1-name-too-looonghelloworld-plugin-v1-name-too-looonghelloworld-plugin-v1-name-to"
                        + "o-looonghelloworld-plugin-v1-name-too-looonghelloworld-plugin-v1-name-too-looong/1-0-0",
                "/api/v1/todos",
                getRolesForTodoMvc1(), Collections.emptyList(), this::assertOnLabelsForTodoMvc1LongName,
                PluginSecurityLevel.forName("strict"));
        assertThat(entandoPlugin.getMetadata().getName()).isEqualTo("loooong-entando");

        AppConfiguration.truncatePluginBaseNameIfLonger = false;
    }

    @Test
    void withACompletePluginDescriptorV2ShouldCreateACorrectEntandoPlugin() throws IOException {

        // given a complete plugin descriptor V2
        PluginDescriptor descriptor = bundleReader
                .readDescriptorFile("plugins/todomvcV2_complete.yaml", PluginDescriptor.class);
        descriptor.setDescriptorMetadata("entando", PluginStubHelper.EXPECTED_PLUGIN_NAME, "loooong-entando");

        // should generate the right populated EntandoPlugin
        EntandoPlugin entandoPlugin = BundleUtilities.generatePluginFromDescriptorV2Plus(descriptor);

        assertOnEntandoPlugin(entandoPlugin, DbmsVendor.MYSQL, "entando/todomvcV2:1.0.0",
                "/myhostname.io/entando-plugin", "/api/v1/todos",
                getRolesForTodoMvc2CompleteBundle(), getPermissionsForTodoMvc2PlusCompleteBundle(),
                this::assertOnLabelsForTodoMvc2, PluginSecurityLevel.forName("lenient"));
        assertThat(entandoPlugin.getMetadata().getName()).isEqualTo("loooong-entando");
    }

    @Test
    void withPluginDescriptorV2ShouldCreateACorrectEntandoPlugin() throws IOException {

        // given a minimum filled plugin descriptor V2
        PluginDescriptor descriptor = bundleReader
                .readDescriptorFile("plugins/todomvcV2.yaml", PluginDescriptor.class);
        descriptor.setDescriptorVersion(PluginDescriptorVersion.V2.getVersion());
        descriptor.setDescriptorMetadata(bundleReader.getBundleId(),
                PluginStubHelper.EXPECTED_PLUGIN_NAME,
                "entando-todomvcV2-1-0-0-" + bundleReader.getBundleId());

        // should generate the right populated EntandoPlugin
        EntandoPlugin entandoPlugin = BundleUtilities.generatePluginFromDescriptorV2Plus(descriptor);

        assertOnEntandoPlugin(entandoPlugin, DbmsVendor.MYSQL,
                "entando/todomvcV2:1.0.0", "/entando/todomvcv2/1-0-0", "/api/v1/todos",
                Collections.emptyList(), Collections.emptyList(), this::assertOnLabelsForTodoMvc2, null);
    }

    @Test
    void withAMinimumFilledPluginDescriptorV3ShouldCreateACorrectEntandoPlugin() throws IOException {

        // given a complete plugin descriptor V3
        PluginDescriptor descriptor = bundleReader
                .readDescriptorFile("plugins/todomvcV3.yaml", PluginDescriptor.class);
        descriptor.setDescriptorMetadata(bundleReader.getBundleId(),
                PluginStubHelper.EXPECTED_PLUGIN_NAME,
                "entando-todomvcV2-1-0-0-" + bundleReader.getBundleId());

        // should generate the right populated EntandoPlugin
        EntandoPlugin entandoPlugin = BundleUtilities.generatePluginFromDescriptorV2Plus(descriptor);

        assertOnEntandoPlugin(entandoPlugin, DbmsVendor.MYSQL, "entando/todomvcV2:1.0.0",
                "/entando/todomvcv2", "/api/v1/todos",
                getRolesForTodoMvc2CompleteBundle(), Collections.emptyList(), this::assertOnLabelsForTodoMvc2, null);
    }

    @Test
    void withACompletePluginDescriptorV3ShouldCreateACorrectEntandoPlugin() throws IOException {

        // given a complete plugin descriptor V3
        PluginDescriptor descriptor = bundleReader
                .readDescriptorFile("plugins/todomvcV3_complete.yaml", PluginDescriptor.class);
        descriptor.setDescriptorMetadata(bundleReader.getBundleId(),
                PluginStubHelper.EXPECTED_PLUGIN_NAME,
                "entando-todomvcV3-1-0-0-" + bundleReader.getBundleId());

        // should generate the right populated EntandoPlugin
        EntandoPlugin entandoPlugin = BundleUtilities.generatePluginFromDescriptorV2Plus(descriptor);

        assertOnEntandoPlugin(entandoPlugin, DbmsVendor.MYSQL, "entando/todomvcV3:1.0.0",
                "/myhostname.io/entando-plugin", "/api/v1/todos",
                getRolesForTodoMvc2CompleteBundle(), getPermissionsForTodoMvc2PlusCompleteBundle(),
                this::assertOnLabelsForTodoMvc3, PluginSecurityLevel.LENIENT);
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


    @Test
    void whenReadingARelativeIngressPathItShouldPrefixItWithASlash() throws IOException {

        // given a plugin descriptor V2
        PluginDescriptor descriptor = bundleReader
                .readDescriptorFile("plugins/exampleV2_relative_ingress_path.yaml", PluginDescriptor.class)
                .setDescriptorMetadata(TestEntitiesGenerator.BUNDLE_NAME,
                        PluginStubHelper.EXPECTED_PLUGIN_NAME,
                        "custombasename-" + TestEntitiesGenerator.BUNDLE_NAME);

        // should add the leading slash to the ingress path
        EntandoPlugin entandoPlugin = BundleUtilities.generatePluginFromDescriptorV2Plus(descriptor);

        assertThat(entandoPlugin.getSpec().getIngressPath()).isEqualTo("/myhostname.io/entando-plugin");
    }

    @Test
    void receivingAListOfEnvironmentVariableShouldCorrectlyConvertThemToAListOfEnvVar() {

        final List<EnvVar> envVars = BundleUtilities.assemblePluginEnvVars(PluginStubHelper.stubEnvironmentVariables());

        assertThat(envVars.get(0).getName()).isEqualTo(PluginStubHelper.TEST_ENV_VAR_1_NAME);
        assertThat(envVars.get(0).getValue()).isEqualTo(PluginStubHelper.TEST_ENV_VAR_1_VALUE);
        assertThat(envVars.get(0).getValueFrom()).isNull();

        assertThat(envVars.get(1).getName()).isEqualTo(PluginStubHelper.TEST_ENV_VAR_2_NAME);
        assertThat(envVars.get(1).getValue()).isNull();
        assertThat(envVars.get(1).getValueFrom().getSecretKeyRef().getName()).isEqualTo(
                PluginStubHelper.TEST_ENV_VAR_2_SECRET_NAME);
        assertThat(envVars.get(1).getValueFrom().getSecretKeyRef().getKey()).isEqualTo(
                PluginStubHelper.TEST_ENV_VAR_2_SECRET_KEY);
    }


    private void assertOnEntandoPlugin(EntandoPlugin entandoPlugin, DbmsVendor dbmsVendor, String image,
            String ingressPath, String healthCheckPath, List<ExpectedRole> roleList, List<Permission> permissionList,
            Consumer<Map<String, String>> labelsAssertionFn, PluginSecurityLevel securityLevel) {

        ObjectMeta metadata = entandoPlugin.getMetadata();
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

    @Test
    void shouldReturnAnEmptyOptionalIfInvalidDataAreReceived() {

        EntandoDeBundleDetails deBundleDetails = new EntandoDeBundleDetails();
        List<EntandoDeBundle> notValidBundles = List.of(
                new EntandoDeBundle(),
                new EntandoDeBundleBuilder().withSpec(new EntandoDeBundleSpec(deBundleDetails, null)).build()
        );

        notValidBundles.forEach(entandoDeBundle -> {
            assertThat(BundleUtilities.composeLatestVersionFromDistTags(entandoDeBundle).isEmpty());
        });

    }

    @Test
    void shouldComposeTheExpectedBundleIdentifier() {

        Map<String, String> testCasesMap = Map.of(
                "http://www.github.com/entando/my-bundle.git", "my-bundle.entando.www.github.com",
                "http://github.com/entando/my-bundle.git", "my-bundle.entando.github.com",
                "http://www.github.com/entando/my-bundle", "my-bundle.entando.www.github.com",
                "http://.github.com/entando/my-bundle", "my-bundle.entando.github.com",
                "http://www.github.com/entando/my-bundle.", "my-bundle.entando.www.github.com",
                "http://www.github.com./entando/my-bundle.", "my-bundle.entando.www.github.com",
                "http://www.github.com/entando/.my-bundle", "my-bundle.entando.www.github.com",
                "http://github.com/entando/my-bundle/", "my-bundle.entando.github.com",
                "git@github.com:entando/my-bundle/", "my-bundle.entando.github.com");

        testCasesMap.entrySet()
                .forEach(entry -> {
                    String actual = BundleUtilities.composeBundleIdentifier(entry.getKey());
                    assertThat(actual).isEqualTo(entry.getValue());
                    assertThat(actual.length()).isLessThanOrEqualTo(253);
                });
    }

    @Test
    void shouldThrowEceptionIfBundleIdentifierSizeExceeds253Chars() throws MalformedURLException {
        String url = "http://www.github.com/entando/my-bundlemymy-bundlemymy-bundlemymy-bundlemymy-bundlemymy-bundlemymy-"
                + "bundlemymy-bundlemymy-bundlemymy-bundlemymy-bundlemymy-bundlemymy-bundlemy-bundlemy-bundlemy-"
                + "bundlemy-bundlemy-bundlemy-bundlemy-bundlemy-bundlemy-bundlemy-bundlemy-bundle";

        assertThrows(EntandoValidationException.class, () -> BundleUtilities.composeBundleIdentifier(url));
    }

    @Test
    void shouldReturnEmptyStringWhenTryingToComposeBundleIdentifierWithANullUrl() {
        assertThat(BundleUtilities.composeBundleIdentifier(null)).isEmpty();
    }

    @Test
    void shouldReturnTheReceivedUrlWithoutTheProtocol() {
        String url = BundleUtilities.removeProtocolFromUrl("http://www.github.com/entando/mybundle.git");
        assertThat(url).isEqualTo("www.github.com/entando/mybundle.git");

        url = BundleUtilities.removeProtocolFromUrl("https://www.github.com/entando/mybundle.git");
        assertThat(url).isEqualTo("www.github.com/entando/mybundle.git");
    }

    @Test
    void shouldThrowExceptionWhileReturningTheReceivedUrlWithoutTheProtocolWhenTheUrlIsNotValid() {
        Stream.of("", "ftp://entando.com", "http://", "https://", "https://.com", "http://.com", "https://my-domain-",
                        "https://my-domain.", "http:// ", "http://com.", "http://.com")
                .forEach(urlString -> {
                    try {
                        assertThrows(EntandoValidationException.class,
                                () -> BundleUtilities.removeProtocolFromUrl(urlString));
                    } catch (Exception e) {
                        fail(e.getMessage());
                    }
                });
    }

    @Test
    void shouldSignTheBundleId() {
        final var bundleId = BundleUtilities.signBundleId(
                BundleInfoStubHelper.GIT_REPO_ADDRESS.replace("http://", ""));
        assertThat(bundleId).isEqualTo(BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);
    }


    @Test
    void shouldReplaceGitAndSshProtocolWithHttp() {
        Map<String, String> testCasesMap = Map.of(
                "git@github.com:entando/my_bundle.git", "http://github.com/entando/my_bundle.git",
                "git://github.com/entando/my_bundle.git", "http://github.com/entando/my_bundle.git",
                "ssh://github.com/entando/my_bundle.git", "http://github.com/entando/my_bundle.git",
                "git@github.com:entando:my_bundle.git", "http://github.com/entando:my_bundle.git");

        testCasesMap.forEach((key, value) -> {
            String actual = BundleUtilities.gitSshProtocolToHttp(key);
            Assertions.assertThat(actual).isEqualTo(value);
        });
    }

    @Test
    void shouldReturnTheSameStringWhenProtocolIsNotOneOfTheExpected() {
        List<String> testCasesList = List.of(
                "got@github.com/entando/my_bundle.git",
                "got://github.com/entando/my_bundle.git",
                "sssh://github.com/entando/my_bundle.git",
                "https://github.com/entando/my_bundle.git",
                "ftp://github.com/entando/my_bundle.git");

        testCasesList.forEach(url -> {
            String actual = BundleUtilities.gitSshProtocolToHttp(url);
            Assertions.assertThat(actual).isEqualTo(url);
        });
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


    private List<Permission> getPermissionsForTodoMvc2PlusCompleteBundle() {

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
                new AbstractMap.SimpleEntry<>("name",
                        "helloworld-plugin-v1-name-too-looonghelloworld-plugin-v1-name-too-looonghelloworld-plugin-v1-n"
                                + "ame-too-looonghelloworld-plugin-v1-name-too-looonghelloworld-plugin-v1-name-too-looo"
                                + "nghelloworld-plugin-v1-name-too-looonghelloworld-plugin-v1-name-too-looong"),
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

    private void assertOnLabelsForTodoMvc3(Map<String, String> labelMap) {

        assertOnLabels(
                labelMap,
                new AbstractMap.SimpleEntry("organization", "entando"),
                new AbstractMap.SimpleEntry("name", "todomvcV3"),
                new AbstractMap.SimpleEntry("version", "1.0.0")
        );
    }

    private void assertOnLabels(Map<String, String> labelMap, AbstractMap.SimpleEntry... expectedLabels) {

        assertThat(labelMap).containsExactly(expectedLabels);
    }
}
