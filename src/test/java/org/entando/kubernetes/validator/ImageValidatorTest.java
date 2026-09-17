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

    private final String garUrl = "docker://europe-west8-docker.pkg.dev/my-project/my-repo/my-bundle:1.0.0";

    @Test
    void parseShouldSplitOrganizationAndRepositoryWithMoreThanTwoPathComponents() {
        final ImageValidator validator = ImageValidator.parse(garUrl);

        assertThat(validator.getTransport()).isEqualTo(ImageValidator.DOCKER_TRANSPORT);
        assertThat(validator.getDomainRegistry()).isEqualTo("europe-west8-docker.pkg.dev");
        assertThat(validator.getOrganization()).isEqualTo("my-project/my-repo");
        assertThat(validator.getRepository()).isEqualTo("my-bundle");
        assertThat(validator.getTag()).isEqualTo("1.0.0");
        assertThat(validator.isDigest()).isFalse();
        assertThat(validator.isValidOrThrow(invalidMex)).isTrue();
    }

    @Test
    void validationShouldBeOkWithMoreThanTwoPathComponents() {
        Stream.of(
                // Google Artifact Registry: <domain>/<project>/<repository>/<image>
                garUrl,
                "docker://europe-west8-docker.pkg.dev/my-project/my-repo/my-bundle",
                "docker://europe-west8-docker.pkg.dev/my-project/my-repo/my-bundle"
                        + "@sha256:92ae85a2740161f8b534e0b85ad267624ea88def5691742008f9353cc72ec060",
                // GitLab container registry: <domain>/<group>/<subgroup>/<project>/<image>
                "docker://registry.gitlab.com/my-group/my-subgroup/my-project/my-bundle:1.0.0",
                "docker://registry.gitlab.com/my-group/my-subgroup/my-project/my-bundle"
                        + "@sha256:92ae85a2740161f8b534e0b85ad267624ea88def5691742008f9353cc72ec060"
        ).forEach(t -> assertThat(ImageValidator.parse(t).isValidOrThrow(invalidMex)).isTrue());
    }

    @Test
    void deeplyNestedPathShouldKeepOnlyTheLastComponentAsRepository() {
        final ImageValidator validator = ImageValidator.parse(
                "docker://registry.gitlab.com/my-group/my-subgroup/my-project/my-bundle:1.0.0");

        assertThat(validator.getDomainRegistry()).isEqualTo("registry.gitlab.com");
        assertThat(validator.getOrganization()).isEqualTo("my-group/my-subgroup/my-project");
        assertThat(validator.getRepository()).isEqualTo("my-bundle");
        assertThat(validator.getTag()).isEqualTo("1.0.0");
    }

    @Test
    void composeShouldPreserveAllPathComponents() {
        assertThat(ImageValidator.parse(garUrl).composeCommonUrlOrThrow(invalidMex))
                .isEqualTo("docker://europe-west8-docker.pkg.dev/my-project/my-repo/my-bundle");

        assertThat(ImageValidator.parse(garUrl).composeCommonUrlWithoutTransportWithoutTagOrThrow(invalidMex))
                .isEqualTo("europe-west8-docker.pkg.dev/my-project/my-repo/my-bundle");

        assertThat(ImageValidator.parse(garUrl).composeCommonUrlWithoutTransportOrThrow(invalidMex))
                .isEqualTo("europe-west8-docker.pkg.dev/my-project/my-repo/my-bundle:1.0.0");
    }

    @Test
    void validationShouldStillThrowErrorWithMoreThanTwoInvalidPathComponents() {
        Stream.of(
                // invalid organization
                "docker://europe-west8-docker.pkg.dev/-my-project/my-repo/my-bundle:1.0.0",
                "docker://europe-west8-docker.pkg.dev/my-project/my-repo-/my-bundle:1.0.0",
                // invalid repository
                "docker://europe-west8-docker.pkg.dev/my-project/my-repo/-my-bundle:1.0.0",
                // invalid domain registry
                "docker://-europe-west8-docker.pkg.dev/my-project/my-repo/my-bundle:1.0.0",
                // invalid tag
                "docker://europe-west8-docker.pkg.dev/my-project/my-repo/my-bundle:",
                // invalid transport
                "oci://europe-west8-docker.pkg.dev/my-project/my-repo/my-bundle:1.0.0"
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
    void singlePathComponentShouldStillFallbackToTheOfficialLibrary() {
        final ImageValidator validator = ImageValidator.parse("docker://docker.io/nginx:1.2.3");

        assertThat(validator.getOrganization()).isEqualTo(ImageValidator.DOCKER_OFFICIAL_LIBRARY);
        assertThat(validator.getRepository()).isEqualTo("nginx");
        assertThat(validator.composeCommonUrlOrThrow(invalidMex)).isEqualTo("docker://docker.io/library/nginx");
    }

    @Test
    void twoPathComponentsShouldStillBeSplitAsOrganizationAndRepository() {
        final ImageValidator validator = ImageValidator.parse("docker://quay.io/centos7/nginx-116-centos7:1.2.3");

        assertThat(validator.getOrganization()).isEqualTo("centos7");
        assertThat(validator.getRepository()).isEqualTo("nginx-116-centos7");
        assertThat(validator.composeCommonUrlOrThrow(invalidMex))
                .isEqualTo("docker://quay.io/centos7/nginx-116-centos7");
    }
}
