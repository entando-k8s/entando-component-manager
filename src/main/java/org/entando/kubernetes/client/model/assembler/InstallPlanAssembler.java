/**
 * assembler pattern to adapt AnalysisReport to InstallPlan.
 */

package org.entando.kubernetes.client.model.assembler;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.stream.Collectors;
import org.entando.kubernetes.client.model.AnalysisReport;
import org.entando.kubernetes.controller.digitalexchange.job.model.ComponentInstallPlan;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.controller.digitalexchange.job.model.Status;

public class InstallPlanAssembler {

    private InstallPlanAssembler() {
    }

    public static InstallPlan toInstallPlan(AnalysisReport analysisReport) {

        if (null == analysisReport) {
            return new InstallPlan();
        }

        return InstallPlan.builder()
                .hasConflicts(analysisReportHasConflicts(analysisReport))
                .widgets(toComponentInstallPlan(analysisReport.getWidgets()))
                .fragments(toComponentInstallPlan(analysisReport.getFragments()))
                .pages(toComponentInstallPlan(analysisReport.getPages()))
                .pageTemplates(toComponentInstallPlan(analysisReport.getPageTemplates()))
                .contents(toComponentInstallPlan(analysisReport.getContents()))
                .contentTemplates(toComponentInstallPlan(analysisReport.getContentTemplates()))
                .contentTypes(toComponentInstallPlan(analysisReport.getContentTypes()))
                .assets(toComponentInstallPlan(analysisReport.getAssets()))
                .directories(toComponentInstallPlan(analysisReport.getDirectories()))
                .resources(toComponentInstallPlan(analysisReport.getResources()))
                .plugins(toComponentInstallPlan(analysisReport.getPlugins()))
                .categories(toComponentInstallPlan(analysisReport.getCategories()))
                .groups(toComponentInstallPlan(analysisReport.getGroups()))
                .labels(toComponentInstallPlan(analysisReport.getLabels()))
                .languages(toComponentInstallPlan(analysisReport.getLanguages()))
                .build();
    }


    private static Map<String, ComponentInstallPlan> toComponentInstallPlan(
            Map<String, Status> components) {

        return components.entrySet().stream()
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(),
                        ComponentInstallPlan.builder().status(entry.getValue()).build()))
                .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    }


    private static boolean analysisReportHasConflicts(AnalysisReport analysisReport) {

        return componentsHaveConflicts(analysisReport.getWidgets())
                || componentsHaveConflicts(analysisReport.getFragments())
                || componentsHaveConflicts(analysisReport.getPages())
                || componentsHaveConflicts(analysisReport.getPageTemplates())
                || componentsHaveConflicts(analysisReport.getContents())
                || componentsHaveConflicts(analysisReport.getContentTemplates())
                || componentsHaveConflicts(analysisReport.getContentTypes())
                || componentsHaveConflicts(analysisReport.getAssets())
                || componentsHaveConflicts(analysisReport.getDirectories())
                || componentsHaveConflicts(analysisReport.getResources())
                || componentsHaveConflicts(analysisReport.getPlugins())
                || componentsHaveConflicts(analysisReport.getCategories())
                || componentsHaveConflicts(analysisReport.getGroups())
                || componentsHaveConflicts(analysisReport.getLabels())
                || componentsHaveConflicts(analysisReport.getLanguages());
    }


    private static boolean componentsHaveConflicts(Map<String, Status> components) {

        return components.values().stream()
                .anyMatch(status -> status == Status.DIFF);
    }
}
