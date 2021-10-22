package org.entando.kubernetes.validator;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.stream.Stream;
import org.entando.kubernetes.exception.EntandoValidationException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class ValidationFunctionsTest {

    private final String emptyMex = "empty";
    private final String invalidMex = "not valid";

    @Test
    void shouldThrowTheExpectedExceptionIfUrlIsEmpty() {

        final EntandoValidationException entandoValidationException = assertThrows(EntandoValidationException.class,
                () -> ValidationFunctions.validateUrlOrThrow(null, emptyMex, invalidMex));

        assertThat(entandoValidationException.getMessage()).isEqualTo(emptyMex);
    }

    @Test
    void shouldThrowTheExpectedExceptionIfUrlIsNotCompliant() {

        Stream.of("ftp://entando.com", "http://", "https://", "https://.com", "http://.com", "https://my-domain-",
                        "https://my-domain.", "http:// ", "http://com.", "http://.com")
                .forEach(urlString -> {
                    try {
                        assertThrows(EntandoValidationException.class,
                                () -> ValidationFunctions.validateUrlOrThrow(urlString, emptyMex, invalidMex),
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

                    try {
                        assertDoesNotThrow(
                                () -> ValidationFunctions.validateUrlOrThrow(urlString, emptyMex, invalidMex));
                    } catch (Exception e) {
                        fail(e.getMessage());
                    }
                });
    }
}
