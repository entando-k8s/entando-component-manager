package org.entando.kubernetes.model.bundle.descriptor;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;
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
    void shouldComposeWithRegistry() {
        final DockerImage dockerImage = DockerImage.fromString(
                "docker.io/entando/my-test/entando-process-driven-plugin:7.1.0");
        assertThat(dockerImage.getRegistry()).isEqualTo("docker.io");
        assertThat(dockerImage.getOrganization()).isEqualTo("entando/my-test");
        assertThat(dockerImage.getName()).isEqualTo("entando-process-driven-plugin");
        assertThat(dockerImage.getTag()).isNotNull();
        assertThat(dockerImage.getTag()).isEqualTo("7.1.0");
    }

    @Test
    void shouldComposeWithoutRegistry() {
        final DockerImage dockerImage1 = DockerImage.fromString(
                "org1/org2/entando-process-driven-plugin:7.1.0");
        assertThat(dockerImage1.getRegistry()).isNull();
        assertThat(dockerImage1.getOrganization()).isEqualTo("org1/org2");
        assertThat(dockerImage1.getName()).isEqualTo("entando-process-driven-plugin");
        assertThat(dockerImage1.getTag()).isNotNull();
        assertThat(dockerImage1.getTag()).isEqualTo("7.1.0");

        final DockerImage dockerImage2 = DockerImage.fromString(
                "org1/org2/org3/entando-process-driven-plugin:7.1.0");
        assertThat(dockerImage2.getRegistry()).isNull();
        assertThat(dockerImage2.getOrganization()).isEqualTo("org1/org2/org3");
        assertThat(dockerImage2.getName()).isEqualTo("entando-process-driven-plugin");
        assertThat(dockerImage2.getTag()).isNotNull();
        assertThat(dockerImage2.getTag()).isEqualTo("7.1.0");

    }

    @Test
    void shouldComposeCorrectToString() {
        Stream.of("entando/simple-ms:1.0.0",
                "entando/simple-ms@sha256:77c38218c01983e4a1cf7f16b6035eb153be0099a146895495c40d83296f71cc",
                "docker.io/entando/my-test/entando-process-driven-plugin:7.1.0").forEach(s -> {
                    final DockerImage dockerImage = DockerImage.fromString(s);
                    assertThat(dockerImage.toString()).isEqualTo(s);
                });
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
        // invalid
        assertThrows(MalformedDockerImageException.class, () -> DockerImage.fromString("example.org/simple-ms:1.0.0"));
        assertThrows(MalformedDockerImageException.class, () -> DockerImage.fromString("example.org"));
    }

}
