package org.entando.kubernetes.model.bundle.descriptor;

import lombok.Data;

@Data
public class FrameDescriptor {

    private String pos;
    private String description;
    private boolean mainFrame;
    private SketchDescriptor sketch;
    private DefaultWidgetDescriptor defaultWidget;

}
