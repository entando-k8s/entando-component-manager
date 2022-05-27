package org.entando.kubernetes.validator.descriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.PostConstruct;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptorVersion;
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
        assertThat(descriptor.getDescriptorVersion()).isEqualTo(PluginDescriptorVersion.V1.getVersion());
    }

    @Test
    void shouldNotOverrideDescriptorVersionWhileEnsuringDescriptorVersionIsSet() {
        PluginDescriptor descriptor = (PluginDescriptor) new PluginDescriptor()
                .setDescriptorVersion(PluginDescriptorVersion.V3.getVersion());
        descriptor = baseDescriptorValidator.ensureDescriptorVersionIsSet(descriptor);
        assertThat(descriptor.getDescriptorVersion()).isEqualTo(PluginDescriptorVersion.V3.getVersion());
    }

    @Test
    void shouldConfigureValidationConfigMap() {

        PluginDescriptorVersion descriptorVersion = PluginDescriptorVersion.V1;
        DescriptorValidationFunction<PluginDescriptor> validationFunction = (PluginDescriptor descriptor) -> descriptor;
        Function<PluginDescriptor, Object> mustNotBeNull = (PluginDescriptor descriptor) -> descriptor;
        Function<PluginDescriptor, Object> mustBeNull = (PluginDescriptor descriptor) -> descriptor;

        baseDescriptorValidator.configureValidationConfigMap(descriptorVersion,
                Collections.singletonList(validationFunction),
                Map.of("notNull", mustNotBeNull),
                Map.of("null", mustBeNull));

        final Map<PluginDescriptorVersion, DescriptorValidatorConfigBean<PluginDescriptor, PluginDescriptorVersion>>
                validationConfigMap = baseDescriptorValidator.getValidationConfigMap();

        assertThat(validationConfigMap).containsOnlyKeys(descriptorVersion);
        assertThat(validationConfigMap.get(descriptorVersion).getValidationFunctions()).containsExactly(
                validationFunction);
        assertThat(validationConfigMap.get(descriptorVersion).getObjectsThatMustNOTBeNull()).containsOnly(
                entry("notNull", mustNotBeNull));
        assertThat(validationConfigMap.get(descriptorVersion).getObjectsThatMustBeNull()).containsOnly(
                entry("null", mustBeNull));
    }


    /**
     * test implementation of BaseDescriptorValidator.
     */
    private static class DummyBaseDescriptorValidator extends
            BaseDescriptorValidator<PluginDescriptor, PluginDescriptorVersion> {

        public DummyBaseDescriptorValidator() {
            super(PluginDescriptorVersion.class);
            super.validationConfigMap = new EnumMap<>(PluginDescriptorVersion.class);
        }

        @Override
        protected PluginDescriptorVersion readDescriptorVersion(PluginDescriptor descriptor) {
            return PluginDescriptorVersion.fromVersion(descriptor.getDescriptorVersion());
        }

        @PostConstruct
        public void setupValidatorConfiguration() {
            super.validationConfigMap = new EnumMap<>(PluginDescriptorVersion.class);

            Map<String, Function<PluginDescriptor, Object>> objectsThatMustNOTBeNull = new LinkedHashMap<>();
            objectsThatMustNOTBeNull.put("spec", PluginDescriptor::getSpec);

            Map<String, Function<PluginDescriptor, Object>> objectsThatMustBeNull = new LinkedHashMap<>();
            objectsThatMustBeNull.put("image", PluginDescriptor::getImage);

            configureValidationConfigMap(PluginDescriptorVersion.V1,
                    Collections.singletonList(super::validateDescriptorFormatOrThrow),
                    objectsThatMustNOTBeNull, objectsThatMustBeNull);
        }
    }
}
