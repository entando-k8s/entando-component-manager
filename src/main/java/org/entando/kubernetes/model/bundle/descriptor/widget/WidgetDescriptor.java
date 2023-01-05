package org.entando.kubernetes.model.bundle.descriptor.widget;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.model.bundle.descriptor.ComponentKey;
import org.entando.kubernetes.model.bundle.descriptor.DescriptorVersion;
import org.entando.kubernetes.model.bundle.descriptor.VersionedDescriptor;
import org.entando.kubernetes.service.digitalexchange.templating.WidgetTemplateGeneratorService;
import org.entando.kubernetes.service.digitalexchange.templating.WidgetTemplateGeneratorService.SystemParams;
import org.springframework.util.ObjectUtils;

@Getter
@Setter
@Builder
@NoArgsConstructor
@Accessors(chain = true)
public class WidgetDescriptor extends VersionedDescriptor {

    private String code;
    private Map<String, String> titles;
    private String group;
    private String customUi;
    private String widgetCategory;

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
    private WidgetExt ext;
    private Map<String, String> paramsDefaults;

    public static final String TYPE_WIDGET_STANDARD = "widget";
    public static final String TYPE_WIDGET_CONFIG = "widget-config";
    public static final String TYPE_WIDGET_APPBUILDER = "app-builder";

    // ------------------------------------------------------------
    // METADATA
    private DescriptorMetadata descriptorMetadata = DescriptorMetadata.builder().build();
    private String parentName;
    private String parentCode;

    // TODO after ENG-4004 we could revert these changes since the descriptor version will be stored into a separate
    //  db col. look also at EntandoBundleWidgetServiceImpl.composeBaseAssetsPath()

    /**
     * deprecated constructor.
     * @deprecated this method is no longer acceptable since it doesn't set the descriptor version
     */
    @Deprecated
    public WidgetDescriptor(String code, Map<String, String> titles, String group,
            String customUi, String widgetCategory, ConfigUi configUi, String customUiPath, String configWidget, String name, String type,
            String configMfe, List<ApiClaim> apiClaims, List<Param> params, List<String> contextParams,
            String customElement, WidgetExt ext, Map<String, String> paramsDefaults,
            DescriptorMetadata descriptorMetadata, String parentName, String parentCode) {

        this(null, code, titles, group, customUi, widgetCategory, configUi, customUiPath, configWidget, name, type, configMfe,
                apiClaims, params, contextParams, customElement, ext, paramsDefaults, descriptorMetadata, parentName,
                parentCode);
    }

    public WidgetDescriptor(String descriptorVersion, String code, Map<String, String> titles, String group,
            String customUi, String widgetCategory, ConfigUi configUi, String customUiPath, String configWidget, String name, String type,
            String configMfe, List<ApiClaim> apiClaims, List<Param> params, List<String> contextParams,
            String customElement, WidgetExt ext, Map<String, String> paramsDefaults,
            DescriptorMetadata descriptorMetadata, String parentName, String parentCode) {
        super.setDescriptorVersion(descriptorVersion);
        this.code = code;
        this.titles = titles;
        this.group = group;
        this.customUi = customUi;
        this.widgetCategory = widgetCategory;
        this.configUi = configUi;
        this.customUiPath = customUiPath;
        this.configWidget = configWidget;
        this.name = name;
        this.type = type;
        this.configMfe = configMfe;
        this.apiClaims = apiClaims;
        this.params = params;
        this.contextParams = contextParams;
        this.customElement = customElement;
        this.ext = ext;
        this.paramsDefaults = paramsDefaults;
        this.descriptorMetadata = descriptorMetadata;
        this.parentName = parentName;
        this.parentCode = parentCode;
    }

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
    @Jacksonized
    @Builder
    @AllArgsConstructor
    public static class DescriptorMetadata {

        /**
         * key = plugin identifier.
         * value = plugin ingress path
         */
        private final Map<String, String> pluginIngressPathMap;
        private final String filename;
        private final String bundleCode;
        private final String[] assets;
        private final SystemParams systemParams;
        private final String bundleId;
        @JsonIgnore
        private final WidgetTemplateGeneratorService templateGeneratorService;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WidgetExt {

        @JsonDeserialize(using = KeepAsJsonDeserializer.class)
        @JsonRawValue
        private String appBuilder;
        @JsonDeserialize(using = KeepAsJsonDeserializer.class)
        @JsonRawValue
        private String adminConsole;
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
        return type != null && type.equals(WidgetDescriptor.TYPE_WIDGET_CONFIG);
    }
}
