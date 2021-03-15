package org.entando.kubernetes.client.model.assembler;

import static org.assertj.core.api.Java6Assertions.assertThat;

import java.util.Map;
import org.entando.kubernetes.client.model.ClientAnalysisReport;
import org.entando.kubernetes.controller.digitalexchange.job.model.AnalysisReport;
import org.entando.kubernetes.controller.digitalexchange.job.model.AnalysisReportComponentResult;
import org.entando.kubernetes.controller.digitalexchange.job.model.Status;
import org.entando.kubernetes.stubhelper.AnalysisReportStubHelper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class AnalysisReportAssemblerTest {

    @Test
    void shouldReturnEmptyAnalysisReportWithNullClientAnalysisReport() {

        AnalysisReport analysisReport = AnalysisReportAssembler.toAnalysisReport(null);
        assertThat(analysisReport.getHasConflicts()).isNull();
        assertThat(analysisReport.getWidgets()).hasSize(0);
        assertThat(analysisReport.getFragments()).hasSize(0);
        assertThat(analysisReport.getPages()).hasSize(0);
        assertThat(analysisReport.getPageTemplates()).hasSize(0);
        assertThat(analysisReport.getContents()).hasSize(0);
        assertThat(analysisReport.getContentTemplates()).hasSize(0);
        assertThat(analysisReport.getContentTypes()).hasSize(0);
        assertThat(analysisReport.getAssets()).hasSize(0);
        assertThat(analysisReport.getDirectories()).hasSize(0);
        assertThat(analysisReport.getResources()).hasSize(0);
        assertThat(analysisReport.getPlugins()).hasSize(0);
        assertThat(analysisReport.getCategories()).hasSize(0);
        assertThat(analysisReport.getGroups()).hasSize(0);
        assertThat(analysisReport.getLabels()).hasSize(0);
        assertThat(analysisReport.getLanguages()).hasSize(0);
    }

    @Test
    void shouldNOTHaveConflictsIfNoComponentsHaveDiffStatus() {

        ClientAnalysisReport clientAnalysisReport = ClientAnalysisReport.builder()
                .assets(Map.of("code1", Status.NEW, "code2", Status.EQUAL))
                .fragments(Map.of("code3", Status.NEW, "code4", Status.EQUAL))
                .build();
        AnalysisReport analysisReport = AnalysisReportAssembler.toAnalysisReport(clientAnalysisReport);

        assertThat(analysisReport.getHasConflicts()).isFalse();
    }

    @Test
    void shouldConvertToAnalysisReportWithFullyPopulatedClientAnalysisReport() {

        ClientAnalysisReport clientAnalysisReport = AnalysisReportStubHelper.stubFullClientAnalysisReport();
        AnalysisReport analysisReport = AnalysisReportAssembler.toAnalysisReport(clientAnalysisReport);

        assertThat(analysisReport.getHasConflicts()).isTrue();
        applyAssertions(analysisReport, clientAnalysisReport);
    }

    @Test
    void shouldConvertToAnalysisReportWithEmptyClientAnalysisReport() {

        ClientAnalysisReport clientAnalysisReport = ClientAnalysisReport.builder().build();
        AnalysisReport analysisReport = AnalysisReportAssembler.toAnalysisReport(clientAnalysisReport);

        assertThat(analysisReport.getHasConflicts()).isFalse();
        applyAssertions(analysisReport, clientAnalysisReport);
    }


    private void applyAssertions(AnalysisReport analysisReport, ClientAnalysisReport clientAnalysisReport) {

        assertOnComponents(analysisReport.getWidgets(), clientAnalysisReport.getWidgets());
        assertOnComponents(analysisReport.getFragments(), clientAnalysisReport.getFragments());
        assertOnComponents(analysisReport.getPages(), clientAnalysisReport.getPages());
        assertOnComponents(analysisReport.getPageTemplates(), clientAnalysisReport.getPageTemplates());
        assertOnComponents(analysisReport.getContents(), clientAnalysisReport.getContents());
        assertOnComponents(analysisReport.getContentTemplates(), clientAnalysisReport.getContentTemplates());
        assertOnComponents(analysisReport.getContentTypes(), clientAnalysisReport.getContentTypes());
        assertOnComponents(analysisReport.getAssets(), clientAnalysisReport.getAssets());
        assertOnComponents(analysisReport.getDirectories(), clientAnalysisReport.getDirectories());
        assertOnComponents(analysisReport.getResources(), clientAnalysisReport.getResources());
        assertOnComponents(analysisReport.getPlugins(), clientAnalysisReport.getPlugins());
        assertOnComponents(analysisReport.getCategories(), clientAnalysisReport.getCategories());
        assertOnComponents(analysisReport.getGroups(), clientAnalysisReport.getGroups());
        assertOnComponents(analysisReport.getLabels(), clientAnalysisReport.getLabels());
        assertOnComponents(analysisReport.getLanguages(), clientAnalysisReport.getLanguages());
    }


    private void assertOnComponents(Map<String, AnalysisReportComponentResult> analysisReportComponents,
            Map<String, Status> analysisReportClientComponents) {

        assertThat(analysisReportComponents.entrySet()).hasSize(analysisReportClientComponents.size());

        analysisReportComponents.forEach((compCode, analysisReportComponentResult) -> {
            assertThat(analysisReportComponentResult.getStatus()).isEqualTo(analysisReportClientComponents.get(compCode));
            // TODO update these 2 lines when services will return data
            assertThat(analysisReportComponentResult.getHash()).isEqualTo("");
            assertThat(analysisReportComponentResult.getUpdateTime()).isNull();
        });
    }
}
