/**
 * assembler pattern to adapt AnalysisReport to ClientAnalysisReport and viceversa.
 */

package org.entando.kubernetes.client.model.assembler;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.stream.Collectors;
import org.entando.kubernetes.client.model.ClientAnalysisReport;
import org.entando.kubernetes.controller.digitalexchange.job.model.AnalysisReport;
import org.entando.kubernetes.controller.digitalexchange.job.model.AnalysisReportComponentResult;
import org.entando.kubernetes.controller.digitalexchange.job.model.Status;

public class AnalysisReportAssembler {

    private AnalysisReportAssembler() {
    }

    public static AnalysisReport toAnalysisReport(ClientAnalysisReport clientAnalysisReport) {

        if (null == clientAnalysisReport) {
            return new AnalysisReport();
        }

        return AnalysisReport.builder()
                .hasConflicts(clientAnalysisReportHasConflicts(clientAnalysisReport))
                .widgets(toAnalysisReportComponentResult(clientAnalysisReport.getWidgets()))
                .fragments(toAnalysisReportComponentResult(clientAnalysisReport.getFragments()))
                .pages(toAnalysisReportComponentResult(clientAnalysisReport.getPages()))
                .pageTemplates(toAnalysisReportComponentResult(clientAnalysisReport.getPageTemplates()))
                .contents(toAnalysisReportComponentResult(clientAnalysisReport.getContents()))
                .contentTemplates(toAnalysisReportComponentResult(clientAnalysisReport.getContentTemplates()))
                .contentTypes(toAnalysisReportComponentResult(clientAnalysisReport.getContentTypes()))
                .assets(toAnalysisReportComponentResult(clientAnalysisReport.getAssets()))
                .directories(toAnalysisReportComponentResult(clientAnalysisReport.getDirectories()))
                .resources(toAnalysisReportComponentResult(clientAnalysisReport.getResources()))
                .plugins(toAnalysisReportComponentResult(clientAnalysisReport.getPlugins()))
                .categories(toAnalysisReportComponentResult(clientAnalysisReport.getCategories()))
                .groups(toAnalysisReportComponentResult(clientAnalysisReport.getGroups()))
                .labels(toAnalysisReportComponentResult(clientAnalysisReport.getLabels()))
                .languages(toAnalysisReportComponentResult(clientAnalysisReport.getLanguages()))
                .build();
    }


    private static Map<String, AnalysisReportComponentResult> toAnalysisReportComponentResult(
            Map<String, Status> components) {

        return components.entrySet().stream()
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), new AnalysisReportComponentResult(
                        entry.getValue(), null, "")))
                .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    }


    private static boolean clientAnalysisReportHasConflicts(ClientAnalysisReport clientAnalysisReport) {

        return componentsHaveConflicts(clientAnalysisReport.getWidgets())
                || componentsHaveConflicts(clientAnalysisReport.getFragments())
                || componentsHaveConflicts(clientAnalysisReport.getPages())
                || componentsHaveConflicts(clientAnalysisReport.getPageTemplates())
                || componentsHaveConflicts(clientAnalysisReport.getContents())
                || componentsHaveConflicts(clientAnalysisReport.getContentTemplates())
                || componentsHaveConflicts(clientAnalysisReport.getContentTypes())
                || componentsHaveConflicts(clientAnalysisReport.getAssets())
                || componentsHaveConflicts(clientAnalysisReport.getDirectories())
                || componentsHaveConflicts(clientAnalysisReport.getResources())
                || componentsHaveConflicts(clientAnalysisReport.getPlugins())
                || componentsHaveConflicts(clientAnalysisReport.getCategories())
                || componentsHaveConflicts(clientAnalysisReport.getGroups())
                || componentsHaveConflicts(clientAnalysisReport.getLabels())
                || componentsHaveConflicts(clientAnalysisReport.getLanguages());
    }


    private static boolean componentsHaveConflicts(Map<String, Status> components) {

        return components.values().stream()
                .anyMatch(status -> status == Status.DIFF);
    }
}
