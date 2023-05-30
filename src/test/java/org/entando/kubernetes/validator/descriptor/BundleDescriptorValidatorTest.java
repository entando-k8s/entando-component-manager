package org.entando.kubernetes.validator.descriptor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.DescriptorVersion;
import org.entando.kubernetes.stubhelper.BundleStubHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class BundleDescriptorValidatorTest {

    private BundleDescriptorValidator validator;

    @BeforeEach
    public void setup() {
        validator = new BundleDescriptorValidator();
        validator.setupValidatorConfiguration();
    }

    @Test
    void shouldCorrectlyValidateABundleDescriptorV1WithValidFields() {
        BundleDescriptor bundleDescriptor = BundleStubHelper.stubBundleDescriptor(null)
                .setComponents(new ComponentSpecDescriptor());

        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(bundleDescriptor));

        bundleDescriptor.setName(null);
        assertDoesNotThrow(() -> validator.validateOrThrow(bundleDescriptor));
    }

    @Test
    void shouldCorrectlyValidateABundleDescriptorV5WithValidFields() {
        BundleDescriptor bundleDescriptor = BundleStubHelper.stubBundleDescriptor(null)
                .setComponents(new ComponentSpecDescriptor());
        bundleDescriptor.setDescriptorVersion(DescriptorVersion.V5.getVersion());

        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(bundleDescriptor));

        bundleDescriptor.setCode(null);
        assertDoesNotThrow(() -> validator.validateOrThrow(bundleDescriptor));
    }

    @Test
    void shouldThrowExceptionWhileValidatingABundleDescriptorWithInvalidFields() {
        BundleDescriptor bundleDescriptor = BundleStubHelper.stubBundleDescriptor(null);

        // empty components
        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(bundleDescriptor));

        // empty code
        bundleDescriptor.setComponents(new ComponentSpecDescriptor());
        bundleDescriptor.setCode(null);
        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(bundleDescriptor));
    }


    @Test
    void shouldCorrectlyValidateABundleDescriptorV6WithValidFields() {
        BundleDescriptor bundleDescriptor = BundleStubHelper.stubBundleDescriptor(null)
                .setComponents(new ComponentSpecDescriptor());
        bundleDescriptor.setDescriptorVersion(DescriptorVersion.V6.getVersion());

        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(bundleDescriptor));

        bundleDescriptor.setCode(null);
        assertDoesNotThrow(() -> validator.validateOrThrow(bundleDescriptor));
    }

    @Test
    void shouldThrowExceptionWhileValidatingABundleDescriptorV6WithInvalidFields() {
        BundleDescriptor bundleDescriptor = BundleStubHelper.stubBundleDescriptor(null)
                .setComponents(new ComponentSpecDescriptor());
        bundleDescriptor.setDescriptorVersion(DescriptorVersion.V6.getVersion());

        // without name
        var name = bundleDescriptor.getName();
        bundleDescriptor.setName(null);
        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(bundleDescriptor));
        bundleDescriptor.setName(name);

        // with code
        bundleDescriptor.setCode("awesome-code");
        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(bundleDescriptor));
        bundleDescriptor.setCode(null);

        // without components
        bundleDescriptor.setComponents(null);
        assertThrows(InvalidBundleException.class, () -> validator.validateOrThrow(bundleDescriptor));
    }
}
