package org.entando.kubernetes.service.digitalexchange.job.model;

import lombok.Getter;
import lombok.Setter;

@Getter@Setter
public class ComponentDescriptor extends Descriptor {

    private String code;
    private String description;

    private ComponentSpecDescriptor components;

}
