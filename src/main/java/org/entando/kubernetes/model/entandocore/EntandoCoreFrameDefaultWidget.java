package org.entando.kubernetes.model.entandocore;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.entando.kubernetes.model.bundle.descriptor.DefaultWidgetDescriptor;

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
