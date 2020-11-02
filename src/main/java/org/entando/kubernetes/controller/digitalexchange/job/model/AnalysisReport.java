package org.entando.kubernetes.controller.digitalexchange.job.model;

import java.util.function.Function;
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

    @Default private AnalysisReportComponent widgets = new AnalysisReportComponent();
    @Default private AnalysisReportComponent fragments = new AnalysisReportComponent();
    @Default private AnalysisReportComponent pages = new AnalysisReportComponent();
    @Default private AnalysisReportComponent pageTemplates = new AnalysisReportComponent();
    @Default private AnalysisReportComponent contents = new AnalysisReportComponent();
    @Default private AnalysisReportComponent contentTemplates = new AnalysisReportComponent();
    @Default private AnalysisReportComponent contentTypes = new AnalysisReportComponent();
    @Default private AnalysisReportComponent assets = new AnalysisReportComponent();
    @Default private AnalysisReportComponent resources = new AnalysisReportComponent();
    @Default private AnalysisReportComponent plugins = new AnalysisReportComponent();
    @Default private AnalysisReportComponent categories = new AnalysisReportComponent();
    @Default private AnalysisReportComponent groups = new AnalysisReportComponent();
    @Default private AnalysisReportComponent labels = new AnalysisReportComponent();
    @Default private AnalysisReportComponent languages = new AnalysisReportComponent();


    /**
     * merge the current AnalysisReport and the received one
     * @param other the AnalysisReport to merge with the current one
     * @return a new AnalysisReport resulting by the merge
     */
    public AnalysisReport merge(AnalysisReport other) {

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
    private AnalysisReportComponent getNotNullAnalysisReportComponent(
            Function<AnalysisReport, AnalysisReportComponent> getAnalysisReportComponentFn, AnalysisReport other) {

        return null != getAnalysisReportComponentFn.apply(this)
                ? getAnalysisReportComponentFn.apply(this)
                : getAnalysisReportComponentFn.apply(other);
    }

    public enum Status {
        NOT_FOUND,
        CONFLICT
    }
}
