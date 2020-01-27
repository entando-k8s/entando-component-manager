package org.entando.kubernetes.model.bundle.descriptor;

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

}
