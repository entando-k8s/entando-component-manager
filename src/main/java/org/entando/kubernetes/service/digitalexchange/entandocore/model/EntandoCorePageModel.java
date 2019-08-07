package org.entando.kubernetes.service.digitalexchange.entandocore.model;

import lombok.Data;
import org.entando.kubernetes.service.digitalexchange.job.model.PageModelDescriptor;

import java.util.Map;

@Data
public class EntandoCorePageModel {

    private String code;
    private String descr;
    private Map<String, String> titles;
    private String group;
    private String template;
    private EntandoCorePageModelConfiguration configuration;

    public EntandoCorePageModel(final PageModelDescriptor descriptor) {
        this.code = descriptor.getCode();
        this.descr = descriptor.getDescription();
        this.titles = descriptor.getTitles();
        this.group = descriptor.getGroup();
        this.template = descriptor.getTemplate();
        this.configuration = new EntandoCorePageModelConfiguration(descriptor.getConfiguration());
    }

}
