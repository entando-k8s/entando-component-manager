package org.entando.kubernetes.service.digitalexchange.job.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter@Setter
public class PageModelDescriptor extends Descriptor {

    private String code;
    private String description;
    private Map<String, String> titles;
    private String group;
    private String template;
    private String templatePath;
    private PageModelConfiguration configuration;

}
