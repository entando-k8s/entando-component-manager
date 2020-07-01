package org.entando.kubernetes.model.bundle.descriptor;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefaultWidgetDescriptor {

    private String code;
    private Map<String, String> properties;
}
