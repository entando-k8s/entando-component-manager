package org.entando.kubernetes.model.bundle.descriptor;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileDescriptor implements Descriptor{

    private String folder;
    private String filename;
    private String base64;

}
