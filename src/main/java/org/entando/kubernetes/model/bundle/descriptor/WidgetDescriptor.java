package org.entando.kubernetes.model.bundle.descriptor;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter@Setter
public class WidgetDescriptor implements Descriptor {

    private String code;
    private Map<String, String> titles;
    private String group;
    private String customUi;
    private String customUiPath;
    private ConfigUIDescriptor configUi;
    private String bundleId;

    @Getter@Setter
    public static class ConfigUIDescriptor {
        private String customElement;
        private List<String> resources;
    }

}
