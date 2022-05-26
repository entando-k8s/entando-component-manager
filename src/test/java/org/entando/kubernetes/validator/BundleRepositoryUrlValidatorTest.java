package org.entando.kubernetes.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.entando.kubernetes.exception.EntandoValidationException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class BundleRepositoryUrlValidatorTest {

    private BundleRepositoryUrlValidator validator = new BundleRepositoryUrlValidator(
            BundleRepositoryUrlValidator.STANDARD_REPO_URL_MAX_LENGTH,
            BundleRepositoryUrlValidator.STANDARD_REPO_URL_MAX_SUBPATHS);

    @Test
    void shouldThrowTheExpectedExceptionIfUrlIsEmpty() {

        final EntandoValidationException entandoValidationException = assertThrows(EntandoValidationException.class,
                () -> validator.validateOrThrow(null));

        assertThat(entandoValidationException.getMessage()).isEqualTo("Empty repo URL detected");
    }

    @Test
    void shouldThrowTheExpectedExceptionIfUrlIsNotCompliant() {

        Stream.of("", "ftp://entando.com", "http://", "https://", "https://.com", "http://.com", "https://my-domain-",
                        "https://my-domain.", "http:// ", "http://com.", "http://.com")
                .forEach(urlString -> {
                    try {
                        assertThrows(EntandoValidationException.class,
                                () -> validator.validateOrThrow(urlString),
                                urlString);
                    } catch (Exception e) {
                        fail(e.getMessage());
                    }
                });
    }

    @Test
    void shouldNotThrowExceptionWhenUrlIsCompliant() {

        Stream.of("https://www.mydomain.com?myparam=value&seconp=myval", "http://www.entando.com",
                        "https://www.entando.com", "http://www.entando.com:80",
                        "https://www.ENtando.com", "http://www.enta-ndo.com:092/3-4/ci-one/asdk-a/",
                        "http://www.en_ta-ndo.com:092/3_a/", "http://www.enta-ndo.com:092/3-",
                        "http://www.en_ta-ndo.com:092/ad-/a/a__SDAda", "https://my-domain", "https://localhost",
                        "https://www.mydomain.com/?myparam=value", "https://www.mydomain.com?myparam=value&seconp=myval",
                        "http://www.enta-ndo.com:123456", "http://www.entando.com/my.sec", "https://localhost/")
                .forEach(urlString -> {
                    String url = validator.validateOrThrow(urlString);
                    assertThat(url).isEqualTo(urlString);
                });
    }

    @Test
    void shouldReplaceGitAndSshProtocolWithHttpAndShouldNotThrowException() {
        List<String> testCasesList = List.of(
                "git@github.com:entando/my_bundle.git",
                "git://github.com/entando/my_bundle.git",
                "ssh://github.com/entando/my_bundle.git",
                "git@github.com:entando:my_bundle.git");

        testCasesList.forEach(url -> {
            String actual = validator.validateOrThrow(url);
            assertThat(actual).isEqualTo(url);
        });
    }

    @Test
    void shouldReturnTheSameStringWhenProtocolIsNotOneOfTheExpected() {
        List<String> testCasesList = List.of(
                "got@github.com/entando/my_bundle.git",
                "got://github.com/entando/my_bundle.git",
                "sssh://github.com/entando/my_bundle.git",
                "ftp://github.com/entando/my_bundle.git");

        testCasesList.forEach(url ->
                assertThrows(EntandoValidationException.class,
                        () -> validator.validateOrThrow(url)));
    }

    @Test
    void shouldThrowExceptionWhileReceivingEmptyRepoUrl() {

        String errMex = "Empty repo URL detected";

        // empty bundle id
        Exception exception = assertThrows(EntandoValidationException.class,
                () -> validator.validateOrThrow(""));
        assertThat(exception.getMessage()).isEqualTo(errMex);

        // null bundle id
        exception = assertThrows(EntandoValidationException.class, () -> validator.composeUrlForcingHttpProtocolOrThrow(null));
        assertThat(exception.getMessage()).isEqualTo(errMex);
    }

    @Test
    void shouldThrowExceptionWhileReceivingARepoUrlContainingAFragment() {

        String repoUrl = "http://github.com/entando/good-bundle#invalid";

        // empty repo url
        Exception exception = assertThrows(EntandoValidationException.class,
                () -> validator.validateOrThrow(repoUrl));
        assertThat(exception.getMessage()).isEqualTo(
                "The repo URL '" + repoUrl + "' contains a '#' char. This is not allowed");
    }

    @Test
    void shouldThrowExceptionWhileReceivingARepoUrlExceedingTheMaxAllowedLength() {

        String subpath = "repo-url-subpath-repo-url-subpath-repo-url-subpath";

        // build a repo url exceeding the max allowed length
        String repoUrl = "https://"
                + IntStream.range(
                        0, BundleRepositoryUrlValidator.STANDARD_REPO_URL_MAX_LENGTH
                                / BundleRepositoryUrlValidator.STANDARD_REPO_URL_MAX_SUBPATHS)
                .mapToObj(i -> subpath)
                .collect(Collectors.joining("/"));

        Exception exception = assertThrows(EntandoValidationException.class,
                () -> validator.validateOrThrow(repoUrl));
        assertThat(exception.getMessage()).isEqualTo(
                "The repo URL '" + repoUrl + "' exceeds the maximum length of "
                        + BundleRepositoryUrlValidator.STANDARD_REPO_URL_MAX_LENGTH);
    }

    @Test
    void shouldThrowExceptionWhileReceivingARepoUrlWithANumberOfSubpathsTooHigh() {

        String repoUrl = "https://"
                + IntStream.range(
                        0, BundleRepositoryUrlValidator.STANDARD_REPO_URL_MAX_SUBPATHS + 1)
                .mapToObj(i -> "subpath-" + i)
                .collect(Collectors.joining("/"));

        Exception exception = assertThrows(EntandoValidationException.class,
                () -> validator.validateOrThrow(repoUrl));
        assertThat(exception.getMessage()).isEqualTo(
                "The number of the repo URL's subpath (identified by splitting the id using the '/' char) exceeds the maximum length of "
                        + BundleRepositoryUrlValidator.STANDARD_REPO_URL_MAX_SUBPATHS);
    }

    @Test
    void shouldThrowExceptionWhileReceivingARepoUrlWithASubpathExceedingTheMaxAllowedLength() {

        String longSubpath = "my-long-subpath-my-long-subpath-my-long-subpath-my-long-subpath-my-long-subpath-my-long-s"
                + "ubpath-my-long-subpath-my-long-subpath-my-long-subpath-my-long-subpath-my-long-subpath-my-long-subpa"
                + "th-my-long-subpath-my-long-subpath-my-long-subpath-my-long-subpath-a";
        String repoUrl = "https://github.com/entando/" + longSubpath;

        Exception exception = assertThrows(EntandoValidationException.class,
                () -> validator.validateOrThrow(repoUrl));
        assertThat(exception.getMessage()).isEqualTo(
                "The subpath '" + longSubpath + "' exceeds the maximum allowed length of "
                        + BundleRepositoryUrlValidator.MAX_SUBPATH_LENGTH);
    }
}
