package org.entando.kubernetes.model.entandocore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Data;
import org.entando.kubernetes.model.bundle.descriptor.PageTemplateConfigurationDescriptor;

@Data
public class EntandoCorePageTemplateConfiguration {

    private List<EntandoCoreFrame> frames;

    public EntandoCorePageTemplateConfiguration(final PageTemplateConfigurationDescriptor descriptor) {
        this.frames = Optional.ofNullable(descriptor)
                .map(PageTemplateConfigurationDescriptor::getFrames)
                .orElseGet(ArrayList::new)
                .stream()
                .map(EntandoCoreFrame::new)
                .collect(Collectors.toList());
    }

}
