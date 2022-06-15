package org.entando.kubernetes.validator.descriptor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
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
    void shouldCorrectlyValidateABundleDescriptorWithValidFields() {
        BundleDescriptor bundleDescriptor = BundleStubHelper.stubBundleDescriptor(null);
        bundleDescriptor.setComponents(new ComponentSpecDescriptor());
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
}
