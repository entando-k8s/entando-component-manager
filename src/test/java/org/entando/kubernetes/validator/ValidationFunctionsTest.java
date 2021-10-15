package org.entando.kubernetes.validator;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.MalformedURLException;
import java.net.URL;
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

        Stream.of("ftp://entando.com", "http://", "https://", "https://my-domain.", "https://my-domain-",
                        "https://.com", "http://.com", "http://lo&calhost", "https://my-domain/?myparam=value",
                        "https://my-domain/?myparam=value&seconp=myval", "http://www.enta-ndo.com:092/3-",
                        "http://www.enta-ndo.com:092/3.", "http://www.enta-ndo.com:123456")
                .forEach(urlString -> {

                    try {
                        URL url = new URL(urlString);
                        assertThrows(EntandoValidationException.class,
                                () -> ValidationFunctions.validateUrlOrThrow(url, emptyMex, invalidMex),
                                urlString);
                    } catch (MalformedURLException e) {
                        fail(e.getMessage());
                    }
                });
    }

    @Test
    void shouldNotThrowExceptionWhenUrlIsCompliant() {

        Stream.of("http://localhost", "https://localhost", "http://www.entando.com", "https://www.entando.com",
                        "http://my-domain", "https://my-domain", "https://my-domain:80", "http://www.entando.com:80",
                        "http://my-DDDdomain", "http://my-DDDdomain", "http://www.enta-ndo.com:092/3-4/ci-on.e/asdk-a/",
                        "http://www.en_ta-ndo.com:092/3_a/")
                .forEach(urlString -> {

                    try {
                        URL url = new URL(urlString);
                        assertDoesNotThrow(
                                () -> ValidationFunctions.validateUrlOrThrow(url, emptyMex, invalidMex));
                    } catch (MalformedURLException e) {
                        fail(e.getMessage());
                    }
                });
    }
}
