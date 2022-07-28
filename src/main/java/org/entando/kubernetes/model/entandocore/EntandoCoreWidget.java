package org.entando.kubernetes.model.entandocore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;

@Data
public class EntandoCoreWidget {

    public static final String GLOBAL_CONFIG_MFE_PREFIX = "global:";

    private String code;
    private Map<String, String> titles;
    private String group;
    private String bundleId;
    private String customUi;
    private Map<String, Object> configUi;
    private List<MfeParam> params;
    private String configMfe;
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
                    new MfeParam(p.getName(), p.getDescription())
            ).collect(Collectors.toList());
        }
        Optional.ofNullable(descriptor.getConfigMfe()).ifPresent(config -> 
                this.configMfe = (config.startsWith(GLOBAL_CONFIG_MFE_PREFIX) ? config.substring(GLOBAL_CONFIG_MFE_PREFIX.length()) : null)
        );
        this.parentCode = descriptor.getParentCode();
        this.paramsDefaults = descriptor.getParamsDefaults();
    }

    @Getter
    @AllArgsConstructor
    public static class MfeParam {
        private final String name;
        private final String description;
    }

}
