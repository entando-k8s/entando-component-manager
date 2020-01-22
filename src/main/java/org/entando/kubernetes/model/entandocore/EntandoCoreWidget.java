package org.entando.kubernetes.model.entandocore;

import java.util.Map;
import lombok.Data;
import org.entando.kubernetes.model.bundle.descriptor.WidgetDescriptor;

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
