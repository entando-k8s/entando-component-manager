package org.entando.kubernetes.validator;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URL;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.EntandoValidationException;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.stubhelper.BundleStubHelper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class ValidationFunctionsTest {

    private final String emptyMex = "empty";
    private final String invalidMex = "not valid";

    @Test
    void shouldThrowTheExpectedExceptionIfUrlIsEmpty() {

        final EntandoValidationException entandoValidationException = assertThrows(EntandoValidationException.class,
                () -> ValidationFunctions.composeUrlOrThrow(null, emptyMex, invalidMex));

        assertThat(entandoValidationException.getMessage()).isEqualTo(emptyMex);
    }

    @Test
    void shouldThrowTheExpectedExceptionIfUrlIsNotCompliant() {

        Stream.of("", "ftp://entando.com", "http://", "https://", "https://.com", "http://.com", "https://my-domain-",
                        "https://my-domain.", "http:// ", "http://com.", "http://.com")
                .forEach(urlString -> {
                    try {
                        assertThrows(EntandoValidationException.class,
                                () -> ValidationFunctions.composeUrlOrThrow(urlString, emptyMex, invalidMex),
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
                    URL url = ValidationFunctions.composeUrlOrThrow(urlString, emptyMex, invalidMex);
                    assertThat(url.toString()).isEqualTo(urlString);
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
            String actual = ValidationFunctions.composeUrlForcingHttpProtocolOrThrow(url, "null", "not valid");
            Assertions.assertThat(actual).isEqualTo(url);
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
                        () -> ValidationFunctions.composeUrlForcingHttpProtocolOrThrow(url, "null", "not valid")));
    }

    @Test
    void shouldCorrectlyValidateAnEntityCode() {
        assertThrows(EntandoComponentManagerException.class, () -> ValidationFunctions.validateEntityCodeOrThrow(null));
        assertThrows(EntandoComponentManagerException.class, () -> ValidationFunctions.validateEntityCodeOrThrow(""));
        assertThrows(EntandoComponentManagerException.class, () -> ValidationFunctions.validateEntityCodeOrThrow("code1234abcd"));
        assertThrows(EntandoComponentManagerException.class, () -> ValidationFunctions.validateEntityCodeOrThrow("-code1234abcd"));
        assertThrows(EntandoComponentManagerException.class, () -> ValidationFunctions.validateEntityCodeOrThrow("code1234abcd-"));
        assertThrows(EntandoComponentManagerException.class, () -> ValidationFunctions.validateEntityCodeOrThrow("code-1234abcm"));

        final String code = ValidationFunctions.validateEntityCodeOrThrow(BundleStubHelper.BUNDLE_CODE);
        assertThat(code).isEqualTo(BundleStubHelper.BUNDLE_CODE);
    }
}
