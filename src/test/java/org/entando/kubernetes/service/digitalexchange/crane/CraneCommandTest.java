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
        assertThat(target.getImageDigest("ubuntu:22.10")).isEqualTo(
                "sha256:3cc2e19d3538681fb614683187329b47520424b80d7b31c97cdeb2403d82cae9");
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
