package org.entando.kubernetes.model.bundle.descriptor;

import java.nio.file.Paths;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileDescriptor implements Descriptor {

    private String folder;
    private String filename;
    private String base64;

    @Override
    public ComponentKey getComponentKey() {
        return new ComponentKey(Paths.get(folder, filename).toString());
    }

}
