package org.entando.kubernetes.model.entandocore;

import static java.util.Optional.ofNullable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.entando.kubernetes.model.bundle.descriptor.FrameDescriptor;

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
