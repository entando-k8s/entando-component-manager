package org.entando.kubernetes.service.digitalexchange.entandocore.model;

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
