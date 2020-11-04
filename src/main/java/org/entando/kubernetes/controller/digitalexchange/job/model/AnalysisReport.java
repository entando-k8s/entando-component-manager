package org.entando.kubernetes.controller.digitalexchange.job.model;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

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


    /**
     * merge the current AnalysisReport and the received one
     * @param other the AnalysisReport to merge with the current one
     * @return a new AnalysisReport resulting by the merge
     */
    public AnalysisReport merge(AnalysisReport other) {

        if (null == other) {
            return this;
        }

        return AnalysisReport.builder()
                .widgets(this.getNotNullAnalysisReportComponent(AnalysisReport::getWidgets, other))
                .fragments(this.getNotNullAnalysisReportComponent(AnalysisReport::getFragments, other))
                .pages(this.getNotNullAnalysisReportComponent(AnalysisReport::getPages, other))
                .pageTemplates(this.getNotNullAnalysisReportComponent(AnalysisReport::getPageTemplates, other))
                .contents(this.getNotNullAnalysisReportComponent(AnalysisReport::getContents, other))
                .contentTemplates(this.getNotNullAnalysisReportComponent(AnalysisReport::getContentTemplates, other))
                .contentTypes(this.getNotNullAnalysisReportComponent(AnalysisReport::getContentTypes, other))
                .assets(this.getNotNullAnalysisReportComponent(AnalysisReport::getAssets, other))
                .resources(this.getNotNullAnalysisReportComponent(AnalysisReport::getResources, other))
                .plugins(this.getNotNullAnalysisReportComponent(AnalysisReport::getPlugins, other))
                .categories(this.getNotNullAnalysisReportComponent(AnalysisReport::getCategories, other))
                .groups(this.getNotNullAnalysisReportComponent(AnalysisReport::getGroups, other))
                .labels(this.getNotNullAnalysisReportComponent(AnalysisReport::getLabels, other))
                .languages(this.getNotNullAnalysisReportComponent(AnalysisReport::getLanguages, other))
                .build();
    }

    /**
     * apply the received function to the current AnalysisReport.
     * if the result is not null, return it.
     * otherwise apply the function to the other AnalysisReport received and return its result
     *
     * @param getAnalysisReportComponentFn
     * @param other
     * @return
     */
    private Map<String, Status> getNotNullAnalysisReportComponent(
            Function<AnalysisReport, Map<String, Status>> getAnalysisReportComponentFn, AnalysisReport other) {

        return ! CollectionUtils.isEmpty(getAnalysisReportComponentFn.apply(this))
                ? getAnalysisReportComponentFn.apply(this)
                : getAnalysisReportComponentFn.apply(other);
    }

    public enum Status {
        NEW,
        DIFF,
        EQUAL;
    }
}
