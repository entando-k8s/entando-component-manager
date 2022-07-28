package org.entando.kubernetes.model.entandocore;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.processor.WidgetProcessor;

@Data
public class EntandoCoreWidget {

    private String code;
    private Map<String, String> titles;
    private String group;
    private String bundleId;
    private String customUi;
    private Map<String, Object> configUi;

    public EntandoCoreWidget(final WidgetDescriptor descriptor) {
        this.code = descriptor.getCode();
        this.titles = descriptor.getTitles();
        this.group = descriptor.getGroup();
        this.customUi = descriptor.getCustomUi();
        if (descriptor.getConfigUi() != null) {
            this.configUi = new HashMap<>();
            this.configUi.put("customElement", descriptor.getConfigUi().getCustomElement()
                    .replaceFirst(WidgetProcessor.CONFIG_WIDGET_GLOBAL_PREFIX, ""));
            this.configUi.put("resources", descriptor.getConfigUi().getResources());
        }
        this.bundleId = descriptor.getDescriptorMetadata().getBundleCode();
    }


}
