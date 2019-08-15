package org.entando.kubernetes.service.digitalexchange.entandocore.model;

import lombok.Data;
import org.entando.kubernetes.service.digitalexchange.job.model.FrameDescriptor;

import static java.util.Optional.ofNullable;

@Data
public class EntandoCoreFrameDescriptor {

    private String pos;
    private String descr;
    private boolean mainFrame;
    private EntandoCoreSketchDescriptor sketch;

    public EntandoCoreFrameDescriptor(final FrameDescriptor descriptor) {
        this.pos = descriptor.getPos();
        this.descr = descriptor.getDescription();
        this.mainFrame = descriptor.isMainFrame();
        this.sketch = ofNullable(descriptor.getSketch())
                .map(EntandoCoreSketchDescriptor::new).orElse(null);
    }

}
