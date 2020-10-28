package org.entando.kubernetes.model.bundle.descriptor;

import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.List;
import lombok.Data;

@Data
public class ComponentSpecDescriptor {

    private List<String> plugins;
    private List<String> widgets;
    private List<String> fragments;
    private List<String> categories;
    private List<String> pages;
    private List<String> pageTemplates;
    private List<String> contentTypes;
    private List<String> contentTemplates;
    private List<String> contents;
    private List<String> groups;
    private List<String> labels;
    private List<String> languages;

    @JsonSetter(value = "contentModels")
    private void setContentModels(List<String> contentModels) {
        this.contentTemplates = contentModels;
    }

    @JsonSetter(value = "pageModels")
    private void setPageModels(List<String> contentModels) {
        this.pageTemplates = contentModels;
    }
}
