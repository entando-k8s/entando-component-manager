package org.entando.kubernetes.model.bundle.descriptor;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FrameDescriptor {

    private String pos;
    private String description;
    private boolean mainFrame;
    private SketchDescriptor sketch;

}
