package org.entando.kubernetes.model.bundle.descriptor;

import lombok.Data;

import java.util.List;

@Data
public class PageModelConfiguration {

    private List<FrameDescriptor> frames;

}
