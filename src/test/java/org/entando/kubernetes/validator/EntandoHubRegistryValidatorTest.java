package org.entando.kubernetes.validator;

import static org.assertj.core.api.Java6Assertions.assertThat;

import java.net.URL;
import org.entando.kubernetes.exception.EntandoValidationException;
import org.entando.kubernetes.model.entandohub.EntandoHubRegistry;
import org.entando.kubernetes.stubhelper.EntandoHubRegistryStubHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class EntandoHubRegistryValidatorTest {

    private final EntandoHubRegistryValidator validator = new EntandoHubRegistryValidator();

    @Test
    void shouldSuccessfullyValidateACorrectlyPopulatedRegistry() {
        final EntandoHubRegistry registry = EntandoHubRegistryStubHelper.stubEntandoHubRegistry1();

        // validate ID too
        assertThat(validator.validateEntandoHubRegistryOrThrow(registry, true)).isTrue();

        // validate registry with empty ID
        registry.setId("");
        assertThat(validator.validateEntandoHubRegistryOrThrow(registry, false)).isTrue();
        assertThat(validator.validateEntandoHubRegistryOrThrow(registry, false)).isTrue();
        // validate registry with null ID
        registry.setId(null);
        assertThat(validator.validateEntandoHubRegistryOrThrow(registry, false)).isTrue();
        assertThat(validator.validateEntandoHubRegistryOrThrow(registry, false)).isTrue();
    }

    @Test
    void shouldThrowExceptionIfTheRegistryIsWronglyPopulated() throws Exception {
        final EntandoHubRegistry registry = EntandoHubRegistryStubHelper.stubEntandoHubRegistry1();

        // empty name
        registry.setName("");
        Assertions.assertThrows(EntandoValidationException.class,
                () -> validator.validateEntandoHubRegistryOrThrow(registry, false));
        // null name
        registry.setName(null);
        Assertions.assertThrows(EntandoValidationException.class,
                () -> validator.validateEntandoHubRegistryOrThrow(registry, false));

        registry.setName(EntandoHubRegistryStubHelper.REGISTRY_NAME_1);

        // null url
        registry.setUrl(null);
        Assertions.assertThrows(EntandoValidationException.class,
                () -> validator.validateEntandoHubRegistryOrThrow(registry, false));

        // not compliant url
        registry.setUrl(new URL("http://.com"));
        Assertions.assertThrows(EntandoValidationException.class,
                () -> validator.validateEntandoHubRegistryOrThrow(registry, false));

        registry.setUrl(new URL(EntandoHubRegistryStubHelper.REGISTRY_URL_STRING_1));

        // empty id
        registry.setId("");
        Assertions.assertThrows(EntandoValidationException.class,
                () -> validator.validateEntandoHubRegistryOrThrow(registry, true));

        // null id
        registry.setId(null);
        Assertions.assertThrows(EntandoValidationException.class,
                () -> validator.validateEntandoHubRegistryOrThrow(registry, true));
    }
}
