package org.entando.kubernetes.controller.digitalexchange.job.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    public Map<String, ComponentInstallPlan> getPlanByType(ComponentType type) {
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
                .widgets(this.getNotNullInstallPlanComponent(InstallPlan::getWidgets, other))
                .fragments(this.getNotNullInstallPlanComponent(InstallPlan::getFragments, other))
                .pages(this.getNotNullInstallPlanComponent(InstallPlan::getPages, other))
                .pageTemplates(this.getNotNullInstallPlanComponent(InstallPlan::getPageTemplates, other))
                .contents(this.getNotNullInstallPlanComponent(InstallPlan::getContents, other))
                .contentTemplates(this.getNotNullInstallPlanComponent(InstallPlan::getContentTemplates, other))
                .contentTypes(this.getNotNullInstallPlanComponent(InstallPlan::getContentTypes, other))
                .assets(this.getNotNullInstallPlanComponent(InstallPlan::getAssets, other))
                .resources(this.getNotNullInstallPlanComponent(InstallPlan::getResources, other))
                .plugins(this.getNotNullInstallPlanComponent(InstallPlan::getPlugins, other))
                .categories(this.getNotNullInstallPlanComponent(InstallPlan::getCategories, other))
                .groups(this.getNotNullInstallPlanComponent(InstallPlan::getGroups, other))
                .labels(this.getNotNullInstallPlanComponent(InstallPlan::getLabels, other))
                .languages(this.getNotNullInstallPlanComponent(InstallPlan::getLanguages, other))
                .build();
    }

    /**
     * apply the received function to the current InstallPlan. if the result is not null, return it. otherwise apply
     * the function to the other InstallPlan received and return its result
     *
     * @param getInstallPlanComponentFn a function that get an InstallPlan and returns the desired Map object
     * @param other                        the other InstallPlan
     * @return the result of the received function on the current object if the result is not null, otherwise the result
     *          of the received function on the other object
     */
    private Map<String, ComponentInstallPlan> getNotNullInstallPlanComponent(
            Function<InstallPlan, Map<String, ComponentInstallPlan>> getInstallPlanComponentFn, InstallPlan other) {

        return !CollectionUtils.isEmpty(getInstallPlanComponentFn.apply(this))
                ? getInstallPlanComponentFn.apply(this)
                : getInstallPlanComponentFn.apply(other);
    }

    /**
     * if there is at least an action different from CREATE the installPlan is custom.
     * @return true if the install plan is custom, false otherwise
     */
    @JsonIgnore
    public boolean isCustomInstallation() {
        return doComponentsHaveSkipAction(this.getWidgets())
                || doComponentsHaveSkipAction(this.getFragments())
                || doComponentsHaveSkipAction(this.getPages())
                || doComponentsHaveSkipAction(this.getPageTemplates())
                || doComponentsHaveSkipAction(this.getContents())
                || doComponentsHaveSkipAction(this.getContentTemplates())
                || doComponentsHaveSkipAction(this.getContentTypes())
                || doComponentsHaveSkipAction(this.getAssets())
                || doComponentsHaveSkipAction(this.getResources())
                || doComponentsHaveSkipAction(this.getPlugins())
                || doComponentsHaveSkipAction(this.getCategories())
                || doComponentsHaveSkipAction(this.getGroups())
                || doComponentsHaveSkipAction(this.getLabels())
                || doComponentsHaveSkipAction(this.getLanguages());
    }

    /**
     * check if at least one of the received ComponentInstallPlan has install action different from CREATE.
     * @param componentInstallPlanMap the map of ComponentInstallPlan co check
     * @return true if at least one ComponentInstallPlan has install action different from CREATE
     */
    private boolean doComponentsHaveSkipAction(Map<String, ComponentInstallPlan> componentInstallPlanMap) {

        if (CollectionUtils.isEmpty(componentInstallPlanMap)) {
            return false;
        }

        return componentInstallPlanMap.values().stream()
                .map(componentInstallPlan -> componentInstallPlan.getAction() == InstallAction.SKIP)
                .reduce((customInstallAction1, customInstallAction2) -> customInstallAction1 || customInstallAction2)
                .orElse(false);
    }
}
