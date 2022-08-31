package org.entando.kubernetes.client.request;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.reportable.Reportable;
import org.entando.kubernetes.model.bundle.reportable.Reportable.Component;
import org.springframework.util.CollectionUtils;

public final class AnalysisReportClientRequestFactory {

    private EnumMap<ComponentType, Consumer<List<String>>> strategy;

    List<String> widgets = new ArrayList<>();
    List<String> fragments = new ArrayList<>();
    List<String> pages = new ArrayList<>();
    List<String> pageTemplates = new ArrayList<>();
    List<String> contents = new ArrayList<>();
    List<String> contentTemplates = new ArrayList<>();
    List<String> contentTypes = new ArrayList<>();
    List<String> assets = new ArrayList<>();
    List<String> directories = new ArrayList<>();
    List<String> resources = new ArrayList<>();
    List<String> plugins = new ArrayList<>();
    List<String> categories = new ArrayList<>();
    List<String> groups = new ArrayList<>();
    List<String> labels = new ArrayList<>();
    List<String> languages = new ArrayList<>();

    private AnalysisReportClientRequestFactory() {
        strategy = new EnumMap<>(ComponentType.class);
        strategy.put(ComponentType.WIDGET, this::widgets);
        strategy.put(ComponentType.FRAGMENT, this::fragments);
        strategy.put(ComponentType.PAGE, this::pages);
        strategy.put(ComponentType.PAGE_TEMPLATE, this::pageTemplates);
        strategy.put(ComponentType.CONTENT, this::contents);
        strategy.put(ComponentType.CONTENT_TEMPLATE, this::contentTemplates);
        strategy.put(ComponentType.CONTENT_TYPE, this::contentTypes);
        strategy.put(ComponentType.ASSET, this::assets);
        strategy.put(ComponentType.DIRECTORY, this::directories);
        strategy.put(ComponentType.RESOURCE, this::resources);
        strategy.put(ComponentType.PLUGIN, this::plugins);
        strategy.put(ComponentType.CATEGORY, this::categories);
        strategy.put(ComponentType.GROUP, this::groups);
        strategy.put(ComponentType.LABEL, this::labels);
        strategy.put(ComponentType.LANGUAGE, this::languages);
    }

    public static AnalysisReportClientRequestFactory anAnalysisReportClientRequest() {
        return new AnalysisReportClientRequestFactory();
    }

    public AnalysisReportClientRequestFactory reportableList(List<Reportable> reportableList) {

        if (null != reportableList) {
            reportableList.forEach(reportable -> {
                if (!CollectionUtils.isEmpty(reportable.getComponents())) {
                    List<String> compCodes = reportable.getComponents().stream()
                            .map(Component::getCode)
                            .collect(Collectors.toList());
                    strategy.get(reportable.getComponentType()).accept(compCodes);
                }
            });
        }

        return this;
    }

    public AnalysisReportClientRequest createEngineAnalysisReportRequest() {
        return new EngineAnalysisReportClientRequest(widgets, fragments, pages, pageTemplates, directories, resources,
                categories, groups, labels, languages);
    }

    public AnalysisReportClientRequest createCMSAnalysisReportRequest() {
        return new CMSAnalysisReportClientRequest(contents, contentTemplates, contentTypes, assets);
    }

    private AnalysisReportClientRequestFactory widgets(List<String> widgets) {
        this.widgets.addAll(widgets);
        return this;
    }

    private AnalysisReportClientRequestFactory fragments(List<String> fragments) {
        this.fragments.addAll(fragments);
        return this;
    }

    private AnalysisReportClientRequestFactory pages(List<String> pages) {
        this.pages.addAll(pages);
        return this;
    }

    private AnalysisReportClientRequestFactory pageTemplates(List<String> pageTemplates) {
        this.pageTemplates.addAll(pageTemplates);
        return this;
    }

    private AnalysisReportClientRequestFactory contents(List<String> contents) {
        this.contents.addAll(contents);
        return this;
    }

    private AnalysisReportClientRequestFactory contentTemplates(List<String> contentTemplates) {
        this.contentTemplates.addAll(contentTemplates);
        return this;
    }

    private AnalysisReportClientRequestFactory contentTypes(List<String> contentTypes) {
        this.contentTypes.addAll(contentTypes);
        return this;
    }

    private AnalysisReportClientRequestFactory assets(List<String> assets) {
        this.assets.addAll(assets);
        return this;
    }

    private AnalysisReportClientRequestFactory directories(List<String> directories) {
        this.directories.addAll(directories);
        return this;
    }

    private AnalysisReportClientRequestFactory resources(List<String> resources) {
        this.resources.addAll(resources);
        return this;
    }

    private AnalysisReportClientRequestFactory plugins(List<String> plugins) {
        this.plugins.addAll(plugins);
        return this;
    }

    private AnalysisReportClientRequestFactory categories(List<String> categories) {
        this.categories.addAll(categories);
        return this;
    }

    private AnalysisReportClientRequestFactory groups(List<String> groups) {
        this.groups.addAll(groups);
        return this;
    }

    private AnalysisReportClientRequestFactory labels(List<String> labels) {
        this.labels.addAll(labels);
        return this;
    }

    private AnalysisReportClientRequestFactory languages(List<String> languages) {
        this.languages.addAll(languages);
        return this;
    }
}
