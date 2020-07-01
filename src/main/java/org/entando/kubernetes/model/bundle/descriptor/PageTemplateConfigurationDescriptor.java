package org.entando.kubernetes.model.bundle.descriptor;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageTemplateConfigurationDescriptor {

    private List<FrameDescriptor> frames;

}
