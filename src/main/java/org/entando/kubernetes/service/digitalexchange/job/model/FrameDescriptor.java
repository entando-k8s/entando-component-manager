package org.entando.kubernetes.service.digitalexchange.job.model;

import lombok.Data;

@Data
public class FrameDescriptor {

    private String pos;
    private String description;
    private boolean mainFrame;
    private SketchDescriptor sketch;

}
