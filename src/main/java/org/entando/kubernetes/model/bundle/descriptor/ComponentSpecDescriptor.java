package org.entando.kubernetes.model.bundle.descriptor;

import java.util.List;
import lombok.Data;

@Data
public class ComponentSpecDescriptor {

    private List<String> plugins;
    private List<String> widgets;
    private List<String> fragments;
    private List<String> pageModels;
    private List<String> contentTypes;
    private List<String> contentModels;
    private List<LabelDescriptor> labels;

}
