package org.entando.kubernetes.model.entandocore;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EntandoCoreFolder {

    private boolean protectedFolder;
    private String path;

    public EntandoCoreFolder(final String path) {
        this.path = path;
        this.protectedFolder = false;
    }

}
