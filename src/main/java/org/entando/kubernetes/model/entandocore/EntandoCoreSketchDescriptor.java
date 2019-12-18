package org.entando.kubernetes.model.entandocore;

import lombok.Data;
import org.entando.kubernetes.model.bundle.descriptor.SketchDescriptor;

@Data
public class EntandoCoreSketchDescriptor {

    private int x1;
    private int y1;
    private int x2;
    private int y2;

    public EntandoCoreSketchDescriptor(final SketchDescriptor sketch) {
        this.x1 = Integer.parseInt(sketch.getX1());
        this.x2 = Integer.parseInt(sketch.getX2());
        this.y1 = Integer.parseInt(sketch.getY1());
        this.y2 = Integer.parseInt(sketch.getY2());
    }
}
