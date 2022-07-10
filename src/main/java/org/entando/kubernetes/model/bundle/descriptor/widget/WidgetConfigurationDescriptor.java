package org.entando.kubernetes.model.bundle.descriptor.widget;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.entando.kubernetes.model.bundle.descriptor.ComponentKey;
import org.entando.kubernetes.model.bundle.descriptor.VersionedDescriptor;
import org.springframework.util.ObjectUtils;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WidgetConfigurationDescriptor extends VersionedDescriptor {

    private Integer pos;
    private String name;
    private String code;
    private Map<String, Object> config;
    
    @Override
    public ComponentKey getComponentKey() {
        return ObjectUtils.isEmpty(code)
                ? new ComponentKey(name) :
                new ComponentKey(code);
    }
    
    public WidgetConfigurationDescriptor setCode(String code) {
        this.code = code;
        return this;
    }
    
}
