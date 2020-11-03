package org.entando.kubernetes.controller.digitalexchange.job.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.entando.kubernetes.stubhelper.AnalysisReportStubHelper;
import org.junit.jupiter.api.Test;

class AnalysisReportTest {

    @Test
    void givenTwoAnalysisReportsTheyShouldMergeThemselves() {

        AnalysisReport analysisReportWithCats = AnalysisReportStubHelper.stubAnalysisReportWithCategories();
        AnalysisReport analysisReportWithFrags = AnalysisReportStubHelper.stubAnalysisReportWithFragments();

        AnalysisReport mergedAnalysisReport = analysisReportWithCats.merge(analysisReportWithFrags);

        assertThat(mergedAnalysisReport).isNotEqualTo(analysisReportWithCats);
        assertThat(mergedAnalysisReport).isNotEqualTo(analysisReportWithFrags);

        assertThat(mergedAnalysisReport.getAssets()).isEmpty();
        assertThat(mergedAnalysisReport.getContents()).isEmpty();
        assertThat(mergedAnalysisReport.getContentTemplates()).isEmpty();
        assertThat(mergedAnalysisReport.getContentTypes()).isEmpty();
        assertThat(mergedAnalysisReport.getGroups()).isEmpty();
        assertThat(mergedAnalysisReport.getLabels()).isEmpty();
        assertThat(mergedAnalysisReport.getLanguages()).isEmpty();
        assertThat(mergedAnalysisReport.getPages()).isEmpty();
        assertThat(mergedAnalysisReport.getPageTemplates()).isEmpty();
        assertThat(mergedAnalysisReport.getPlugins()).isEmpty();
        assertThat(mergedAnalysisReport.getResources()).isEmpty();
        assertThat(mergedAnalysisReport.getWidgets()).isEmpty();

        assertThat(mergedAnalysisReport.getCategories()).containsAllEntriesOf(analysisReportWithCats.getCategories());
        assertThat(mergedAnalysisReport.getFragments()).containsAllEntriesOf(analysisReportWithFrags.getFragments());
    }
}