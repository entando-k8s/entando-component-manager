package org.entando.kubernetes.validator.descriptor;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.entando.kubernetes.validator.descriptor.PluginDescriptorValidator.HEALTHCHECK_INGRESS_TYPE_CANONICAL;
import static org.entando.kubernetes.validator.descriptor.PluginDescriptorValidator.HEALTHCHECK_INGRESS_TYPE_CUSTOM;
import static org.entando.kubernetes.validator.descriptor.PluginDescriptorValidator.HEALTHCHECK_INGRESS_TYPE_DEFAULT;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.collections4.KeyValue;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;
import org.apache.commons.lang.StringUtils;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.model.bundle.descriptor.DescriptorVersion;
import org.entando.kubernetes.model.bundle.descriptor.plugin.EnvironmentVariable;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.plugin.SecretKeyRef;
import org.entando.kubernetes.model.bundle.descriptor.plugin.ValueFrom;
import org.entando.kubernetes.stubhelper.PluginStubHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class PluginDescriptorValidatorTest {

    private PluginDescriptorValidator validator;
    private final YAMLMapper yamlMapper = new YAMLMapper();

    @BeforeEach
    public void genericSetup() {
        validator = new PluginDescriptorValidator(200);
        validator.setupValidatorConfiguration();
    }

    @Test
    void shouldCorrectlyValidatePluginDescriptorV1() throws IOException {
        PluginDescriptor descriptor = yamlMapper
                .readValue(new File("src/test/resources/bundle/plugins/todomvcV1.yaml"),
                        PluginDescriptor.class);
        descriptor.setDescriptorMetadata(PluginStubHelper.BUNDLE_ID, PluginStubHelper.BUNDLE_CODE,
                PluginStubHelper.TEST_DESCRIPTOR_IMAGE_SHA,
                PluginStubHelper.EXPECTED_PLUGIN_NAME, "entando-todomvcV1-1-0-0", "endpoint", "custIngr");
        validator.validateOrThrow(descriptor);
    }

    @Test
    void shouldCorrectlyValidatePluginDescriptorV2() throws IOException {
        PluginDescriptor descriptor = yamlMapper
                .readValue(new File("src/test/resources/bundle/plugins/todomvcV2.yaml"),
                        PluginDescriptor.class);
        descriptor.setDescriptorMetadata(PluginStubHelper.BUNDLE_ID, PluginStubHelper.BUNDLE_CODE,
                PluginStubHelper.TEST_DESCRIPTOR_IMAGE_SHA,
                PluginStubHelper.EXPECTED_PLUGIN_NAME, "entando-todomvcV2-1-0-0", "endpoint", "custIngr");
        validator.validateOrThrow(descriptor);

        descriptor = yamlMapper
                .readValue(new File("src/test/resources/bundle/plugins/todomvcV2_complete.yaml"),
                        PluginDescriptor.class);
        descriptor.setDescriptorMetadata(PluginStubHelper.BUNDLE_ID, PluginStubHelper.BUNDLE_CODE,
                PluginStubHelper.TEST_DESCRIPTOR_IMAGE_SHA,
                PluginStubHelper.EXPECTED_PLUGIN_NAME, "customBaseName", "endpoint", "custIngr");
        validator.validateOrThrow(descriptor);
    }

    @Test
    void shouldCorrectlyValidatePluginDescriptorV3() throws IOException {
        PluginDescriptor descriptor = yamlMapper
                .readValue(new File("src/test/resources/bundle/plugins/todomvcV3.yaml"),
                        PluginDescriptor.class);
        descriptor.setDescriptorMetadata(PluginStubHelper.BUNDLE_ID, PluginStubHelper.BUNDLE_CODE,
                PluginStubHelper.TEST_DESCRIPTOR_IMAGE_SHA,
                PluginStubHelper.EXPECTED_PLUGIN_NAME, "entando-todomvcV1-1-0-0", "endpoint", "custIngr");
        validator.validateOrThrow(descriptor);

        descriptor = yamlMapper
                .readValue(new File("src/test/resources/bundle/plugins/todomvcV3_complete.yaml"),
                        PluginDescriptor.class);
        descriptor.setDescriptorMetadata(PluginStubHelper.BUNDLE_ID, PluginStubHelper.BUNDLE_CODE,
                PluginStubHelper.TEST_DESCRIPTOR_IMAGE_SHA,
                PluginStubHelper.EXPECTED_PLUGIN_NAME, "customBaseName", "endpoint", "custIngr");
        validator.validateOrThrow(descriptor);
    }

    @Test
    void shouldThrowExceptionWhenDescriptorVersionNotRecognized() throws Exception {

        PluginDescriptor descriptor = yamlMapper
                .readValue(new File("src/test/resources/bundle/plugins/todomvcV2.yaml"),
                        PluginDescriptor.class)
                .setDescriptorMetadata(PluginStubHelper.BUNDLE_ID, PluginStubHelper.BUNDLE_CODE,
                        PluginStubHelper.TEST_DESCRIPTOR_IMAGE_SHA,
                        PluginStubHelper.EXPECTED_PLUGIN_NAME, "entando-todomvcV2-1-0-0", "endpoint", "custIngr");
        descriptor.setDescriptorVersion("v2.5");

        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));
    }

    @Test
    void shouldThrowExceptionIfDescriptorVersionIsNOTCompatibleWithTheDataFormat() throws IOException {

        List<KeyValue<String, DescriptorVersion>> descriptorVersionToSetList = Arrays.asList(
                new DefaultKeyValue<>("todomvcV1.yaml", DescriptorVersion.V2),
                new DefaultKeyValue<>("todomvcV2.yaml", DescriptorVersion.V1),
                new DefaultKeyValue<>("todomvcV3_complete.yaml", DescriptorVersion.V1),
                new DefaultKeyValue<>("todomvcV4_complete.yaml", DescriptorVersion.V3));

        for (KeyValue<String, DescriptorVersion> keyValue : descriptorVersionToSetList) {

            PluginDescriptor descriptor = yamlMapper
                    .readValue(new File("src/test/resources/bundle/plugins/" + keyValue.getKey()),
                            PluginDescriptor.class)
                    .setDescriptorMetadata(PluginStubHelper.stubDescriptorMetadata());
            descriptor.setDescriptorVersion(keyValue.getValue().getVersion());

            assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));
        }
    }

    @Test
    void shouldThrowExceptionWhenPluginSecurityLevelIsUnknown() {

        // plugin descriptor V1
        PluginDescriptor descriptorV1 = PluginStubHelper.stubPluginDescriptorV1();
        descriptorV1.getSpec().setSecurityLevel("unknown");

        // plugin descriptor V2
        PluginDescriptor descriptorV2 = PluginStubHelper.stubPluginDescriptorV2();
        descriptorV2.setSecurityLevel("unknown");

        List<PluginDescriptor> pluginDescriptorList = Arrays.asList(descriptorV1, descriptorV2);

        for (PluginDescriptor pluginDescriptor : pluginDescriptorList) {
            assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(pluginDescriptor));
        }
    }

    @Test
    void shouldThrowExceptionIfEnvironmentVariablesAreNOTValid() {

        PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV4WithEnvVars();

        EnvironmentVariable varWithValue = new EnvironmentVariable(
                PluginStubHelper.TEST_ENV_VAR_1_NAME,
                PluginStubHelper.TEST_ENV_VAR_1_VALUE,
                null);

        EnvironmentVariable varWithRef = new EnvironmentVariable(
                PluginStubHelper.TEST_ENV_VAR_2_NAME,
                null,
                new ValueFrom().setSecretKeyRef(
                        new SecretKeyRef(PluginStubHelper.BUNDLE_ID + "-"
                                + PluginStubHelper.EXPECTED_PLUGIN_NAME_FROM_DEP_BASE_NAME,
                                PluginStubHelper.TEST_ENV_VAR_2_SECRET_KEY)));

        descriptor.setEnvironmentVariables(Arrays.asList(varWithValue, varWithRef));
        descriptor.setDescriptorMetadata(PluginStubHelper.BUNDLE_ID,
                PluginStubHelper.BUNDLE_CODE,
                PluginStubHelper.TEST_DESCRIPTOR_IMAGE_SHA,
                PluginStubHelper.EXPECTED_PLUGIN_NAME,
                PluginStubHelper.EXPECTED_PLUGIN_NAME_FROM_DEP_BASE_NAME,
                PluginStubHelper.EXPECTED_INGRESS_PATH_V_5,
                PluginStubHelper.EXPECTED_INGRESS_PATH_V_3_OR_V_4);
        assertThat(validator.validateOrThrow(descriptor)).isTrue();

        // with empty name should fail
        descriptor.getEnvironmentVariables().get(0).setName("");
        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));

        // with null name should fail
        descriptor.getEnvironmentVariables().get(0).setName(null);
        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));

        descriptor.getEnvironmentVariables().get(0).setName(PluginStubHelper.TEST_ENV_VAR_1_NAME);

        descriptor.getEnvironmentVariables().get(1).safeGetValueFrom().getSecretKeyRef()
                .setKey(PluginStubHelper.TEST_ENV_VAR_2_SECRET_KEY);

        // violating RFC 1123
        Stream.of("ENV", "env_1", "-env-1", "env-1-", "env?1", StringUtils.repeat("a", 254))
                .forEach(name -> {
                    descriptor.getEnvironmentVariables().get(1).safeGetValueFrom().getSecretKeyRef().setName(name);
                    assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));
                });
    }

    @Test
    void shouldAllowEmptyOrNullValuesForEnvironmentVariables() {

        PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV4WithEnvVars();

        // with empty value and null secret
        descriptor.getEnvironmentVariables().get(0).setValue("");
        assertThat(validator.validateOrThrow(descriptor)).isTrue();

        // with null value and null secret
        descriptor.getEnvironmentVariables().get(0).setValue(null);
        assertThat(validator.validateOrThrow(descriptor)).isTrue();

        descriptor.getEnvironmentVariables().get(0).setValue(PluginStubHelper.TEST_ENV_VAR_1_VALUE);

        // with empty value and empty secretKeyRefName key
        descriptor.getEnvironmentVariables().get(1).safeGetValueFrom().getSecretKeyRef().setKey("");
        assertThat(validator.validateOrThrow(descriptor)).isTrue();

        // with null value and null  secretKeyRefName key
        descriptor.getEnvironmentVariables().get(1).safeGetValueFrom().getSecretKeyRef().setKey(null);
        assertThat(validator.validateOrThrow(descriptor)).isTrue();

        descriptor.getEnvironmentVariables().get(1).safeGetValueFrom().getSecretKeyRef()
                .setKey(PluginStubHelper.TEST_ENV_VAR_2_SECRET_KEY);

        // with empty value and empty secretKeyRefName name
        descriptor.getEnvironmentVariables().get(1).safeGetValueFrom().getSecretKeyRef().setName("");
        assertThat(validator.validateOrThrow(descriptor)).isTrue();

        // with null value and null  secretKeyRefName name
        descriptor.getEnvironmentVariables().get(1).safeGetValueFrom().getSecretKeyRef().setName(null);
        assertThat(validator.validateOrThrow(descriptor)).isTrue();
    }


    @Test
    void shouldOnlyAcceptSecretsBelongingToTheBundle() {
        var secretsNames = new String[]{
                PluginStubHelper.BUNDLE_ID + "-env-2-secret-lcorsettientando-xmasbundle",
                "env-2-secret-lcorsettientando-xmasbundle-7485af32-xmasbundle-firegloves-github-org"
        };

        for (var secretName : secretsNames) {
            PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV4WithEnvVars();
            EnvironmentVariable varWithValue = new EnvironmentVariable(
                    PluginStubHelper.TEST_ENV_VAR_1_NAME,
                    PluginStubHelper.TEST_ENV_VAR_1_VALUE,
                    null);
            EnvironmentVariable varWithRef = new EnvironmentVariable(
                    "ENV_2_NAME", null,
                    new ValueFrom().setSecretKeyRef(
                            new SecretKeyRef(secretName, "env-2secret-key")));
            descriptor.setEnvironmentVariables(Arrays.asList(varWithValue, varWithRef));
            descriptor.setDescriptorMetadata(
                    PluginStubHelper.BUNDLE_ID,
                    PluginStubHelper.BUNDLE_CODE,
                    PluginStubHelper.TEST_DESCRIPTOR_IMAGE_SHA,
                    PluginStubHelper.EXPECTED_PLUGIN_NAME,
                    PluginStubHelper.EXPECTED_PLUGIN_NAME_FROM_DEP_BASE_NAME,
                    PluginStubHelper.EXPECTED_INGRESS_PATH_V_5,
                    PluginStubHelper.EXPECTED_INGRESS_PATH_V_3_OR_V_4);

            if (secretName.contains("github-org")) {
                assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));
            } else {
                assertThat(validator.validateOrThrow(descriptor)).isTrue();
            }

        }
    }

    @Test
    void shouldThrowExceptionWhenValidatingAPluginDescriptorWithNonProprietarySecrets() {
        PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV4WithEnvVars()
                .setDescriptorMetadata("abcd1234",
                        "abcd1234-mybundle",
                        PluginStubHelper.EXPECTED_PLUGIN_NAME,
                        PluginStubHelper.TEST_DESCRIPTOR_IMAGE_SHA,
                        PluginStubHelper.TEST_DESCRIPTOR_DEPLOYMENT_BASE_NAME,
                        PluginStubHelper.EXPECTED_INGRESS_PATH_V_5,
                        PluginStubHelper.EXPECTED_INGRESS_PATH_V_3_OR_V_4);
        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));
    }

    @Test
    void shouldValidatePluginDescriptorWithProprietarySecrets() {

        List<EnvironmentVariable> environmentVariables = Arrays.asList(
                new EnvironmentVariable(PluginStubHelper.TEST_ENV_VAR_1_NAME, PluginStubHelper.TEST_ENV_VAR_1_VALUE,
                        null),
                new EnvironmentVariable(PluginStubHelper.TEST_ENV_VAR_2_NAME, null,
                        new ValueFrom().setSecretKeyRef(
                                new SecretKeyRef(PluginStubHelper.BUNDLE_ID + "-"
                                        + PluginStubHelper.EXPECTED_PLUGIN_NAME_FROM_DEP_BASE_NAME,
                                        PluginStubHelper.TEST_ENV_VAR_2_SECRET_KEY))));

        PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV4()
                .setDescriptorMetadata(PluginStubHelper.BUNDLE_ID,
                        PluginStubHelper.BUNDLE_CODE,
                        PluginStubHelper.TEST_DESCRIPTOR_IMAGE_SHA,
                        PluginStubHelper.EXPECTED_PLUGIN_NAME,
                        PluginStubHelper.EXPECTED_PLUGIN_NAME_FROM_DEP_BASE_NAME,
                        PluginStubHelper.EXPECTED_INGRESS_PATH_V_5,
                        PluginStubHelper.EXPECTED_INGRESS_PATH_V_3_OR_V_4)
                .setEnvironmentVariables(environmentVariables);

        Assertions.assertDoesNotThrow(() -> validator.validateOrThrow(descriptor));
    }

    @Test
    void shouldCorrectlyValidatePluginNameLength() {

        PluginDescriptorValidator validator = new PluginDescriptorValidator(50);
        validator.setupValidatorConfiguration();

        PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV4()
                .setDescriptorMetadata(PluginStubHelper.BUNDLE_ID,
                        PluginStubHelper.BUNDLE_CODE,
                        PluginStubHelper.TEST_DESCRIPTOR_IMAGE_SHA,
                        PluginStubHelper.EXPECTED_PLUGIN_NAME,
                        "my-very-very-very-very-very-very-very-very-very-very-very-long-deployment-name",
                        PluginStubHelper.EXPECTED_INGRESS_PATH_V_5,
                        PluginStubHelper.EXPECTED_INGRESS_PATH_V_3_OR_V_4);

        assertThrows(EntandoComponentManagerException.class, () -> validator.validateOrThrow(descriptor));
    }

    @Test
    void shouldCorrectlyValidateIngressPath() {

        PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV5();

        // valid with format not compliant with bundle code
        Stream.of("/mybundle/ingress", "mybundle/ingress")
                .forEach(ingress -> {
                    descriptor.setIngressPath(ingress);
                    assertDoesNotThrow(() -> validator.validateOrThrow(descriptor));
                });

        // NOT valid with format compliant with bundle code
        Stream.of("/mybundle-1a2b3c4d/ingress", "mybundle-1a2b3c4d/ingress")
                .forEach(ingress -> {
                    descriptor.setIngressPath(ingress);
                    assertThrows(EntandoComponentManagerException.class, () -> validator.validateOrThrow(descriptor));
                });
    }

    @Test
    void shouldCorrectlyValidateRoles() {

        PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV5();

        // valid with format not compliant with bundle code
        List<String> roles = Arrays.asList("role1", "roles2");
        descriptor.setRoles(roles);
        assertDoesNotThrow(() -> validator.validateOrThrow(descriptor));

        // NOT valid with format compliant with bundle code
        roles = IntStream.range(0, 401).mapToObj(i -> StringUtils.leftPad("" + i, 10, "0"))
                .collect(Collectors.toList());
        descriptor.setRoles(roles);
        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));
    }

    @Test
    void shouldThrowExceptionWhenReceivingAnInvalidValueForFullDeploymentNameMaxlength() {

        assertThrows(EntandoComponentManagerException.class, () -> new PluginDescriptorValidator(49));
        assertThrows(EntandoComponentManagerException.class, () -> new PluginDescriptorValidator(201));
    }

    @Test
    void shouldAllowValidValueForFullDeploymentNameMaxlength() {

        assertDoesNotThrow(() -> new PluginDescriptorValidator(50));
        assertDoesNotThrow(() -> new PluginDescriptorValidator(51));
        assertDoesNotThrow(() -> new PluginDescriptorValidator(100));
        assertDoesNotThrow(() -> new PluginDescriptorValidator(199));
        assertDoesNotThrow(() -> new PluginDescriptorValidator(200));
    }

    @Test
    void shouldThrowOnIncorrectIngressHealthCheckPathSetting() {
        final PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV5();

        descriptor.setHealthCheckIngress("incorrectValue");
        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));
    }

    @Test
    void shouldAssignDefaultIngressHealthCheckPathSetting() {
        final PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV5();

        // force null value, just in case
        descriptor.setHealthCheckIngress(null);
        validator.validateOrThrow(descriptor);
        assertThat(descriptor.getHealthCheckIngress()).isEqualTo(HEALTHCHECK_INGRESS_TYPE_DEFAULT);
    }

    @Test
    void shouldIngressHealthCheckPathSettingIgnoreCase() {
        final PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV5();

        descriptor.setHealthCheckIngress("CUSTOM");
        validator.validateOrThrow(descriptor);
        assertThat(descriptor.getHealthCheckIngress()).isEqualTo(HEALTHCHECK_INGRESS_TYPE_CUSTOM);

        descriptor.setHealthCheckIngress("caNonIcal");
        validator.validateOrThrow(descriptor);
        assertThat(descriptor.getHealthCheckIngress()).isEqualTo(HEALTHCHECK_INGRESS_TYPE_CANONICAL);
    }

}
