package org.entando.kubernetes.validator;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.collections4.KeyValue;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;
import org.apache.commons.lang.StringUtils;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptorVersion;
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
        validator = new PluginDescriptorValidator();
        validator.setupValidatorConfiguration();
    }

    @Test
    void shouldCorrectlyValidatePluginDescriptorV1() throws IOException {
        PluginDescriptor descriptor = yamlMapper
                .readValue(new File("src/test/resources/bundle/plugins/todomvcV1.yaml"),
                        PluginDescriptor.class);
        validator.validateOrThrow(descriptor);
    }

    @Test
    void shouldCorrectlyValidatePluginDescriptorV2() throws IOException {
        PluginDescriptor descriptor = yamlMapper
                .readValue(new File("src/test/resources/bundle/plugins/todomvcV2.yaml"),
                        PluginDescriptor.class);
        validator.validateOrThrow(descriptor);

        descriptor = yamlMapper
                .readValue(new File("src/test/resources/bundle/plugins/todomvcV2_complete.yaml"),
                        PluginDescriptor.class);
        validator.validateOrThrow(descriptor);
    }

    @Test
    void shouldCorrectlyValidatePluginDescriptorV3() throws IOException {
        PluginDescriptor descriptor = yamlMapper
                .readValue(new File("src/test/resources/bundle/plugins/todomvcV3.yaml"),
                        PluginDescriptor.class);
        validator.validateOrThrow(descriptor);

        descriptor = yamlMapper
                .readValue(new File("src/test/resources/bundle/plugins/todomvcV3_complete.yaml"),
                        PluginDescriptor.class);
        validator.validateOrThrow(descriptor);
    }

    @Test
    void shouldThrowExceptionWhenPluginDescriptorVersionNotRecognized() throws Exception {

        PluginDescriptor descriptor = yamlMapper
                .readValue(new File("src/test/resources/bundle/plugins/todomvcV2.yaml"),
                        PluginDescriptor.class);
        descriptor.setDescriptorVersion("v2.5");

        Assertions.assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));
    }

    @Test
    void shouldThrowExceptionIfPluginDescriptorVersionIsNOTCompatibleWithTheDataFormat() throws IOException {

        List<KeyValue<String, PluginDescriptorVersion>> descriptorVersionToSetList = Arrays.asList(
                new DefaultKeyValue<>("todomvcV1.yaml", PluginDescriptorVersion.V2),
                new DefaultKeyValue<>("todomvcV2.yaml", PluginDescriptorVersion.V1),
                new DefaultKeyValue<>("todomvcV3_complete.yaml", PluginDescriptorVersion.V1),
                new DefaultKeyValue<>("todomvcV4_complete.yaml", PluginDescriptorVersion.V3));

        for (KeyValue<String, PluginDescriptorVersion> keyValue : descriptorVersionToSetList) {

            PluginDescriptor descriptor = yamlMapper
                    .readValue(new File("src/test/resources/bundle/plugins/" + keyValue.getKey()),
                            PluginDescriptor.class);
            descriptor.setDescriptorVersion(keyValue.getValue().getVersion());

            Assertions.assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));
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
            Assertions.assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(pluginDescriptor));
        }
    }

    @Test
    void shouldThrowExceptionIfEnvironmentVariablesAreNOTValid() {

        PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV4WithEnvVars();
        assertThat(validator.validateOrThrow(descriptor)).isTrue();

        // with empty name should fail
        descriptor.getEnvironmentVariables().get(0).setName("");
        Assertions.assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));

        // with null name should fail
        descriptor.getEnvironmentVariables().get(0).setName(null);
        Assertions.assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));

        descriptor.getEnvironmentVariables().get(0).setName(PluginStubHelper.TEST_ENV_VAR_1_NAME);

        // with empty value and null secret should fail
        descriptor.getEnvironmentVariables().get(0).setValue("");
        Assertions.assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));

        // with null value and null secret should fail
        descriptor.getEnvironmentVariables().get(0).setValue(null);
        Assertions.assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));

        descriptor.getEnvironmentVariables().get(0).setValue(PluginStubHelper.TEST_ENV_VAR_1_VALUE);

        // with empty value and empty secretKeyRefName should fail
        descriptor.getEnvironmentVariables().get(1).getSecretKeyRef().setKey("");
        Assertions.assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));

        // with null value and empty secretKeyRefName should fail
        descriptor.getEnvironmentVariables().get(1).getSecretKeyRef().setKey(null);
        Assertions.assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));

        descriptor.getEnvironmentVariables().get(1).getSecretKeyRef()
                .setKey(PluginStubHelper.TEST_ENV_VAR_2_SECRET_KEY);

        // with empty value and empty secretKeyRefName should fail
        descriptor.getEnvironmentVariables().get(1).getSecretKeyRef().setName("");
        Assertions.assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));

        // with null value and empty secretKeyRefName should fail
        descriptor.getEnvironmentVariables().get(1).getSecretKeyRef().setName(null);
        Assertions.assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));

        // violating RFC 1123
        Stream.of("ENV", "env_1", "-env-1", "env-1-", "env?1", StringUtils.repeat("a", 254))
                .forEach(name -> {
                    descriptor.getEnvironmentVariables().get(1).getSecretKeyRef().setName(name);
                    Assertions.assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(descriptor));
                });
    }
}
