package org.entando.kubernetes.model.entandocore;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
public class EntandoCoreWidget {

    private static final Logger logger = LoggerFactory.getLogger(EntandoCoreWidget.class);

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
        this.configMfe = descriptor.getConfigMfe();
        this.parentCode = descriptor.getParentCode();
        this.paramsDefaults = descriptor.getParamsDefaults();
    }

    @Getter
    @AllArgsConstructor
    public static class MfeParam {
        final private String name;
        final private String description;
    }

}
