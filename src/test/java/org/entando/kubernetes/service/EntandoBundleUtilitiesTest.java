package org.entando.kubernetes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.entando.kubernetes.TestEntitiesGenerator.getTestBundle;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.entando.kubernetes.config.AppConfiguration;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.EntandoValidationException;
import org.entando.kubernetes.model.bundle.BundleType;
import org.entando.kubernetes.model.bundle.descriptor.DescriptorVersion;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.common.DbmsVendor;
import org.entando.kubernetes.model.common.ExpectedRole;
import org.entando.kubernetes.model.common.Permission;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleBuilder;
import org.entando.kubernetes.model.debundle.EntandoDeBundleDetails;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpec;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPluginSpec;
import org.entando.kubernetes.model.plugin.PluginSecurityLevel;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.stubhelper.BundleInfoStubHelper;
import org.entando.kubernetes.stubhelper.BundleStubHelper;
import org.entando.kubernetes.stubhelper.PluginStubHelper;
import org.entando.kubernetes.stubhelper.WidgetStubHelper;
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
        descriptor.setDescriptorVersion(DescriptorVersion.V1.getVersion());
        descriptor.setDescriptorMetadata(PluginStubHelper.BUNDLE_ID, PluginStubHelper.BUNDLE_CODE,
                PluginStubHelper.TEST_DESCRIPTOR_IMAGE_SHA, PluginStubHelper.EXPECTED_PLUGIN_NAME, "entando-todomvcv1",
                PluginStubHelper.EXPECTED_INGRESS_PATH_V_5,
                PluginStubHelper.EXPECTED_INGRESS_PATH_V_3_OR_V_4);
        descriptor.getDockerImage().setSha256(PluginStubHelper.PLUGIN_IMAGE_SHA);

        // should generate the right populated EntandoPlugin
        EntandoPlugin entandoPlugin = BundleUtilities.generatePluginFromDescriptorV1(descriptor);

        assertOnEntandoPlugin(entandoPlugin, DbmsVendor.MYSQL,
                "entando/todomvcV1@" + PluginStubHelper.PLUGIN_IMAGE_SHA,
                "/entando/todomvcv1/1-0-0", "/api/v1/todos",
                getRolesForTodoMvc1(), Collections.<Permission>emptyList(), this::assertOnLabelsForTodoMvc1,
                PluginSecurityLevel.forName("strict"));
    }

    @Test
    void withPluginDescriptorV1WithImageNameTooLongShouldTruncateAndCreateACorrectEntandoPlugin() throws IOException {

        AppConfiguration.truncatePluginBaseNameIfLonger = true;

        // given a plugin descriptor V1
        PluginDescriptor descriptor = bundleReader
                .readDescriptorFile("plugins/todomvcV1_docker_image_too_long.yaml", PluginDescriptor.class);
        descriptor.setDescriptorVersion(DescriptorVersion.V1.getVersion());
        descriptor.setDescriptorMetadata(PluginStubHelper.BUNDLE_ID, PluginStubHelper.BUNDLE_CODE,
                PluginStubHelper.TEST_DESCRIPTOR_IMAGE_SHA, PluginStubHelper.EXPECTED_PLUGIN_NAME, "loooong-entando",
                PluginStubHelper.EXPECTED_INGRESS_PATH_V_5,
                PluginStubHelper.EXPECTED_INGRESS_PATH_V_3_OR_V_4);
        descriptor.getDockerImage().setSha256(PluginStubHelper.PLUGIN_IMAGE_SHA);

        // should generate the right populated EntandoPlugin
        EntandoPlugin entandoPlugin = BundleUtilities.generatePluginFromDescriptorV1(descriptor);

        assertOnEntandoPlugin(entandoPlugin, DbmsVendor.MYSQL,
                "entando/helloworld-plugin-v1-name-too-looonghelloworld-plugin-v1-name-too-looonghelloworld-plugi"
                        + "n-v1-name-too-looonghelloworld-plugin-v1-name-too-looonghelloworld-plugin-v1-name-too-looong"
                        + "helloworld-plugin-v1-name-too-looonghelloworld-plugin-v1-name-too-looong@"
                        + PluginStubHelper.PLUGIN_IMAGE_SHA,
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
        descriptor.setDescriptorMetadata(PluginStubHelper.BUNDLE_ID, PluginStubHelper.BUNDLE_CODE,
                PluginStubHelper.TEST_DESCRIPTOR_IMAGE_SHA, PluginStubHelper.EXPECTED_PLUGIN_NAME, "loooong-entando",
                PluginStubHelper.EXPECTED_INGRESS_PATH_V_5,
                null);
        descriptor.getDockerImage().setSha256(PluginStubHelper.PLUGIN_IMAGE_SHA);

        // should generate the right populated EntandoPlugin
        EntandoPlugin entandoPlugin = BundleUtilities.generatePluginFromDescriptorV2Plus(descriptor);

        assertOnEntandoPlugin(entandoPlugin, DbmsVendor.MYSQL,
                "entando/todomvcV2@" + PluginStubHelper.PLUGIN_IMAGE_SHA,
                PluginStubHelper.EXPECTED_INGRESS_PATH_V_5, "/api/v1/todos",
                getRolesForTodoMvc2CompleteBundle(), getPermissionsForTodoMvc2PlusCompleteBundle(),
                this::assertOnLabelsForTodoMvc2, PluginSecurityLevel.forName("lenient"));
        assertThat(entandoPlugin.getMetadata().getName()).isEqualTo("loooong-entando");
        assertThat(entandoPlugin.getSpec().getCustomIngressPath()).isNull();
    }

    @Test
    void withPluginDescriptorV2ShouldCreateACorrectEntandoPlugin() throws IOException {

        // given a minimum filled plugin descriptor V2
        PluginDescriptor descriptor = bundleReader
                .readDescriptorFile("plugins/todomvcV2.yaml", PluginDescriptor.class);
        descriptor.setDescriptorVersion(DescriptorVersion.V2.getVersion());
        descriptor.setDescriptorMetadata(PluginStubHelper.BUNDLE_ID, PluginStubHelper.BUNDLE_CODE,
                PluginStubHelper.TEST_DESCRIPTOR_IMAGE_SHA, PluginStubHelper.EXPECTED_PLUGIN_NAME,
                "entando-todomvcV2-1-0-0-" + bundleReader.getDeBundleMetadataName(),
                PluginStubHelper.EXPECTED_INGRESS_PATH_V_5,
                PluginStubHelper.EXPECTED_INGRESS_PATH_V_3_OR_V_4);
        descriptor.getDockerImage().setSha256(PluginStubHelper.PLUGIN_IMAGE_SHA);

        // should generate the right populated EntandoPlugin
        EntandoPlugin entandoPlugin = BundleUtilities.generatePluginFromDescriptorV2Plus(descriptor);

        assertOnEntandoPlugin(entandoPlugin, DbmsVendor.MYSQL,
                "entando/todomvcV2@" + PluginStubHelper.PLUGIN_IMAGE_SHA,
                PluginStubHelper.EXPECTED_INGRESS_PATH_V_5, "/api/v1/todos",
                Collections.<ExpectedRole>emptyList(), Collections.emptyList(), this::assertOnLabelsForTodoMvc2, null);
    }

    @Test
    void withAMinimumFilledPluginDescriptorV3ShouldCreateACorrectEntandoPlugin() throws IOException {

        // given a complete plugin descriptor V3
        PluginDescriptor descriptor = bundleReader
                .readDescriptorFile("plugins/todomvcV3.yaml", PluginDescriptor.class);
        descriptor.setDescriptorMetadata(PluginStubHelper.BUNDLE_ID, PluginStubHelper.BUNDLE_CODE,
                PluginStubHelper.TEST_DESCRIPTOR_IMAGE_SHA, PluginStubHelper.EXPECTED_PLUGIN_NAME,
                "entando-todomvcV2-1-0-0-" + bundleReader.getDeBundleMetadataName(),
                PluginStubHelper.EXPECTED_INGRESS_PATH_V_5,
                PluginStubHelper.EXPECTED_INGRESS_PATH_V_3_OR_V_4);
        descriptor.getDockerImage().setSha256(PluginStubHelper.PLUGIN_IMAGE_SHA);

        // should generate the right populated EntandoPlugin
        EntandoPlugin entandoPlugin = BundleUtilities.generatePluginFromDescriptorV2Plus(descriptor);

        assertOnEntandoPlugin(entandoPlugin, DbmsVendor.MYSQL,
                "entando/todomvcV2@" + PluginStubHelper.PLUGIN_IMAGE_SHA,
                PluginStubHelper.EXPECTED_INGRESS_PATH_V_5, "/api/v1/todos",
                getRolesForTodoMvc2CompleteBundle(), Collections.emptyList(), this::assertOnLabelsForTodoMvc2, null);
    }

    @Test
    void withACompletePluginDescriptorV3ShouldCreateACorrectEntandoPlugin() throws IOException {

        // given a complete plugin descriptor V3
        PluginDescriptor descriptor = bundleReader
                .readDescriptorFile("plugins/todomvcV3_complete.yaml", PluginDescriptor.class);
        descriptor.setDescriptorMetadata(PluginStubHelper.BUNDLE_ID, PluginStubHelper.BUNDLE_CODE,
                PluginStubHelper.TEST_DESCRIPTOR_IMAGE_SHA, PluginStubHelper.EXPECTED_PLUGIN_NAME,
                "entando-todomvcV3-1-0-0-" + bundleReader.getDeBundleMetadataName(),
                PluginStubHelper.EXPECTED_INGRESS_PATH_V_5,
                PluginStubHelper.EXPECTED_INGRESS_PATH_V_3_OR_V_4);
        descriptor.getDockerImage().setSha256(PluginStubHelper.PLUGIN_IMAGE_SHA);

        // should generate the right populated EntandoPlugin
        EntandoPlugin entandoPlugin = BundleUtilities.generatePluginFromDescriptorV2Plus(descriptor);

        assertOnEntandoPlugin(entandoPlugin, DbmsVendor.MYSQL,
                "entando/todomvcV3@" + PluginStubHelper.PLUGIN_IMAGE_SHA,
                PluginStubHelper.EXPECTED_INGRESS_PATH_V_5, "/api/v1/todos",
                getRolesForTodoMvc2CompleteBundle(), getPermissionsForTodoMvc2PlusCompleteBundle(),
                this::assertOnLabelsForTodoMvc3, PluginSecurityLevel.LENIENT);
        assertThat(entandoPlugin.getSpec().getCustomIngressPath()).isEqualTo(
                PluginStubHelper.EXPECTED_INGRESS_PATH_V_3_OR_V_4);
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
    void shouldReturnAnEmptyOptionalIfLatestIsNotValid() {

        Map<String, Object> failingDistTag = Map.of(
                "version", "main",
                "tarball", "docker://registry.hub.docker.com/my-org/my-bundle");
        EntandoDeBundleDetails deBundleDetails = new EntandoDeBundleDetails("my-bundle", "", failingDistTag,
                Collections.emptyList(), Collections.emptyList(), "");

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
        String url =
                "http://www.github.com/entando/my-bundlemymy-bundlemymy-bundlemymy-bundlemymy-bundlemymy-bundlemymy-"
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

        url = BundleUtilities.removeProtocolFromUrl("docker://docker.io/entando/bundle");
        assertThat(url).isEqualTo("docker.io/entando/bundle");

    }

    @Test
    void shouldThrowExceptionWhileReturningTheReceivedUrlWithoutTheProtocolWhenTheUrlIsNotValid() {
        Stream.of("", "ftp://entando.com", "http://", "https://", "https://.com", "http://.com", "https://my-domain-",
                        "https://my-domain.", "http:// ", "http://com.", "http://.com", "docker://-test")
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
        final var bundleId = BundleUtilities.getBundleId(
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

    @Test
    void shouldReturnTheExpectedIngressPathWithPluginDescriptorV4() {
        final PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV4();
        final String ingressPath = BundleUtilities.extractIngressPathFromDescriptor(descriptor, "code");
        assertThat(ingressPath).isEqualTo(PluginStubHelper.EXPECTED_INGRESS_PATH_V_3_OR_V_4);
    }

    @Test
    void shouldReturnTheExpectedIngressPathWithPluginDescriptorV5() {
        final PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV5()
                .setName("MyStrange___plugin.Name");
        final String ingressPath = BundleUtilities.extractIngressPathFromDescriptor(descriptor,
                PluginStubHelper.BUNDLE_CODE);
        assertThat(ingressPath).isEqualTo("/" + PluginStubHelper.BUNDLE_CODE + "/mystrange---plugin-name");
    }

    @Test
    void shouldIgnoreCustomIngressPathWithPluginDescriptorV5() {
        final PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV5()
                .setIngressPath("mycustomingress");
        final String ingressPath = BundleUtilities.extractIngressPathFromDescriptor(descriptor,
                PluginStubHelper.BUNDLE_CODE);
        assertThat(ingressPath).isEqualTo(PluginStubHelper.EXPECTED_INGRESS_PATH_V_5);
    }

    @Test
    void shouldComposeTheExpectedCodeWithDescriptorVersionMinorThan5AndCodeWithoutTheHash() {
        final WidgetDescriptor widgetDescriptor = WidgetStubHelper.stubWidgetDescriptorV1();
        widgetDescriptor.setDescriptorVersion(DescriptorVersion.V4.getVersion());

        final String code = BundleUtilities.composeDescriptorCode(widgetDescriptor.getCode(),
                widgetDescriptor.getName(),
                widgetDescriptor, BundleInfoStubHelper.GIT_REPO_ADDRESS);

        assertThat(code).isEqualTo(WidgetStubHelper.WIDGET_1_CODE + "-"
                + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);
    }

    @Test
    void shouldComposeTheExpectedCodeWithDescriptorVersionMinorThan5AndCodeWithTheCorrectHash() {

        WidgetDescriptor widgetDescriptor = WidgetStubHelper.stubWidgetDescriptorV1();
        widgetDescriptor.setCode(WidgetStubHelper.WIDGET_1_CODE + "-"
                + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);
        widgetDescriptor.setDescriptorVersion(DescriptorVersion.V4.getVersion());

        final String code = BundleUtilities.composeDescriptorCode(widgetDescriptor.getCode(),
                widgetDescriptor.getName(),
                widgetDescriptor, BundleInfoStubHelper.GIT_REPO_ADDRESS);

        assertThat(code).isEqualTo(WidgetStubHelper.WIDGET_1_CODE + "-"
                + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);
    }

    @Test
    void shouldComposeTheExpectedCodeWithDescriptorVersionMinorThan5AndCodeWithAnIncorrectHash() {
        String wrongHash = "abcd1234";

        WidgetDescriptor widgetDescriptor = WidgetStubHelper.stubWidgetDescriptorV1();
        widgetDescriptor.setCode(WidgetStubHelper.WIDGET_1_CODE + "-" + wrongHash);
        widgetDescriptor.setDescriptorVersion(DescriptorVersion.V4.getVersion());

        final String code = BundleUtilities.composeDescriptorCode(widgetDescriptor.getCode(),
                widgetDescriptor.getName(),
                widgetDescriptor, BundleInfoStubHelper.GIT_REPO_ADDRESS);

        assertThat(code).isEqualTo(WidgetStubHelper.WIDGET_1_CODE + "-" + wrongHash
                + "-" + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);
    }

    @Test
    void shouldSetTheExpectedWidgetCodeWithWidgetDescriptorV5() {

        WidgetDescriptor widgetDescriptor = WidgetStubHelper.stubWidgetDescriptorV5();

        final String code = BundleUtilities.composeDescriptorCode(widgetDescriptor.getCode(),
                widgetDescriptor.getName(),
                widgetDescriptor, BundleInfoStubHelper.GIT_REPO_ADDRESS);

        assertThat(code).isEqualTo(WidgetStubHelper.WIDGET_1_NAME
                + "-" + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);
    }

    @Test
    void shouldDecodeAndvalidateUrl() {

        assertThrows(IllegalArgumentException.class, () -> BundleUtilities.decodeUrl(null));

        final String urlOk = "docker://docker.io/enatando/repository";
        final String encodedUrlOk = Base64.getEncoder().encodeToString(urlOk.getBytes(StandardCharsets.UTF_8));

        assertThat(BundleUtilities.decodeUrl(encodedUrlOk)).isEqualTo(urlOk);

        final String urlKo = "-docker.io/enatando/repository";
        final String encodedUrlKo = Base64.getEncoder().encodeToString(urlKo.getBytes(StandardCharsets.UTF_8));

        assertThrows(EntandoValidationException.class, () -> BundleUtilities.decodeUrl(encodedUrlKo));

    }

    @Test
    void shouldComposeTheExpectedIngressPathFromDockerImage() {

        // given a plugin descriptor V2
        PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV2();
        // when the ingress path is composed starting by the docker image
        String ingressPath = BundleUtilities.composeIngressPathForV1(descriptor);
        // then the expected ingress path is generated
        assertThat(ingressPath).isEqualTo("/entando/the-lucas");
    }

    @Test
    void shouldExtractNameFromEntityCode() {
        assertThrows(EntandoComponentManagerException.class, () -> BundleUtilities.extractNameFromEntityCode(null));
        assertThrows(EntandoComponentManagerException.class, () -> BundleUtilities.extractNameFromEntityCode(""));
        assertThrows(EntandoComponentManagerException.class,
                () -> BundleUtilities.extractNameFromEntityCode("code1234abcd"));

        final String name = BundleUtilities.extractNameFromEntityCode(BundleStubHelper.BUNDLE_CODE);
        assertThat(name).isEqualTo(BundleStubHelper.BUNDLE_NAME);
    }

    @Test
    void shouldExtractIdFromEntityCode() {
        assertThrows(EntandoComponentManagerException.class, () -> BundleUtilities.extractIdFromEntityCode(null));
        assertThrows(EntandoComponentManagerException.class, () -> BundleUtilities.extractIdFromEntityCode(""));
        assertThrows(EntandoComponentManagerException.class,
                () -> BundleUtilities.extractIdFromEntityCode("code1234abcd"));

        final String id = BundleUtilities.extractIdFromEntityCode(BundleStubHelper.BUNDLE_CODE);
        assertThat(id).isEqualTo(BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);
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
