package org.entando.kubernetes.model.bundle.descriptor.widget;

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
public class WidgetConfigurationDescriptor {

    private Integer pos;
    private String name;
    private String code;
    private Map<String, Object> config;
    
    public WidgetConfigurationDescriptor setCode(String code) {
        this.code = code;
        return this;
    }
    
}
