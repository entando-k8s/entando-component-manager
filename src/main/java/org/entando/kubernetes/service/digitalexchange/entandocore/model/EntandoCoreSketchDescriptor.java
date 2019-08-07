package org.entando.kubernetes.service.digitalexchange.entandocore.model;

import lombok.Data;
import org.entando.kubernetes.service.digitalexchange.job.model.SketchDescriptor;

@Data
public class EntandoCoreSketchDescriptor {

    private int x1;
    private int y1;
    private int x2;
    private int y2;

    public EntandoCoreSketchDescriptor(final SketchDescriptor sketch) {
        this.x1 = sketch.getX1();
        this.x2 = sketch.getX2();
        this.y1 = sketch.getY1();
        this.y2 = sketch.getY2();
    }
}
