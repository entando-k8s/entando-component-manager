package org.entando.kubernetes.service.digitalexchange.job.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter@Setter
public class ContentModelDescriptor extends Descriptor {

    private String id;
    private String contentType;
    private String description;
    private String contentShape;
    private String contentShapePath;

}
