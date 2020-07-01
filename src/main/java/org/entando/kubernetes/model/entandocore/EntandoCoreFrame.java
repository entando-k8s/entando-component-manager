package org.entando.kubernetes.model.entandocore;

import static java.util.Optional.ofNullable;

import lombok.*;
import org.entando.kubernetes.model.bundle.descriptor.FrameDescriptor;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntandoCoreFrame {

    private String pos;
    private String descr;
    private boolean mainFrame;
    private EntandoCoreSketchDescriptor sketch;
    private EntandoCoreFrameDefaultWidget defaultWidget;

    public EntandoCoreFrame(final FrameDescriptor descriptor) {
        this.pos = descriptor.getPos();
        this.descr = descriptor.getDescription();
        this.mainFrame = descriptor.isMainFrame();
        this.sketch = ofNullable(descriptor.getSketch())
                .map(EntandoCoreSketchDescriptor::new).orElse(null);
        if (descriptor.hasDefaultWidget()) {
            this.defaultWidget = new EntandoCoreFrameDefaultWidget(descriptor.getDefaultWidget());
        }
    }

}
