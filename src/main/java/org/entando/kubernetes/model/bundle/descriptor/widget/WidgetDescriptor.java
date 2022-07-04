package org.entando.kubernetes.model.bundle.descriptor.widget;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.entando.kubernetes.model.bundle.descriptor.ComponentKey;
import org.entando.kubernetes.model.bundle.descriptor.VersionedDescriptor;
import org.entando.kubernetes.model.bundle.processor.WidgetProcessor;
import org.entando.kubernetes.service.digitalexchange.templating.WidgetTemplateGeneratorService;
import org.springframework.util.ObjectUtils;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class WidgetDescriptor extends VersionedDescriptor {

    private String code;
    private Map<String, String> titles;
    private String group;
    private String customUi;

    // ------------------------------------------------------------
    // Version 1

    private ConfigUi configUi;
    private String customUiPath;
    private String configWidget;

    // ------------------------------------------------------------
    // Version 5
    private String name;
    private String type;
    private String configMfe;
    private List<ApiClaim> apiClaims;
    private List<Param> params;
    private List<String> contextParams;
    private String customElement;

    public static final String TYPE_WIDGET_STANDARD = "widget";
    public static final String TYPE_WIDGET_CONFIG = "widget-config";
    public static final String TYPE_WIDGET_APPBUILDER = "app-builder";

    // ------------------------------------------------------------
    // METADATA
    private DescriptorMetadata descriptorMetadata = new DescriptorMetadata(null, null, null, null, null);
    private String parentName;
    private String parentCode;

    // ------------------------------------------------------------
    @Override
    public ComponentKey getComponentKey() {
        return ObjectUtils.isEmpty(code)
                ? new ComponentKey(name) :
                new ComponentKey(code);
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ConfigUi {

        private String customElement;
        private List<String> resources;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ApiClaim {

        public static final String INTERNAL_API = "internal";
        public static final String EXTERNAL_API = "external";

        private String name;
        private String type;
        private String pluginName;
        private String bundleId;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Param {

        private String name;
        private String description;
    }

    @Getter
    @AllArgsConstructor
    public static class DescriptorMetadata {

        /**
         * key = plugin identifier.
         * value = plugin ingress path
         */
        private final Map<String, String> pluginIngressPathMap;
        private final String filename;
        private final String bundleCode;
        private final String bundleId;
        private final WidgetTemplateGeneratorService templateGeneratorService;
    }

    public WidgetDescriptor setCode(String code) {
        this.code = code;
        return this;
    }

    public void applyFallbacks() {
        if (getType() == null) {
            setType(WidgetDescriptor.TYPE_WIDGET_STANDARD);
        }
    }

    @Override
    public boolean isAuxiliary() {
        if (isVersion1()) {
            return false;
        }
        return type != null && !type.equals(WidgetDescriptor.TYPE_WIDGET_STANDARD);
    }
}
