package org.entando.kubernetes.controller.digitalexchange.job.model;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisReport {
    @Default private Map<String, Status> widgets = new HashMap<>();
    @Default private Map<String, Status> fragments = new HashMap<>();
    @Default private Map<String, Status> pages = new HashMap<>();
    @Default private Map<String, Status> pageTemplates = new HashMap<>();
    @Default private Map<String, Status> contents = new HashMap<>();
    @Default private Map<String, Status> contentTemplates = new HashMap<>();
    @Default private Map<String, Status> contentTypes = new HashMap<>();
    @Default private Map<String, Status> assets = new HashMap<>();
    @Default private Map<String, Status> resources = new HashMap<>();
    @Default private Map<String, Status> plugins = new HashMap<>();
    @Default private Map<String, Status> categories = new HashMap<>();
    @Default private Map<String, Status> groups = new HashMap<>();
    @Default private Map<String, Status> labels = new HashMap<>();
    @Default private Map<String, Status> languages = new HashMap<>();

    public enum Status {
        NOT_FOUND,
        CONFLICT
    }
}
