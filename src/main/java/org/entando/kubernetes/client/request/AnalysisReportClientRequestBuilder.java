package org.entando.kubernetes.client.request;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Consumer;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.reportable.Reportable;

public final class AnalysisReportClientRequestBuilder {

    private EnumMap<ComponentType, Consumer<List<String>>> strategy;

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

    private AnalysisReportClientRequestBuilder() {
        strategy = new EnumMap<>(ComponentType.class);
        strategy.put(ComponentType.WIDGET, this::widgets);
        strategy.put(ComponentType.FRAGMENT, this::fragments);
        strategy.put(ComponentType.PAGE, this::pages);
        strategy.put(ComponentType.PAGE_TEMPLATE, this::pageTemplates);
        strategy.put(ComponentType.CONTENT, this::contents);
        strategy.put(ComponentType.CONTENT_TEMPLATE, this::contentTemplates);
        strategy.put(ComponentType.ASSET, this::assets);
        strategy.put(ComponentType.RESOURCE, this::resources);
        strategy.put(ComponentType.PLUGIN, this::plugins);
        strategy.put(ComponentType.CATEGORY, this::categories);
        strategy.put(ComponentType.GROUP, this::groups);
        strategy.put(ComponentType.LABEL, this::labels);
        strategy.put(ComponentType.LANGUAGE, this::languages);
        strategy.put(ComponentType.DIRECTORY, this::directories);
    }

    public static AnalysisReportClientRequestBuilder anAnalysisReportClientRequest() {
        return new AnalysisReportClientRequestBuilder();
    }

    public AnalysisReportClientRequestBuilder reportableList(List<Reportable> reportableList) {

        reportableList.forEach(reportable -> strategy.get(reportable.getComponentType()).accept(reportable.getCodes()));
        return this;
    }

    public AnalysisReportClientRequestBuilder widgets(List<String> widgets) {
        this.widgets = widgets;
        return this;
    }

    public AnalysisReportClientRequestBuilder fragments(List<String> fragments) {
        this.fragments = fragments;
        return this;
    }

    public AnalysisReportClientRequestBuilder pages(List<String> pages) {
        this.pages = pages;
        return this;
    }

    public AnalysisReportClientRequestBuilder pageTemplates(List<String> pageTemplates) {
        this.pageTemplates = pageTemplates;
        return this;
    }

    public AnalysisReportClientRequestBuilder contents(List<String> contents) {
        this.contents = contents;
        return this;
    }

    public AnalysisReportClientRequestBuilder contentTemplates(List<String> contentTemplates) {
        this.contentTemplates = contentTemplates;
        return this;
    }

    public AnalysisReportClientRequestBuilder contentTypes(List<String> contentTypes) {
        this.contentTypes = contentTypes;
        return this;
    }

    public AnalysisReportClientRequestBuilder assets(List<String> assets) {
        this.assets = assets;
        return this;
    }

    public AnalysisReportClientRequestBuilder resources(List<String> resources) {
        this.resources = resources;
        return this;
    }

    public AnalysisReportClientRequestBuilder plugins(List<String> plugins) {
        this.plugins = plugins;
        return this;
    }

    public AnalysisReportClientRequestBuilder categories(List<String> categories) {
        this.categories = categories;
        return this;
    }

    public AnalysisReportClientRequestBuilder groups(List<String> groups) {
        this.groups = groups;
        return this;
    }

    public AnalysisReportClientRequestBuilder labels(List<String> labels) {
        this.labels = labels;
        return this;
    }

    public AnalysisReportClientRequestBuilder languages(List<String> languages) {
        this.languages = languages;
        return this;
    }

    public AnalysisReportClientRequestBuilder directories(List<String> directories) {
        this.directories = directories;
        return this;
    }

    public AnalysisReportClientRequest build() {
        return new AnalysisReportClientRequest(widgets, fragments, pages, pageTemplates, contents, contentTemplates,
                contentTypes, assets, resources, plugins, categories, groups, labels, languages, directories);
    }
}
