package org.entando.kubernetes.model.bundle.descriptor;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FrameDescriptor {

    private String pos;
    private String description;
    private boolean mainFrame;
    private SketchDescriptor sketch;
    private DefaultWidgetDescriptor defaultWidget;

    public boolean hasDefaultWidget() {
        return this.defaultWidget != null;
    }

}
