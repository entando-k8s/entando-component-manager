package org.entando.kubernetes.validator;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.validator.routines.UrlValidator;
import org.entando.kubernetes.exception.EntandoValidationException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class ValidationFunctionsTest {

    private final UrlValidator urlValidator = new UrlValidator();
    private final String emptyMex = "empty";
    private final String invalidMex = "not valid";

    @Test
    void shouldThrowTheExpectedExceptionIfUrlIsEmpty() {

        final EntandoValidationException entandoValidationException = assertThrows(EntandoValidationException.class,
                () -> ValidationFunctions.validateUrlOrThrow(urlValidator, null, emptyMex, invalidMex));

        assertThat(entandoValidationException.getMessage()).isEqualTo(emptyMex);
    }

    @Test
    void shouldThrowTheExpectedExceptionIfUrlIsNotCompliant() throws MalformedURLException {

        final URL url = new URL("http://.com");

        final EntandoValidationException entandoValidationException = assertThrows(EntandoValidationException.class,
                () -> ValidationFunctions.validateUrlOrThrow(urlValidator, url, emptyMex, invalidMex));

        assertThat(entandoValidationException.getMessage()).isEqualTo(invalidMex);
    }

    @Test
    void shouldNotThrowExceptionWhenUrlIsCompliat() throws MalformedURLException {

        final URL url = new URL("http://www.entando.com");

        assertDoesNotThrow(() -> ValidationFunctions.validateUrlOrThrow(urlValidator, url, emptyMex, invalidMex));
    }
}
