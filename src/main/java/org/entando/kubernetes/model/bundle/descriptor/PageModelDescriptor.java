package org.entando.kubernetes.model.bundle.descriptor;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter@Setter
public class PageModelDescriptor implements Descriptor {

    private String code;
    private String description;
    private Map<String, String> titles;
    private String group;
    private String template;
    private String templatePath;
    private PageModelConfiguration configuration;

}
