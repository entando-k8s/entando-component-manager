package org.entando.kubernetes.model.entandocore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;

@Data
public class EntandoCoreWidget {

    private String code;
    private Map<String, String> titles;
    private String group;
    private String bundleId;
    private String customUi;
    private Map<String, Object> configUi;
    private List<WidgetParameter> params;
    private String parentCode;
    private Map<String, String> paramsDefaults;

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
        if (null != descriptor.getParams()) {
            this.params = descriptor.getParams().stream().map(p ->
                    new WidgetParameter(p.getName(), p.getDescription())
            ).collect(Collectors.toList());
        }
        this.parentCode = descriptor.getParentCode();
        this.paramsDefaults = descriptor.getParamsDefaults();
    }

    @Getter
    public static class WidgetParameter {

        public WidgetParameter(String name, String description) {
            this.name = name;
            this.description = description;
        }

        private String name;
        private String description;

    }

}
