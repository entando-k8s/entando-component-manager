package org.entando.kubernetes.service.digitalexchange.job.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter@Setter
public class WidgetDescriptor extends Descriptor {

    private String code;
    private Map<String, String> titles;
    private String group;
    private String customUi;
    private String customUiPath;

}
