package org.entando.kubernetes.model.bundle.descriptor;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.entando.kubernetes.model.bundle.descriptor.DockerImage.MalformedDockerImageException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class DockerImageTest {

    @Test
    void shouldComposeDockerImageFromStringWithTag() {
        final DockerImage dockerImage = DockerImage.fromString("entando/simple-ms:1.0.0");
        assertThat(dockerImage.getOrganization()).isEqualTo("entando");
        assertThat(dockerImage.getName()).isEqualTo("simple-ms");
        assertThat(dockerImage.getTag()).isEqualTo("1.0.0");
        assertThat(dockerImage.getSha256()).isNull();
    }

    @Test
    void shouldComposeDockerImageFromStringWithSha256() {
        final DockerImage dockerImage = DockerImage.fromString(
                "entando/simple-ms@sha256:77c38218c01983e4a1cf7f16b6035eb153be0099a146895495c40d83296f71cc");
        assertThat(dockerImage.getOrganization()).isEqualTo("entando");
        assertThat(dockerImage.getName()).isEqualTo("simple-ms");
        assertThat(dockerImage.getTag()).isNull();
        assertThat(dockerImage.getSha256()).isEqualTo(
                "sha256:77c38218c01983e4a1cf7f16b6035eb153be0099a146895495c40d83296f71cc");
    }

    @Test
    void shouldThrowExceptionWithInvalidDockerImage() {
        // empty
        assertThrows(MalformedDockerImageException.class, () -> DockerImage.fromString(""));
        // null
        assertThrows(MalformedDockerImageException.class, () -> DockerImage.fromString(null));
        // without org / image name
        assertThrows(MalformedDockerImageException.class, () -> DockerImage.fromString("simple-ms:1.0.0"));
        // malformed
        assertThrows(MalformedDockerImageException.class, () -> DockerImage.fromString("entando//simple-ms:1.0.0"));
    }
}
