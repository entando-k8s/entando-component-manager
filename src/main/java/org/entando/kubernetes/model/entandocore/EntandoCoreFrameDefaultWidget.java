package org.entando.kubernetes.model.entandocore;

import lombok.*;
import org.entando.kubernetes.model.bundle.descriptor.DefaultWidgetDescriptor;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntandoCoreFrameDefaultWidget {
    String code;
    Map<String, String> properties;

    public EntandoCoreFrameDefaultWidget(DefaultWidgetDescriptor defaultWidgetDescriptor) {
        this.code = defaultWidgetDescriptor.getCode();
        this.properties = defaultWidgetDescriptor.getProperties();
    }
}
