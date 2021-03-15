package org.entando.kubernetes.assertionhelper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Map.Entry;
import org.entando.kubernetes.controller.digitalexchange.job.model.AnalysisReport;
import org.entando.kubernetes.controller.digitalexchange.job.model.AnalysisReportComponentResult;

public class AnalysisReportAssertionHelper {


    public static void assertOnAnalysisReports(AnalysisReport expected, AnalysisReport actual) {

        assertThat(actual.getHasConflicts()).isEqualTo(expected.getHasConflicts());
        assertThat(actual.getAssets()).containsOnly(toEntryArray(expected.getAssets()));
        assertThat(actual.getFragments()).containsOnly(toEntryArray(expected.getFragments()));
        assertThat(actual.getContents()).containsOnly(toEntryArray(expected.getContents()));
        assertThat(actual.getContentTemplates()).containsOnly(toEntryArray(expected.getContentTemplates()));
        assertThat(actual.getContentTypes()).containsOnly(toEntryArray(expected.getContentTypes()));
        assertThat(actual.getGroups()).containsOnly(toEntryArray(expected.getGroups()));
        assertThat(actual.getLabels()).containsOnly(toEntryArray(expected.getLabels()));
        assertThat(actual.getLanguages()).containsOnly(toEntryArray(expected.getLanguages()));
        assertThat(actual.getPages()).containsOnly(toEntryArray(expected.getPages()));
        assertThat(actual.getPageTemplates()).containsOnly(toEntryArray(expected.getPageTemplates()));
        assertThat(actual.getPlugins()).containsOnly(toEntryArray(expected.getPlugins()));
        assertThat(actual.getCategories()).containsOnly(toEntryArray(expected.getCategories()));
        assertThat(actual.getResources()).containsOnly(toEntryArray(expected.getResources()));
        assertThat(actual.getWidgets()).containsOnly(toEntryArray(expected.getWidgets()));
    }

    private static Entry[] toEntryArray(Map<String, AnalysisReportComponentResult> map) {
        return map.entrySet().toArray(Entry[]::new);
    }
}
