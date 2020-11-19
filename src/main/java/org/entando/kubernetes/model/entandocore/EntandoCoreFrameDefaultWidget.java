package org.entando.kubernetes.model.entandocore;

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

    public EntandoCoreFrameDefaultWidget(DefaultWidgetDescriptor defaultWidgetDescriptor) {
        this.code = defaultWidgetDescriptor.getCode();
    }
}
