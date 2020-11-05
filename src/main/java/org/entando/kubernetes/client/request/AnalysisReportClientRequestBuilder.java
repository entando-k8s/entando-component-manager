package org.entando.kubernetes.client.request;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.reportable.Reportable;
import org.springframework.util.StringUtils;

public final class AnalysisReportClientRequestBuilder {

    private EnumMap<ComponentType, Consumer<List<String>>> strategy;

    List<String> widgets = new ArrayList<>();
    List<String> fragments = new ArrayList<>();
    List<String> pages = new ArrayList<>();
    List<String> pageTemplates = new ArrayList<>();
    List<String> contents = new ArrayList<>();
    List<String> contentTemplates = new ArrayList<>();
    List<String> contentTypes = new ArrayList<>();
    List<String> assets = new ArrayList<>();
    List<String> resources = new ArrayList<>();
    List<String> plugins = new ArrayList<>();
    List<String> categories = new ArrayList<>();
    List<String> groups = new ArrayList<>();
    List<String> labels = new ArrayList<>();
    List<String> languages = new ArrayList<>();

    private AnalysisReportClientRequestBuilder() {
        strategy = new EnumMap<>(ComponentType.class);
        strategy.put(ComponentType.WIDGET, this::widgets);
        strategy.put(ComponentType.FRAGMENT, this::fragments);
        strategy.put(ComponentType.PAGE, this::pages);
        strategy.put(ComponentType.PAGE_TEMPLATE, this::pageTemplates);
        strategy.put(ComponentType.CONTENT, this::contents);
        strategy.put(ComponentType.CONTENT_TEMPLATE, this::contentTemplates);
        strategy.put(ComponentType.CONTENT_TYPE, this::contentTypes);
        strategy.put(ComponentType.ASSET, this::assets);
        strategy.put(ComponentType.RESOURCE, this::resources);
        strategy.put(ComponentType.PLUGIN, this::plugins);
        strategy.put(ComponentType.CATEGORY, this::categories);
        strategy.put(ComponentType.GROUP, this::groups);
        strategy.put(ComponentType.LABEL, this::labels);
        strategy.put(ComponentType.LANGUAGE, this::languages);
        strategy.put(ComponentType.DIRECTORY, this::resources);
    }

    public static AnalysisReportClientRequestBuilder anAnalysisReportClientRequest() {
        return new AnalysisReportClientRequestBuilder();
    }

    public AnalysisReportClientRequestBuilder reportableList(List<Reportable> reportableList) {

        reportableList.forEach(reportable -> {
            if (! StringUtils.isEmpty(reportable.getCodes())) {
                strategy.get(reportable.getComponentType()).accept(reportable.getCodes());
            }
        });
        return this;
    }

    private AnalysisReportClientRequestBuilder widgets(List<String> widgets) {
        this.widgets.addAll(widgets);
        return this;
    }

    private AnalysisReportClientRequestBuilder fragments(List<String> fragments) {
        this.fragments.addAll(fragments);
        return this;
    }

    private AnalysisReportClientRequestBuilder pages(List<String> pages) {
        this.pages.addAll(pages);
        return this;
    }

    private AnalysisReportClientRequestBuilder pageTemplates(List<String> pageTemplates) {
        this.pageTemplates.addAll(pageTemplates);
        return this;
    }

    private AnalysisReportClientRequestBuilder contents(List<String> contents) {
        this.contents.addAll(contents);
        return this;
    }

    private AnalysisReportClientRequestBuilder contentTemplates(List<String> contentTemplates) {
        this.contentTemplates.addAll(contentTemplates);
        return this;
    }

    private AnalysisReportClientRequestBuilder contentTypes(List<String> contentTypes) {
        this.contentTypes.addAll(contentTypes);
        return this;
    }

    private AnalysisReportClientRequestBuilder assets(List<String> assets) {
        this.assets.addAll(assets);
        return this;
    }

    private AnalysisReportClientRequestBuilder resources(List<String> resources) {
        this.resources.addAll(resources);
        return this;
    }

    private AnalysisReportClientRequestBuilder plugins(List<String> plugins) {
        this.plugins.addAll(plugins);
        return this;
    }

    private AnalysisReportClientRequestBuilder categories(List<String> categories) {
        this.categories.addAll(categories);
        return this;
    }

    private AnalysisReportClientRequestBuilder groups(List<String> groups) {
        this.groups.addAll(groups);
        return this;
    }

    private AnalysisReportClientRequestBuilder labels(List<String> labels) {
        this.labels.addAll(labels);
        return this;
    }

    private AnalysisReportClientRequestBuilder languages(List<String> languages) {
        this.languages.addAll(languages);
        return this;
    }

    public AnalysisReportClientRequest build() {
        return new AnalysisReportClientRequest(widgets, fragments, pages, pageTemplates, contents, contentTemplates,
                contentTypes, assets, resources, plugins, categories, groups, labels, languages);
    }
}
