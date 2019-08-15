package org.entando.kubernetes.service.digitalexchange.job.model;

import lombok.Data;

import java.util.List;

@Data
public class ComponentSpecDescriptor {

    private ServiceDescriptor service;
    private List<String> widgets;
    private List<String> pageModels;
    private List<String> contentTypes;
    private List<String> contentModels;

}
