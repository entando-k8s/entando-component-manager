package org.entando.kubernetes.model.entandocore;

import java.util.Map;
import lombok.Data;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetConfigurationDescriptor;

@Data
public class EntandoCorePageWidgetConfiguration {

    private String code;
    private Map<String, Object> config;

    private EntandoCorePageWidgetConfiguration() {
    }

    public EntandoCorePageWidgetConfiguration(WidgetConfigurationDescriptor descriptor) {
        this.code = descriptor.getCode();
        this.config = descriptor.getConfig();
    }

}
