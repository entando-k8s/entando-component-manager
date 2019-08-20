package org.entando.kubernetes.service.digitalexchange.job.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter@Setter
public class ContentTypeDescriptor extends Descriptor {

    private String code;
    private String name;
    private String status;

    private List<ContentTypeAttribute> attributes;

}
