package org.entando.kubernetes.model.bundle.descriptor;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefaultWidgetDescriptor {

    private String code;
    private Map<String, String> properties;
}
