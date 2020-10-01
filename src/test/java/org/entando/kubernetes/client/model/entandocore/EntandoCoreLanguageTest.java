package org.entando.kubernetes.client.model.entandocore;

import static org.assertj.core.api.Assertions.assertThat;

import org.entando.kubernetes.model.bundle.descriptor.LanguageDescriptor;
import org.entando.kubernetes.model.entandocore.EntandoCoreLanguage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class EntandoCoreLanguageTest {

    private final String langCode = "bo";
    private final String langDescription = "dead lang";
    private final boolean active = true;

    @Test
    void shouldReadDescriptorFile() {
        EntandoCoreLanguage ecLang = new EntandoCoreLanguage(getTestLanguageDescriptor());
        assertThat(ecLang.getCode()).isEqualTo(langCode);
        assertThat(ecLang.getDescription()).isEqualTo(langDescription);
        assertThat(ecLang.getIsActive()).isEqualTo(active);
    }

    private LanguageDescriptor getTestLanguageDescriptor() {

        return LanguageDescriptor.builder()
                .code(langCode)
                .description(langDescription)
                .isActive(active)
                .build();
    }
}
