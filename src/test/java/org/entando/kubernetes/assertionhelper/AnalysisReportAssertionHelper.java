package org.entando.kubernetes.assertionhelper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Map.Entry;
import org.entando.kubernetes.client.model.AnalysisReport;
import org.entando.kubernetes.controller.digitalexchange.job.model.Status;

public class AnalysisReportAssertionHelper {


    public static void assertOnAnalysisReports(AnalysisReport expected, AnalysisReport actual) {

        assertThat(actual.getAssets()).containsOnly(toEntryArrayStatus(expected.getAssets()));
        assertThat(actual.getFragments()).containsOnly(toEntryArrayStatus(expected.getFragments()));
        assertThat(actual.getContents()).containsOnly(toEntryArrayStatus(expected.getContents()));
        assertThat(actual.getContentTemplates()).containsOnly(toEntryArrayStatus(expected.getContentTemplates()));
        assertThat(actual.getContentTypes()).containsOnly(toEntryArrayStatus(expected.getContentTypes()));
        assertThat(actual.getGroups()).containsOnly(toEntryArrayStatus(expected.getGroups()));
        assertThat(actual.getLabels()).containsOnly(toEntryArrayStatus(expected.getLabels()));
        assertThat(actual.getLanguages()).containsOnly(toEntryArrayStatus(expected.getLanguages()));
        assertThat(actual.getPages()).containsOnly(toEntryArrayStatus(expected.getPages()));
        assertThat(actual.getPageTemplates()).containsOnly(toEntryArrayStatus(expected.getPageTemplates()));
        assertThat(actual.getPlugins()).containsOnly(toEntryArrayStatus(expected.getPlugins()));
        assertThat(actual.getCategories()).containsOnly(toEntryArrayStatus(expected.getCategories()));
        assertThat(actual.getResources()).containsOnly(toEntryArrayStatus(expected.getResources()));
        assertThat(actual.getWidgets()).containsOnly(toEntryArrayStatus(expected.getWidgets()));
    }

    private static Entry[] toEntryArrayStatus(Map<String, Status> map) {
        return map.entrySet().toArray(Entry[]::new);
    }
}
