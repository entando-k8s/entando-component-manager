package org.entando.kubernetes.model.entandocore;

import java.nio.file.Path;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EntandoCoreFile {

    private boolean protectedFolder;
    private String path;
    private String filename;
    private String base64;

}
