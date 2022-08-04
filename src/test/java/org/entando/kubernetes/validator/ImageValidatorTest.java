package org.entando.kubernetes.validator;

import static org.assertj.core.api.Java6Assertions.assertThat;

import java.util.stream.Stream;
import org.entando.kubernetes.exception.EntandoValidationException;
import org.junit.Assert;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class ImageValidatorTest {

    private final String emptyMex = "empty";
    private final String invalidMex = "not valid";

    private final String[] okImageUrl = new String[]{
            "docker://library/nginx", // library can be a valid hostname ...
            "docker://docker.io/nginx@sha256:92ae85a2740161f8b534e0b85ad267624ea88def5691742008f9353cc72ec060",
            "docker://quay.io/centos7/nginx-116-centos7@sha256:92ae85a2740161f8b534e0b85ad267624ea88def5691742008f9353cc72ec060",
            "docker://docker.io/library/nginx",
            "docker://docker.io/my-library/nginx",
            "docker://docker.io/nginx",
            // "docker://docker.io/library/nginx:latest",
            "docker://docker.io/nginx:1.2.3",
            "docker://localhost/entando/nginx:1.2.3",
            "docker://test.com:8080/entando/nginx:1.2.3"
    };


    @Test
    void validationShouldBeOk() {
        Stream.of(okImageUrl).forEach(t -> {
            assertThat(ImageValidator.parse(t).isValidOrThrow(invalidMex)).isTrue();
        });
    }

    @Test
    void validationShouldThrowError() {
        Stream.of(
                null,
                "docker://docker.io-/library/nginx:12",
                "docker://docker.io/library-/nginx:12",
                "docker://docker.io/library/-nginx:12",
                "docker://-docker.io/library/nginx:12",
                "docker://docker.io-/library/nginx:12",
                "docker://docker.io/",
                "docker://library/nginx:1212:12",
                "docker://library/nginx:",
                "docker://host:port/entando/nginx:12",
                "docker.io/library/nginx",
                "docker:///nginx:1212:12",
                "oci://docker.io/library/nginx"
        ).forEach(t -> {
            ImageValidator validator = ImageValidator.parse(t);
            try {
                validator.isValidOrThrow(invalidMex);
                Assert.fail("validation must throw error for image url: " + t);
            } catch (EntandoValidationException ex) {
                assertThat(ex.getMessage()).startsWith(invalidMex);
            }
        });
    }

    @Test
    void composeShouldBeOk() {
        final String COMPOSED = "docker://docker.io/library/nginx";
        final String test = "docker://docker.io/nginx@sha256:92ae85a2740161f8b534e0b85ad267624ea88def5691742008f9353cc72ec060";

        assertThat(ImageValidator.parse(test).composeCommonUrlOrThrow(invalidMex)).isEqualTo(COMPOSED);
    }

    @Test
    void composeWithoutTransportWithoutTagShouldBeOk() {
        final String COMPOSED = "docker.io/library/nginx";
        final String test = "docker://docker.io/nginx@sha256:92ae85a2740161f8b534e0b85ad267624ea88def5691742008f9353cc72ec060";

        assertThat(ImageValidator.parse(test).composeCommonUrlWithoutTransportWithoutTagOrThrow(invalidMex)).isEqualTo(
                COMPOSED);
    }

    @Test
    void composeWithoutTransportShouldBeOk() {
        final String COMPOSED = "docker.io/library/nginx@sha256:92ae85a2740161f8b534e0b85ad267624ea88def5691742008f9353cc72ec060";
        final String test = "docker://docker.io/nginx@sha256:92ae85a2740161f8b534e0b85ad267624ea88def5691742008f9353cc72ec060";

        assertThat(ImageValidator.parse(test).composeCommonUrlWithoutTransportOrThrow(invalidMex)).isEqualTo(
                COMPOSED);
    }

}
