package org.entando.kubernetes.validator.descriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.PostConstruct;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.model.bundle.descriptor.DescriptorVersion;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.validator.descriptor.DescriptorValidatorConfigBean.DescriptorValidationFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class BaseDescriptorValidatorTest {

    private DummyBaseDescriptorValidator baseDescriptorValidator;


    @BeforeEach
    public void setup() {
        baseDescriptorValidator = new DummyBaseDescriptorValidator();
    }

    @Test
    void shouldEnsureDescriptorVersionIsSet() {
        PluginDescriptor descriptor = new PluginDescriptor();
        descriptor = baseDescriptorValidator.ensureDescriptorVersionIsSet(descriptor);
        assertThat(descriptor.getDescriptorVersion()).isEqualTo(DescriptorVersion.V1.getVersion());
    }

    @Test
    void shouldNotOverrideDescriptorVersionWhileEnsuringDescriptorVersionIsSet() {
        PluginDescriptor descriptor = (PluginDescriptor) new PluginDescriptor()
                .setDescriptorVersion(DescriptorVersion.V3.getVersion());
        descriptor = baseDescriptorValidator.ensureDescriptorVersionIsSet(descriptor);
        assertThat(descriptor.getDescriptorVersion()).isEqualTo(DescriptorVersion.V3.getVersion());
    }

    @Test
    void shouldConfigureValidationConfigMap() {

        DescriptorVersion descriptorVersion = DescriptorVersion.V1;
        DescriptorValidationFunction<PluginDescriptor> validationFunction = (PluginDescriptor descriptor) -> descriptor;
        Function<PluginDescriptor, Object> mustNotBeNull = (PluginDescriptor descriptor) -> descriptor;
        Function<PluginDescriptor, Object> mustBeNull = (PluginDescriptor descriptor) -> descriptor;

        baseDescriptorValidator.addValidationConfigMap(descriptorVersion,
                Collections.singletonList(validationFunction),
                Map.of("notNull", mustNotBeNull),
                Map.of("null", mustBeNull));

        final Map<DescriptorVersion, DescriptorValidatorConfigBean<PluginDescriptor>>
                validationConfigMap = baseDescriptorValidator.getValidationConfigMap();

        assertThat(validationConfigMap).containsOnlyKeys(descriptorVersion);
        assertThat(validationConfigMap.get(descriptorVersion).getValidationFunctions()).containsExactly(
                validationFunction);
        assertThat(validationConfigMap.get(descriptorVersion).getObjectsThatMustNOTBeNull()).containsOnly(
                entry("notNull", mustNotBeNull));
        assertThat(validationConfigMap.get(descriptorVersion).getObjectsThatMustBeNull()).containsOnly(
                entry("null", mustBeNull));
    }

    @Test
    void shouldThrowExceptionWithUnrecognizedVersion() {

        baseDescriptorValidator.addValidationConfigMap(DescriptorVersion.V1,
                Collections.emptyList(),
                Collections.emptyMap(),
                Collections.emptyMap());

        baseDescriptorValidator.addValidationConfigMap(DescriptorVersion.V5,
                Collections.emptyList(),
                Collections.emptyMap(),
                Collections.emptyMap());

        PluginDescriptor descriptor = (PluginDescriptor) new PluginDescriptor()
                .setDescriptorMetadata("abcdefgh", "abcdefgh-my-bundle", "1ab2c3d4", "code", "name", "endpooint", "cusIngr")
                .setDescriptorVersion(DescriptorVersion.V5.getVersion());

        // existent version should work
        assertDoesNotThrow(() -> baseDescriptorValidator.validateOrThrow(descriptor));

        // non existent version should throw
        descriptor.setDescriptorVersion("non_existent");
        assertThrows(InvalidBundleException.class, () -> baseDescriptorValidator.validateOrThrow(descriptor));

        descriptor.setDescriptorVersion(DescriptorVersion.V3.getVersion());
        assertThrows(InvalidBundleException.class, () -> baseDescriptorValidator.validateOrThrow(descriptor));
    }


    /**
     * test implementation of BaseDescriptorValidator.
     */
    private static class DummyBaseDescriptorValidator extends
            BaseDescriptorValidator<PluginDescriptor> {

        public DummyBaseDescriptorValidator() {
            super.validationConfigMap = new EnumMap<>(DescriptorVersion.class);
        }

        @Override
        protected DescriptorVersion readDescriptorVersion(PluginDescriptor descriptor) {
            return DescriptorVersion.fromVersion(descriptor.getDescriptorVersion());
        }

        @PostConstruct
        public void setupValidatorConfiguration() {
            super.validationConfigMap = new EnumMap<>(DescriptorVersion.class);

            Map<String, Function<PluginDescriptor, Object>> objectsThatMustNOTBeNull = new LinkedHashMap<>();
            objectsThatMustNOTBeNull.put("spec", PluginDescriptor::getSpec);

            Map<String, Function<PluginDescriptor, Object>> objectsThatMustBeNull = new LinkedHashMap<>();
            objectsThatMustBeNull.put("image", PluginDescriptor::getImage);

            addValidationConfigMap(DescriptorVersion.V1,
                    Collections.singletonList(super::validateDescriptorFormatOrThrow),
                    objectsThatMustNOTBeNull, objectsThatMustBeNull);
        }
    }
}
