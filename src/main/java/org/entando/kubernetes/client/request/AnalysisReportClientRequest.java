package org.entando.kubernetes.client.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class AnalysisReportClientRequest {

    List<String> widgets;
    List<String> fragments;
    List<String> pages;
    List<String> pageTemplates;
    List<String> contents;
    List<String> contentTemplates;
    List<String> contentTypes;
    List<String> assets;
    List<String> resources;
    List<String> plugins;
    List<String> categories;
    List<String> groups;
    List<String> labels;
    List<String> languages;
    List<String> directories;
}
