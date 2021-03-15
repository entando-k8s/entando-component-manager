package org.entando.kubernetes.stubhelper;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import org.entando.kubernetes.client.model.ClientAnalysisReport;
import org.entando.kubernetes.controller.digitalexchange.job.model.AnalysisReport;
import org.entando.kubernetes.controller.digitalexchange.job.model.AnalysisReportComponentResult;
import org.entando.kubernetes.controller.digitalexchange.job.model.Status;

public class AnalysisReportStubHelper {

    public static final Map.Entry<String, AnalysisReportComponentResult> WIDGET_1_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.WIDGET_CODE_1, stubAnalysisReportComponentResult(Status.DIFF));
    public static final Map.Entry<String, AnalysisReportComponentResult> WIDGET_2_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.WIDGET_CODE_2, stubAnalysisReportComponentResult(Status.NEW));
    public static final Map.Entry<String, AnalysisReportComponentResult> FRAGMENT_1_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.FRAGMENT_CODE_1, stubAnalysisReportComponentResult(Status.DIFF));
    public static final Map.Entry<String, AnalysisReportComponentResult> FRAGMENT_2_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.FRAGMENT_CODE_2, stubAnalysisReportComponentResult(Status.NEW));
    public static final Map.Entry<String, AnalysisReportComponentResult> PAGE_1_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.PAGE_CODE_1, stubAnalysisReportComponentResult(Status.DIFF));
    public static final Map.Entry<String, AnalysisReportComponentResult> PAGE_2_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.PAGE_CODE_2, stubAnalysisReportComponentResult(Status.NEW));
    public static final Map.Entry<String, AnalysisReportComponentResult> PAGE_TEMPLATE_1_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.PAGE_TEMPL_CODE_1, stubAnalysisReportComponentResult(Status.DIFF));
    public static final Map.Entry<String, AnalysisReportComponentResult> PAGE_TEMPLATE_2_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.PAGE_TEMPL_CODE_2, stubAnalysisReportComponentResult(Status.NEW));
    public static final Map.Entry<String, AnalysisReportComponentResult> CONTENT_1_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.CONTENT_CODE_1, stubAnalysisReportComponentResult(Status.DIFF));
    public static final Map.Entry<String, AnalysisReportComponentResult> CONTENT_2_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.CONTENT_CODE_2, stubAnalysisReportComponentResult(Status.NEW));
    public static final Map.Entry<String, AnalysisReportComponentResult> CONTENT_TEMPLATE_1_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.CONTENT_TEMPL_CODE_1, stubAnalysisReportComponentResult(Status.DIFF));
    public static final Map.Entry<String, AnalysisReportComponentResult> CONTENT_TEMPLATE_2_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.CONTENT_TEMPL_CODE_2, stubAnalysisReportComponentResult(Status.NEW));
    public static final Map.Entry<String, AnalysisReportComponentResult> CONTENT_TYPE_1_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.CONTENT_TYPE_CODE_1, stubAnalysisReportComponentResult(Status.DIFF));
    public static final Map.Entry<String, AnalysisReportComponentResult> CONTENT_TYPE_2_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.CONTENT_TYPE_CODE_2, stubAnalysisReportComponentResult(Status.NEW));
    public static final Map.Entry<String, AnalysisReportComponentResult> ASSET_1_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.ASSET_CODE_1, stubAnalysisReportComponentResult(Status.DIFF));
    public static final Map.Entry<String, AnalysisReportComponentResult> ASSET_2_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.ASSET_CODE_2, stubAnalysisReportComponentResult(Status.NEW));
    public static final Map.Entry<String, AnalysisReportComponentResult> RESOURCE_1_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.RESOURCE_CODE_1, stubAnalysisReportComponentResult(Status.DIFF));
    public static final Map.Entry<String, AnalysisReportComponentResult> RESOURCE_2_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.RESOURCE_CODE_2, stubAnalysisReportComponentResult(Status.NEW));
    public static final Map.Entry<String, AnalysisReportComponentResult> PLUGIN_1_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.PLUGIN_CODE_1, stubAnalysisReportComponentResult(Status.DIFF));
    public static final Map.Entry<String, AnalysisReportComponentResult> PLUGIN_2_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.PLUGIN_CODE_2, stubAnalysisReportComponentResult(Status.NEW));
    public static final Map.Entry<String, AnalysisReportComponentResult> CATEGORY_1_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.CATEGORY_CODE_1, stubAnalysisReportComponentResult(Status.DIFF));
    public static final Map.Entry<String, AnalysisReportComponentResult> CATEGORY_2_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.CATEGORY_CODE_2, stubAnalysisReportComponentResult(Status.NEW));
    public static final Map.Entry<String, AnalysisReportComponentResult> GROUP_1_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.GROUP_CODE_1, stubAnalysisReportComponentResult(Status.DIFF));
    public static final Map.Entry<String, AnalysisReportComponentResult> GROUP_2_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.GROUP_CODE_2, stubAnalysisReportComponentResult(Status.NEW));
    public static final Map.Entry<String, AnalysisReportComponentResult> LABEL_1_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.LABEL_CODE_1, stubAnalysisReportComponentResult(Status.DIFF));
    public static final Map.Entry<String, AnalysisReportComponentResult> LABEL_2_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.LABEL_CODE_2, stubAnalysisReportComponentResult(Status.NEW));
    public static final Map.Entry<String, AnalysisReportComponentResult> LANGUAGE_1_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.LANG_CODE_1, stubAnalysisReportComponentResult(Status.DIFF));
    public static final Map.Entry<String, AnalysisReportComponentResult> LANGUAGE_2_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.LANG_CODE_2, stubAnalysisReportComponentResult(Status.NEW));
    public static final Map.Entry<String, AnalysisReportComponentResult> DIRECTORY_1_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.DIRECTORY_CODE_1, stubAnalysisReportComponentResult(Status.DIFF));
    public static final Map.Entry<String, AnalysisReportComponentResult> DIRECTORY_2_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.DIRECTORY_CODE_2, stubAnalysisReportComponentResult(Status.NEW));

    public static AnalysisReportComponentResult stubAnalysisReportComponentResult(Status status) {
        return new AnalysisReportComponentResult(status, null, ReportableStubHelper.HASH);
    }

    public static AnalysisReport stubAnalysisReportWithCategories() {
        return AnalysisReport.builder()
                .categories(Map.ofEntries(CATEGORY_1_ENTRY, CATEGORY_2_ENTRY))
                .build();
    }

    public static AnalysisReport stubAnalysisReportWithFragments() {
        return AnalysisReport.builder()
                .hasConflicts(true)
                .fragments(Map.ofEntries(FRAGMENT_1_ENTRY, FRAGMENT_2_ENTRY))
                .build();
    }

    public static AnalysisReport stubAnalysisReportWithPlugins() {
        return AnalysisReport.builder()
                .plugins(Map.ofEntries(PLUGIN_1_ENTRY, PLUGIN_2_ENTRY))
                .build();
    }

    public static AnalysisReport stubAnalysisReportWithAssets() {
        return AnalysisReport.builder()
                .assets(Map.ofEntries(ASSET_1_ENTRY, ASSET_2_ENTRY))
                .build();
    }

    public static AnalysisReport stubAnalysisReportWithFragmentsAndCategories() {
        return AnalysisReport.builder()
                .hasConflicts(true)
                .categories(Map.ofEntries(CATEGORY_1_ENTRY, CATEGORY_2_ENTRY))
                .fragments(Map.ofEntries(FRAGMENT_1_ENTRY, FRAGMENT_2_ENTRY))
                .build();
    }

    public static AnalysisReport stubAnalysisReportWithContents() {
        return AnalysisReport.builder()
                .contents(Map.ofEntries(CONTENT_1_ENTRY, CONTENT_2_ENTRY))
                .build();
    }

    public static AnalysisReport stubAnalysisReportWithAssetsAndContents() {
        return AnalysisReport.builder()
                .assets(Map.ofEntries(ASSET_1_ENTRY, ASSET_2_ENTRY))
                .contents(Map.ofEntries(CONTENT_1_ENTRY, CONTENT_2_ENTRY))
                .build();
    }

    public static AnalysisReport stubAnalysisReportWithFragmentsAndCategoriesAndPluginsAndAssets() {
        return AnalysisReport.builder()
                .categories(Map.ofEntries(CATEGORY_1_ENTRY, CATEGORY_2_ENTRY))
                .fragments(Map.ofEntries(FRAGMENT_1_ENTRY, FRAGMENT_2_ENTRY))
                .plugins(Map.ofEntries(PLUGIN_1_ENTRY, PLUGIN_2_ENTRY))
                .assets(Map.ofEntries(ASSET_1_ENTRY, ASSET_2_ENTRY))
                .build();
    }

    public static AnalysisReport stubAnalysisReportWithFragmentsAndCategoriesAndPluginsAndAssetsAndContents() {
        return AnalysisReport.builder()
                .categories(Map.ofEntries(CATEGORY_1_ENTRY, CATEGORY_2_ENTRY))
                .fragments(Map.ofEntries(FRAGMENT_1_ENTRY, FRAGMENT_2_ENTRY))
                .plugins(Map.ofEntries(PLUGIN_1_ENTRY, PLUGIN_2_ENTRY))
                .assets(Map.ofEntries(ASSET_1_ENTRY, ASSET_2_ENTRY))
                .contents(Map.ofEntries(CONTENT_1_ENTRY, CONTENT_2_ENTRY))
                .build();
    }

    public static AnalysisReport stubFullEngineAnalysisReport() {
        return AnalysisReport.builder()
                .hasConflicts(true)
                .widgets(Map.ofEntries(WIDGET_1_ENTRY, WIDGET_2_ENTRY))
                .fragments(Map.ofEntries(FRAGMENT_1_ENTRY, FRAGMENT_2_ENTRY))
                .pages(Map.ofEntries(PAGE_1_ENTRY, PAGE_2_ENTRY))
                .pageTemplates(Map.ofEntries(PAGE_TEMPLATE_1_ENTRY, PAGE_TEMPLATE_2_ENTRY))
                // files and directories are both managed as resources by the remote handler
                .resources(Map.ofEntries(RESOURCE_1_ENTRY, RESOURCE_2_ENTRY, DIRECTORY_1_ENTRY, DIRECTORY_2_ENTRY))
                .categories(Map.ofEntries(CATEGORY_1_ENTRY, CATEGORY_2_ENTRY))
                .groups(Map.ofEntries(GROUP_1_ENTRY, GROUP_2_ENTRY))
                .labels(Map.ofEntries(LABEL_1_ENTRY, LABEL_2_ENTRY))
                .languages(Map.ofEntries(LANGUAGE_1_ENTRY, LANGUAGE_2_ENTRY))
                .build();
    }

    public static AnalysisReport stubFullCMSAnalysisReport() {
        return AnalysisReport.builder()
                .hasConflicts(true)
                .contents(Map.ofEntries(CONTENT_1_ENTRY, CONTENT_2_ENTRY))
                .contentTemplates(Map.ofEntries(CONTENT_TEMPLATE_1_ENTRY, CONTENT_TEMPLATE_2_ENTRY))
                .contentTypes(Map.ofEntries(CONTENT_TYPE_1_ENTRY, CONTENT_TYPE_2_ENTRY))
                .assets(Map.ofEntries(ASSET_1_ENTRY, ASSET_2_ENTRY))
                .build();
    }

    public static AnalysisReport stubFullK8SServiceAnalysisReport() {
        return AnalysisReport.builder()
                .hasConflicts(true)
                .plugins(Map.ofEntries(PLUGIN_1_ENTRY, PLUGIN_2_ENTRY))
                .build();
    }

    public static AnalysisReport stubFullAnalysisReport() {
        return AnalysisReport.builder()
                .hasConflicts(true)
                .widgets(Map.ofEntries(WIDGET_1_ENTRY, WIDGET_2_ENTRY))
                .fragments(Map.ofEntries(FRAGMENT_1_ENTRY, FRAGMENT_2_ENTRY))
                .pages(Map.ofEntries(PAGE_1_ENTRY, PAGE_2_ENTRY))
                .pageTemplates(Map.ofEntries(PAGE_TEMPLATE_1_ENTRY, PAGE_TEMPLATE_2_ENTRY))
                .contents(Map.ofEntries(CONTENT_1_ENTRY, CONTENT_2_ENTRY))
                .contentTemplates(Map.ofEntries(CONTENT_TEMPLATE_1_ENTRY, CONTENT_TEMPLATE_2_ENTRY))
                .contentTypes(Map.ofEntries(CONTENT_TYPE_1_ENTRY, CONTENT_TYPE_2_ENTRY))
                .assets(Map.ofEntries(ASSET_1_ENTRY, ASSET_2_ENTRY))
                // files and directories are both managed as resources by the remote handler
                .resources(Map.ofEntries(RESOURCE_1_ENTRY, RESOURCE_2_ENTRY, DIRECTORY_1_ENTRY, DIRECTORY_2_ENTRY))
                .plugins(Map.ofEntries(PLUGIN_1_ENTRY, PLUGIN_2_ENTRY))
                .categories(Map.ofEntries(CATEGORY_1_ENTRY, CATEGORY_2_ENTRY))
                .groups(Map.ofEntries(GROUP_1_ENTRY, GROUP_2_ENTRY))
                .labels(Map.ofEntries(LABEL_1_ENTRY, LABEL_2_ENTRY))
                .languages(Map.ofEntries(LANGUAGE_1_ENTRY, LANGUAGE_2_ENTRY))
                .build();
    }

    public static ClientAnalysisReport stubFullClientAnalysisReport() {

        return ClientAnalysisReport.builder()
                .widgets(Map.of(ReportableStubHelper.WIDGET_CODE_1, Status.DIFF, ReportableStubHelper.WIDGET_CODE_2,
                        Status.EQUAL))
                .fragments(
                        Map.of(ReportableStubHelper.FRAGMENT_CODE_1, Status.DIFF, ReportableStubHelper.FRAGMENT_CODE_2,
                                Status.NEW))
                .pages(Map.of(ReportableStubHelper.PAGE_CODE_1, Status.DIFF, ReportableStubHelper.PAGE_CODE_2,
                        Status.EQUAL))
                .pageTemplates(Map.of(ReportableStubHelper.PAGE_TEMPL_CODE_1, Status.DIFF,
                        ReportableStubHelper.PAGE_TEMPL_CODE_2, Status.NEW))
                .contents(Map.of(ReportableStubHelper.CONTENT_CODE_1, Status.DIFF, ReportableStubHelper.CONTENT_CODE_2,
                        Status.EQUAL))
                .contentTemplates(Map.of(ReportableStubHelper.CONTENT_TEMPL_CODE_1, Status.DIFF,
                        ReportableStubHelper.CONTENT_CODE_2, Status.NEW))
                .contentTypes(Map.of(ReportableStubHelper.CONTENT_TYPE_CODE_1, Status.DIFF,
                        ReportableStubHelper.CONTENT_TYPE_CODE_2, Status.EQUAL))
                .assets(Map.of(ReportableStubHelper.ASSET_CODE_1, Status.DIFF, ReportableStubHelper.ASSET_CODE_2,
                        Status.NEW))
                .resources(
                        Map.of(ReportableStubHelper.RESOURCE_CODE_1, Status.DIFF, ReportableStubHelper.RESOURCE_CODE_2,
                                Status.EQUAL, ReportableStubHelper.DIRECTORY_CODE_1, Status.DIFF,
                                ReportableStubHelper.DIRECTORY_CODE_2, Status.NEW))
                .plugins(Map.of(ReportableStubHelper.PLUGIN_CODE_1, Status.DIFF, ReportableStubHelper.PLUGIN_CODE_2,
                        Status.EQUAL))
                .categories(
                        Map.of(ReportableStubHelper.CATEGORY_CODE_1, Status.DIFF, ReportableStubHelper.CATEGORY_CODE_2,
                                Status.NEW))
                .groups(Map.of(ReportableStubHelper.GROUP_CODE_1, Status.DIFF, ReportableStubHelper.GROUP_CODE_2,
                        Status.EQUAL))
                .labels(Map.of(ReportableStubHelper.LABEL_CODE_1, Status.DIFF, ReportableStubHelper.LABEL_CODE_2,
                        Status.NEW))
                .languages(Map.of(ReportableStubHelper.LANG_CODE_1, Status.DIFF, ReportableStubHelper.LANG_CODE_2,
                        Status.EQUAL))
                .build();
    }

}
