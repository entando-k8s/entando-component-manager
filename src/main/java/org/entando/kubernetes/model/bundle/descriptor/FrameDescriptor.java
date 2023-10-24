package org.entando.kubernetes.model.bundle.descriptor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
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
