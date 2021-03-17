package org.entando.kubernetes.controller.digitalexchange.job.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.springframework.util.CollectionUtils;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class InstallPlan {

    @Default
    private Boolean hasConflicts = null;
    @Default
    private Map<String, ComponentInstallPlan> widgets = new HashMap<>();
    @Default
    private Map<String, ComponentInstallPlan> fragments = new HashMap<>();
    @Default
    private Map<String, ComponentInstallPlan> pages = new HashMap<>();
    @Default
    private Map<String, ComponentInstallPlan> pageTemplates = new HashMap<>();
    @Default
    private Map<String, ComponentInstallPlan> contents = new HashMap<>();
    @Default
    private Map<String, ComponentInstallPlan> contentTemplates = new HashMap<>();
    @Default
    private Map<String, ComponentInstallPlan> contentTypes = new HashMap<>();
    @Default
    private Map<String, ComponentInstallPlan> assets = new HashMap<>();
    @Default
    private Map<String, ComponentInstallPlan> directories = new HashMap<>();
    @Default
    private Map<String, ComponentInstallPlan> resources = new HashMap<>();
    @Default
    private Map<String, ComponentInstallPlan> plugins = new HashMap<>();
    @Default
    private Map<String, ComponentInstallPlan> categories = new HashMap<>();
    @Default
    private Map<String, ComponentInstallPlan> groups = new HashMap<>();
    @Default
    private Map<String, ComponentInstallPlan> labels = new HashMap<>();
    @Default
    private Map<String, ComponentInstallPlan> languages = new HashMap<>();

    public Map<String, ComponentInstallPlan> getReportByType(ComponentType type) {
        switch (type) {
            case WIDGET:
                return widgets;
            case FRAGMENT:
                return fragments;
            case PAGE:
                return pages;
            case PAGE_TEMPLATE:
                return pageTemplates;
            case CONTENT:
                return contents;
            case CONTENT_TEMPLATE:
                return contentTemplates;
            case CONTENT_TYPE:
                return contentTypes;
            case ASSET:
                return assets;
            case DIRECTORY:
                return directories;
            case RESOURCE:
                return resources;
            case PLUGIN:
                return plugins;
            case CATEGORY:
                return categories;
            case GROUP:
                return groups;
            case LABEL:
                return labels;
            case LANGUAGE:
                return languages;
            default:
                return new HashMap<>();
        }
    }


    /**
     * merge the current AnalysisReport and the received one.
     *
     * @param other the AnalysisReport to merge with the current one
     * @return a new AnalysisReport resulting by the merge
     */
    public InstallPlan merge(InstallPlan other) {

        if (null == other) {
            return this;
        }

        return InstallPlan.builder()
                .hasConflicts(
                        Boolean.TRUE.equals(this.hasConflicts) || Boolean.TRUE.equals(other.hasConflicts))
                .widgets(this.getNotNullAnalysisReportComponent(InstallPlan::getWidgets, other))
                .fragments(this.getNotNullAnalysisReportComponent(InstallPlan::getFragments, other))
                .pages(this.getNotNullAnalysisReportComponent(InstallPlan::getPages, other))
                .pageTemplates(this.getNotNullAnalysisReportComponent(InstallPlan::getPageTemplates, other))
                .contents(this.getNotNullAnalysisReportComponent(InstallPlan::getContents, other))
                .contentTemplates(this.getNotNullAnalysisReportComponent(InstallPlan::getContentTemplates, other))
                .contentTypes(this.getNotNullAnalysisReportComponent(InstallPlan::getContentTypes, other))
                .assets(this.getNotNullAnalysisReportComponent(InstallPlan::getAssets, other))
                .resources(this.getNotNullAnalysisReportComponent(InstallPlan::getResources, other))
                .plugins(this.getNotNullAnalysisReportComponent(InstallPlan::getPlugins, other))
                .categories(this.getNotNullAnalysisReportComponent(InstallPlan::getCategories, other))
                .groups(this.getNotNullAnalysisReportComponent(InstallPlan::getGroups, other))
                .labels(this.getNotNullAnalysisReportComponent(InstallPlan::getLabels, other))
                .languages(this.getNotNullAnalysisReportComponent(InstallPlan::getLanguages, other))
                .build();
    }

    /**
     * apply the received function to the current AnalysisReport. if the result is not null, return it. otherwise apply
     * the function to the other AnalysisReport received and return its result
     *
     * @param getAnalysisReportComponentFn a function that get an AnalysisReport and returns the desired Map object
     * @param other                        the other AnalysisReport
     * @return the result of the received function on the current object if the result is not null, otherwise the result
     *          of the received function on the other object
     */
    private Map<String, ComponentInstallPlan> getNotNullAnalysisReportComponent(
            Function<InstallPlan, Map<String, ComponentInstallPlan>> getAnalysisReportComponentFn, InstallPlan other) {

        return !CollectionUtils.isEmpty(getAnalysisReportComponentFn.apply(this))
                ? getAnalysisReportComponentFn.apply(this)
                : getAnalysisReportComponentFn.apply(other);
    }
}
