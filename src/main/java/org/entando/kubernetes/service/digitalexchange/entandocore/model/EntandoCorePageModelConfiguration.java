package org.entando.kubernetes.service.digitalexchange.entandocore.model;

import lombok.Data;
import org.entando.kubernetes.service.digitalexchange.job.model.PageModelConfiguration;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class EntandoCorePageModelConfiguration {

    private List<EntandoCoreFrameDescriptor> frames;

    public EntandoCorePageModelConfiguration(final PageModelConfiguration descriptor) {
        this.frames = descriptor.getFrames().stream()
                .map(EntandoCoreFrameDescriptor::new)
                .collect(Collectors.toList());
    }

}
