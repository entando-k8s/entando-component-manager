package org.entando.kubernetes.model.entandocore;

import java.util.Map;
import lombok.Data;
import org.entando.kubernetes.model.bundle.descriptor.PageModelDescriptor;

@Data
public class EntandoCorePageModel {

    private String code;
    private String descr;
    private Map<String, String> titles;
    private String template;
    private EntandoCorePageModelConfiguration configuration;

    public EntandoCorePageModel(final PageModelDescriptor descriptor) {
        this.code = descriptor.getCode();
        this.descr = descriptor.getDescription();
        this.titles = descriptor.getTitles();
        this.template = descriptor.getTemplate();
        this.configuration = new EntandoCorePageModelConfiguration(descriptor.getConfiguration());
    }

}
