package org.entando.kubernetes.model.entandocore;

import java.util.Map;
import lombok.Data;
import org.entando.kubernetes.model.bundle.descriptor.PageTemplateDescriptor;

@Data
public class EntandoCorePageTemplate {

    private String code;
    private String descr;
    private Map<String, String> titles;
    private String template;
    private EntandoCorePageTemplateConfiguration configuration;

    public EntandoCorePageTemplate(final PageTemplateDescriptor descriptor) {
        this.code = descriptor.getCode();
        this.descr = descriptor.getDescription();
        this.titles = descriptor.getTitles();
        this.template = descriptor.getTemplate();
        this.configuration = new EntandoCorePageTemplateConfiguration(descriptor.getConfiguration());
    }

}
