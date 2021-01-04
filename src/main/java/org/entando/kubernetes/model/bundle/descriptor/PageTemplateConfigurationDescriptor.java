package org.entando.kubernetes.model.bundle.descriptor;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageTemplateConfigurationDescriptor {

    @Default
    private List<FrameDescriptor> frames = new ArrayList<>();

}
