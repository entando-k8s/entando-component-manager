package org.entando.kubernetes.client.model.assembler;

import static org.assertj.core.api.Java6Assertions.assertThat;

import java.util.Map;
import org.entando.kubernetes.client.model.AnalysisReport;
import org.entando.kubernetes.controller.digitalexchange.job.model.ComponentInstallPlan;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.controller.digitalexchange.job.model.Status;
import org.entando.kubernetes.stubhelper.AnalysisReportStubHelper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class InstallPlanAssemblerTest {

    @Test
    void shouldReturnEmptyInstallPlanWithNullAnalysisReport() {

        InstallPlan installPlan = InstallPlanAssembler.toInstallPlan(null);
        assertThat(installPlan.getHasConflicts()).isNull();
        assertThat(installPlan.getWidgets()).hasSize(0);
        assertThat(installPlan.getFragments()).hasSize(0);
        assertThat(installPlan.getPages()).hasSize(0);
        assertThat(installPlan.getPageTemplates()).hasSize(0);
        assertThat(installPlan.getContents()).hasSize(0);
        assertThat(installPlan.getContentTemplates()).hasSize(0);
        assertThat(installPlan.getContentTypes()).hasSize(0);
        assertThat(installPlan.getAssets()).hasSize(0);
        assertThat(installPlan.getDirectories()).hasSize(0);
        assertThat(installPlan.getResources()).hasSize(0);
        assertThat(installPlan.getPlugins()).hasSize(0);
        assertThat(installPlan.getCategories()).hasSize(0);
        assertThat(installPlan.getGroups()).hasSize(0);
        assertThat(installPlan.getLabels()).hasSize(0);
        assertThat(installPlan.getLanguages()).hasSize(0);
    }

    @Test
    void shouldNOTHaveConflictsIfNoComponentsHaveDiffStatus() {

        AnalysisReport analysisReport = AnalysisReport.builder()
                .assets(Map.of("code1", Status.NEW, "code2", Status.EQUAL))
                .widgets(Map.of("code1", Status.NEW, "code2", Status.EQUAL))
                .fragments(Map.of("code1", Status.NEW, "code2", Status.EQUAL))
                .pages(Map.of("code1", Status.NEW, "code2", Status.EQUAL))
                .pageTemplates(Map.of("code1", Status.NEW, "code2", Status.EQUAL))
                .contents(Map.of("code1", Status.NEW, "code2", Status.EQUAL))
                .contentTemplates(Map.of("code1", Status.NEW, "code2", Status.EQUAL))
                .contentTypes(Map.of("code1", Status.NEW, "code2", Status.EQUAL))
                .directories(Map.of("code3", Status.NEW, "code4", Status.EQUAL))
                .resources(Map.of("code3", Status.NEW, "code4", Status.EQUAL))
                .plugins(Map.of("code3", Status.NEW, "code4", Status.EQUAL))
                .categories(Map.of("code3", Status.NEW, "code4", Status.EQUAL))
                .groups(Map.of("code3", Status.NEW, "code4", Status.EQUAL))
                .labels(Map.of("code3", Status.NEW, "code4", Status.EQUAL))
                .languages(Map.of("code3", Status.NEW, "code4", Status.EQUAL))
                .build();
        InstallPlan installPlan = InstallPlanAssembler.toInstallPlan(analysisReport);

        assertThat(installPlan.getHasConflicts()).isFalse();
    }

    @Test
    void shouldConvertToInstallPlanWithFullyPopulatedAnalysisReport() {

        AnalysisReport analysisReport = AnalysisReportStubHelper.stubFullAnalysisReport();
        InstallPlan installPlan = InstallPlanAssembler.toInstallPlan(analysisReport);

        assertThat(installPlan.getHasConflicts()).isTrue();
        applyAssertions(installPlan, analysisReport);
    }

    @Test
    void shouldConvertToInstallPlanWithEmptyAnalysisReport() {

        AnalysisReport analysisReport = AnalysisReport.builder().build();
        InstallPlan installPlan = InstallPlanAssembler.toInstallPlan(analysisReport);

        assertThat(installPlan.getHasConflicts()).isFalse();
        applyAssertions(installPlan, analysisReport);
    }


    private void applyAssertions(InstallPlan installPlan, AnalysisReport analysisReport) {

        assertOnComponents(installPlan.getWidgets(), analysisReport.getWidgets());
        assertOnComponents(installPlan.getFragments(), analysisReport.getFragments());
        assertOnComponents(installPlan.getPages(), analysisReport.getPages());
        assertOnComponents(installPlan.getPageTemplates(), analysisReport.getPageTemplates());
        assertOnComponents(installPlan.getContents(), analysisReport.getContents());
        assertOnComponents(installPlan.getContentTemplates(), analysisReport.getContentTemplates());
        assertOnComponents(installPlan.getContentTypes(), analysisReport.getContentTypes());
        assertOnComponents(installPlan.getAssets(), analysisReport.getAssets());
        assertOnComponents(installPlan.getDirectories(), analysisReport.getDirectories());
        assertOnComponents(installPlan.getResources(), analysisReport.getResources());
        assertOnComponents(installPlan.getPlugins(), analysisReport.getPlugins());
        assertOnComponents(installPlan.getCategories(), analysisReport.getCategories());
        assertOnComponents(installPlan.getGroups(), analysisReport.getGroups());
        assertOnComponents(installPlan.getLabels(), analysisReport.getLabels());
        assertOnComponents(installPlan.getLanguages(), analysisReport.getLanguages());
    }


    private void assertOnComponents(Map<String, ComponentInstallPlan> analysisReportComponents,
            Map<String, Status> analysisReportClientComponents) {

        assertThat(analysisReportComponents.entrySet()).hasSize(analysisReportClientComponents.size());

        analysisReportComponents.forEach((compCode, analysisReportComponentResult) -> {
            assertThat(analysisReportComponentResult.getStatus()).isEqualTo(analysisReportClientComponents.get(compCode));
            assertThat(analysisReportComponentResult.getAction()).isNull();
            // TODO update these 2 lines when services will return data
            assertThat(analysisReportComponentResult.getHash()).isNull();
            assertThat(analysisReportComponentResult.getUpdateTime()).isNull();
        });
    }
}
