package org.entando.kubernetes.model.bundle.descriptor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

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

}
