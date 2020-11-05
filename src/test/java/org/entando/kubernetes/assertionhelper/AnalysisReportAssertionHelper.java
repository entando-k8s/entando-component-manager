package org.entando.kubernetes.assertionhelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static wiremock.org.eclipse.jetty.util.LazyList.hasEntry;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import org.entando.kubernetes.controller.digitalexchange.job.model.AnalysisReport;
import org.entando.kubernetes.controller.digitalexchange.job.model.AnalysisReport.Status;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.CollectionUtils;

public class AnalysisReportAssertionHelper {


    public static void assertOnAnalysisReports(AnalysisReport expected, AnalysisReport actual) {

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


    private static Entry[] toEntryArray(Map<String, Status> map) {
        return map.entrySet().toArray(Entry[]::new);
    }


    public static void assertOnAnalysisReport(AnalysisReport expected, ResultActions actual) throws Exception {

        actual.andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.payload.widgets").isEmpty());

        assertOnAnalysisReportJsonPath(actual, "widgets", expected.getWidgets());
        assertOnAnalysisReportJsonPath(actual, "fragments", expected.getFragments());
        assertOnAnalysisReportJsonPath(actual, "pages", expected.getPages());
        assertOnAnalysisReportJsonPath(actual, "pageTemplates", expected.getPageTemplates());
        assertOnAnalysisReportJsonPath(actual, "contents", expected.getContents());
        assertOnAnalysisReportJsonPath(actual, "contentTemplates", expected.getContentTemplates());
        assertOnAnalysisReportJsonPath(actual, "contentTypes", expected.getContentTypes());
        assertOnAnalysisReportJsonPath(actual, "assets", expected.getAssets());
        assertOnAnalysisReportJsonPath(actual, "resources", expected.getResources());
        assertOnAnalysisReportJsonPath(actual, "plugins", expected.getPlugins());
        assertOnAnalysisReportJsonPath(actual, "categories", expected.getCategories());
        assertOnAnalysisReportJsonPath(actual, "groups", expected.getGroups());
        assertOnAnalysisReportJsonPath(actual, "labels", expected.getLabels());
        assertOnAnalysisReportJsonPath(actual, "languages", expected.getLanguages());
    }

    private static void assertOnAnalysisReportJsonPath(ResultActions resultActions, String jsonSubPath,
            Map<String, Status> componentReport) throws Exception {

        if (CollectionUtils.isEmpty(componentReport)) {
            resultActions.andExpect(jsonPath("$.payload." + jsonSubPath).isEmpty());
        } else {
            componentReport.forEach((key, status) -> {
                try {
                    resultActions.andExpect(
                            jsonPath("$.payload." + jsonSubPath + "." + key, is(status.toString())));
                } catch (Exception e) {
                    Assertions.fail();
                }
            });
        }
    }
}
