package org.entando.kubernetes.model.bundle.descriptor;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WidgetDescriptor implements Descriptor {

    private String code;
    private Map<String, String> titles;
    private String group;
    private String customUi;
    private String customUiPath;
    private ConfigUIDescriptor configUi;
    private String bundleId;

    @Getter
    @Setter
    public static class ConfigUIDescriptor {

        private String customElement;
        private List<String> resources;
    }

}
