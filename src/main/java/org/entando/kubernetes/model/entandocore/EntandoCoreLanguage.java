package org.entando.kubernetes.model.entandocore;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.entando.kubernetes.model.bundle.descriptor.LanguageDescriptor;

@Data
@Accessors(chain = true)
@NoArgsConstructor
public class EntandoCoreLanguage {

    private String code;
    private String description;

    @Getter(AccessLevel.NONE)
    private boolean isActive;

    public boolean getIsActive() {
        return isActive;
    }

    public EntandoCoreLanguage(LanguageDescriptor descriptor) {
        this.code = descriptor.getCode();
        this.description = descriptor.getDescription();
        this.isActive = descriptor.getIsActive();
    }
}
