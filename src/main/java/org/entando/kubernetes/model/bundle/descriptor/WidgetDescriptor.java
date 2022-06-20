package org.entando.kubernetes.model.bundle.descriptor;

import java.util.List;
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
public class WidgetDescriptor implements Descriptor {

    private String code;
    private Map<String, String> titles;
    private String group;
    private String customUi;
    private String customUiPath;
    private ConfigUIDescriptor configUi;
    private String bundleId;
    private List<WidgetParameter> params;
    private String configMfe;
    private String parentCode;
    private Map<String, String> paramsDefaults;

    @Override
    public ComponentKey getComponentKey() {
        return new ComponentKey(code);
    }

    @Getter
    @Setter
    public static class ConfigUIDescriptor {
        private String customElement;
        private List<String> resources;
    }
    
    @Getter
    @Setter
    public static class WidgetParameter {
        
        public WidgetParameter(String name, String description) {
            this.name = name;
            this.description = description;
        }
        
        private String name;
        private String description;
        
    }
    
}
