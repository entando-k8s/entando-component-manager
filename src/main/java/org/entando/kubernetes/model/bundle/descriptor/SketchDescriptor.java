package org.entando.kubernetes.model.bundle.descriptor;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SketchDescriptor {

    private int x1;
    private int y1;
    private int x2;
    private int y2;

}
