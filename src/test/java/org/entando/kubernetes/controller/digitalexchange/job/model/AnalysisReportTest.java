package org.entando.kubernetes.controller.digitalexchange.job.model;

import org.entando.kubernetes.assertionhelper.AnalysisReportAssertionHelper;
import org.entando.kubernetes.stubhelper.AnalysisReportStubHelper;
import org.junit.jupiter.api.Test;

class AnalysisReportTest {

    @Test
    void givenTwoAnalysisReportsTheyShouldMergeThemselves() {

        AnalysisReport analysisReportWithCats = AnalysisReportStubHelper.stubAnalysisReportWithCategories();
        AnalysisReport analysisReportWithFrags = AnalysisReportStubHelper.stubAnalysisReportWithFragments();

        AnalysisReport mergedAnalysisReport = analysisReportWithCats.merge(analysisReportWithFrags);

        AnalysisReport expected = AnalysisReportStubHelper.stubAnalysisReportWithFragmentsAndCategories();
        AnalysisReportAssertionHelper.assertOnAnalysisReports(expected, mergedAnalysisReport);
    }
}