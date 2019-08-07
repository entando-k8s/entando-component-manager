package org.entando.kubernetes.service.digitalexchange.entandocore.model;

import lombok.Data;
import org.entando.kubernetes.service.digitalexchange.job.model.WidgetDescriptor;

import java.util.Map;

@Data
public class EntandoCoreWidget {

    private String code;
    private Map<String, String> titles;
    private String group;
    private String customUi;

    public EntandoCoreWidget(final WidgetDescriptor descriptor) {
        this.code = descriptor.getCode();
        this.titles = descriptor.getTitles();
        this.group = descriptor.getGroup();
        this.customUi = descriptor.getCustomUi();
    }

}
