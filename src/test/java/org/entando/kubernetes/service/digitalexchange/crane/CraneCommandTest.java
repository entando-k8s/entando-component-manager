package org.entando.kubernetes.service.digitalexchange.crane;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.entando.kubernetes.service.digitalexchange.crane.CraneCommand.CraneException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class CraneCommandTest {

    private CraneCommand target;

    @BeforeEach
    public void setup() {
        this.target = new CraneCommand();
    }

    @Test
    void shouldReturnTheExpectedDigest() {
        assertThat(target.getImageDigest("entando/entando-component-manager:7.0.0")).isEqualTo(
                "sha256:bb52b0dfd55d98b92e7a0540d1e0237a7ed594dcb2cce3bced597e3d7c567c2c");
    }

    @Test
    void shouldThrowExceptionWhileFetchingDigestOfNonExistentImage() {
        assertThrows(CraneException.class, () -> target.getImageDigest("what-img:NOT_EXISTING"));
    }

    @Test
    void shouldThrowExceptionWhileFetchingDigestOfEmptyValues() {
        assertThrows(CraneException.class, () -> target.getImageDigest(""));
        assertThrows(CraneException.class, () -> target.getImageDigest(null));
    }
}
