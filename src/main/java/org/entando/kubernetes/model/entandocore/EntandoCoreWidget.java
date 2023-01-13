package org.entando.kubernetes.model.entandocore;

import static org.entando.kubernetes.service.digitalexchange.BundleUtilities.GLOBAL_PREFIX;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.util.Strings;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;

@Data
public class EntandoCoreWidget {

    private String code;
    private Map<String, String> titles;
    private String group;
    private String bundleId;
    private String customUi;
    private Map<String, Object> configUi;
    private List<MfeParam> params;
    private String configUiName;
    private String parentCode;
    private Map<String, String> paramsDefaults;
    private String widgetCategory;

    public EntandoCoreWidget(final WidgetDescriptor descriptor) {
        this.code = descriptor.getCode();
        this.titles = descriptor.getTitles();
        this.group = descriptor.getGroup();
        this.customUi = descriptor.getCustomUi();
        buildAndSetConfigUiData(descriptor);
        this.bundleId = descriptor.getDescriptorMetadata().getBundleCode();
        if (null != descriptor.getParams()) {
            this.params = descriptor.getParams().stream().map(p ->
                    new MfeParam(p.getName(), p.getDescription())
            ).collect(Collectors.toList());
        }
        this.parentCode = descriptor.getParentCode();
        this.paramsDefaults = descriptor.getParamsDefaults();
        this.widgetCategory = descriptor.getWidgetCategory();
    }

    private void buildAndSetConfigUiData(WidgetDescriptor descriptor) {
        String configMfe = descriptor.getConfigMfe();
        this.configUiName = null;
        if (!Strings.isBlank(configMfe) && configMfe.startsWith(GLOBAL_PREFIX)) {
            this.configUiName = configMfe.substring(GLOBAL_PREFIX.length());
        } else if (descriptor.getConfigUi() != null) {
            this.configUi = new HashMap<>();
            this.configUi.put("customElement", descriptor.getConfigUi().getCustomElement());
            this.configUi.put("resources", descriptor.getConfigUi().getResources());
        }
    }

    @Getter
    @AllArgsConstructor
    public static class MfeParam {

        private final String name;
        private final String description;
    }

}
