package org.entando.kubernetes.model.bundle.descriptor;

import lombok.Data;

import java.util.Map;

@Data
public class DefaultWidgetDescriptor {

    private String code;
    private Map<String, String> properties;
}
