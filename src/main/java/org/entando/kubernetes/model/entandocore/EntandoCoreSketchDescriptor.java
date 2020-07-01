package org.entando.kubernetes.model.entandocore;

import lombok.*;
import org.entando.kubernetes.model.bundle.descriptor.SketchDescriptor;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
