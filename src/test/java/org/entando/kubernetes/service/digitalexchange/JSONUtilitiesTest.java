package org.entando.kubernetes.service.digitalexchange;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentKey;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.Descriptor;
import org.entando.kubernetes.stubhelper.BundleStubHelper;
import org.junit.jupiter.api.Test;
import org.zalando.problem.ThrowableProblem;

class JSONUtilitiesTest {

    @Test
    void serializeDescriptor_shouldBeOk() {
        String contentDescriptorFile = "/contents/cng102-descriptor.yaml";
        ComponentSpecDescriptor spec = new ComponentSpecDescriptor();
        spec.setContents(Collections.singletonList(contentDescriptorFile));
        final BundleDescriptor bundleDescriptor = BundleStubHelper.stubBundleDescriptor(spec);
        assertDoesNotThrow(() -> JSONUtilities.serializeDescriptor(bundleDescriptor));
    }

    @Test
    void serializeDescriptor_shouldThrowException() {
        final DummyDescriptor dummyDescriptor = new DummyDescriptor();
        assertThrows(ThrowableProblem.class, () -> JSONUtilities.serializeDescriptor(dummyDescriptor));
    }

    /**
     * dummy descriptor useful to simulate JsonProcessingException.
     */
    private static class DummyDescriptor implements Descriptor {

        public DummyDescriptor getSelf() {
            return this;
        }

        @Override
        public String getDescriptorClassName() {
            return Descriptor.super.getDescriptorClassName();
        }

        @Override
        public ComponentKey getComponentKey() {
            return new ComponentKey("bye dear");
        }

        @Override
        public String toString() {
            return getSelf().getClass().getName();
        }
    }
}
