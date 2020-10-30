package org.entando.kubernetes.controller.digitalexchange.job.model;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallRequest.InstallAction;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallActionsByComponentType {
    @Default private Map<String, InstallAction> widgets = new HashMap<>();
    @Default private Map<String, InstallAction> fragments = new HashMap<>();
    @Default private Map<String, InstallAction> pages = new HashMap<>();
    @Default private Map<String, InstallAction> pageTemplates = new HashMap<>();
    @Default private Map<String, InstallAction> contents = new HashMap<>();
    @Default private Map<String, InstallAction> contentTemplates = new HashMap<>();
    @Default private Map<String, InstallAction> contentTypes = new HashMap<>();
    @Default private Map<String, InstallAction> assets = new HashMap<>();
    @Default private Map<String, InstallAction> resources = new HashMap<>();
    @Default private Map<String, InstallAction> plugins = new HashMap<>();
    @Default private Map<String, InstallAction> categories = new HashMap<>();
    @Default private Map<String, InstallAction> groups = new HashMap<>();
    @Default private Map<String, InstallAction> labels = new HashMap<>();
    @Default private Map<String, InstallAction> languages = new HashMap<>();
}
