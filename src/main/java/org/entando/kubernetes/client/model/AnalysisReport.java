package org.entando.kubernetes.client.model;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.entando.kubernetes.controller.digitalexchange.job.model.Status;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class AnalysisReport {

    @Default
    private Map<String, Status> widgets = new HashMap<>();
    @Default
    private Map<String, Status> fragments = new HashMap<>();
    @Default
    private Map<String, Status> pages = new HashMap<>();
    @Default
    private Map<String, Status> pageTemplates = new HashMap<>();
    @Default
    private Map<String, Status> contents = new HashMap<>();
    @Default
    private Map<String, Status> contentTemplates = new HashMap<>();
    @Default
    private Map<String, Status> contentTypes = new HashMap<>();
    @Default
    private Map<String, Status> assets = new HashMap<>();
    @Default
    private Map<String, Status> directories = new HashMap<>();
    @Default
    private Map<String, Status> resources = new HashMap<>();
    @Default
    private Map<String, Status> plugins = new HashMap<>();
    @Default
    private Map<String, Status> categories = new HashMap<>();
    @Default
    private Map<String, Status> groups = new HashMap<>();
    @Default
    private Map<String, Status> labels = new HashMap<>();
    @Default
    private Map<String, Status> languages = new HashMap<>();
}
