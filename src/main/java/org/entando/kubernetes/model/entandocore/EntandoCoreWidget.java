package org.entando.kubernetes.model.entandocore;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;

@Data
public class EntandoCoreWidget {

    private String code;
    private Map<String, String> titles;
    private String group;
    private String bundleId;
    private String customUi;
    private Map<String, Object> configUi;
    private List<WidgetParameter> parameters;
    private String parentType;
    private Map<String, String> config;

    public EntandoCoreWidget(final WidgetDescriptor descriptor) {
        this.code = descriptor.getCode();
        this.titles = descriptor.getTitles();
        this.group = descriptor.getGroup();
        this.customUi = descriptor.getCustomUi();
        if (descriptor.getConfigUi() != null) {
            this.configUi = new HashMap<>();
            this.configUi.put("customElement", descriptor.getConfigUi().getCustomElement());
            this.configUi.put("resources", descriptor.getConfigUi().getResources());
        }
        this.bundleId = descriptor.getDescriptorMetadata().getBundleCode();
        if (null != descriptor.getParameters()) {
            this.parameters = new ArrayList<>();
            descriptor.getParameters().entrySet().stream()
                    .forEach(e -> this.parameters.add(new WidgetParameter(e.getKey(), e.getValue())));
        }
        this.parentType = descriptor.getParentType();
        this.config = descriptor.getConfig();
    }

    public static class WidgetParameter {

        public WidgetParameter(String code, String description) {
            this.code = code;
            this.description = description;
        }

        private String code;
        private String description;

    }

}
