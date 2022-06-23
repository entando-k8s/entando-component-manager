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
    private String bundleId;

    // ------------------------------------------------------------
    // Version 1

    private ConfigUIDescriptor configUi;
    private String customUiPath;

    // ------------------------------------------------------------
    // Version 5

    private String name;
    private String configWidget;
    private String customElement;
    private List<ApiClaim> apiClaims;
    private DescriptorMetadata descriptorMetadata;
    private String parentName;
    private String parentCode;


    @Override
    public ComponentKey getComponentKey() {
        return ObjectUtils.isEmpty(code)
                ? new ComponentKey(name) :
                new ComponentKey(code);
    }

    @Getter
    @Setter
    public static class ConfigUIDescriptor {

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
    @AllArgsConstructor
    public static class DescriptorMetadata {

        /**
         * key = plugin identifier.
         * value = plugin ingress path
         */
        private final Map<String, String> pluginIngressPathMap;
    }

    public WidgetDescriptor setCode(String code) {
        this.code = code;
        return this;
    }
}
