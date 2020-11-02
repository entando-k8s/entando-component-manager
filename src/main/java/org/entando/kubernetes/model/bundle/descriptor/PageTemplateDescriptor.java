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
public class PageTemplateDescriptor implements Descriptor {

    private String code;
    private String description;
    private Map<String, String> titles;
    private String template;
    private String templatePath;
    private PageTemplateConfigurationDescriptor configuration;

    @Override
    public ComponentKey getComponentKey() {
        return new ComponentKey(code);
    }
}
