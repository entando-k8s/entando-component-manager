package org.entando.kubernetes.model.bundle.descriptor;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WidgetConfigurationDescriptor {
    private Integer pos;
    private String code;
    private Map<String, Object> config;
}
