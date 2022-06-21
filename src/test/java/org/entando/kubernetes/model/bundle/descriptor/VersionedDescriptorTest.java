package org.entando.kubernetes.model.bundle.descriptor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class VersionedDescriptorTest {

    @Test
    void shouldDetectVersionEqualOrGreaterThanTheReceived() {

        VersionedDescriptor descriptor = new DummyVersionedDescriptor()
                .setDescriptorVersion(DescriptorVersion.V4.getVersion());

        assertThat(descriptor.isVersionEqualOrGreaterThan(DescriptorVersion.V3)).isTrue();
        assertThat(descriptor.isVersionEqualOrGreaterThan(DescriptorVersion.V4)).isTrue();
    }

    @Test
    void shouldDetectVersionMinorThanTheReceived() {

        VersionedDescriptor descriptor = new DummyVersionedDescriptor()
                .setDescriptorVersion(DescriptorVersion.V2.getVersion());

        assertThat(descriptor.isVersionEqualOrGreaterThan(DescriptorVersion.V3)).isFalse();
    }

    @Test
    void shouldReturnFalseWhileReceivingAnInvalidDescriptorVersion() {

        VersionedDescriptor descriptor = new DummyVersionedDescriptor()
                .setDescriptorVersion("vFive");

        assertThat(descriptor.isVersionEqualOrGreaterThan(DescriptorVersion.V3)).isFalse();
    }

    private class DummyVersionedDescriptor extends VersionedDescriptor {
        @Override
        public ComponentKey getComponentKey() {
            return null;
        }
    }
}
