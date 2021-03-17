package org.entando.kubernetes.stubhelper;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import org.entando.kubernetes.client.model.AnalysisReport;
import org.entando.kubernetes.controller.digitalexchange.job.model.Status;

public class AnalysisReportStubHelper {

    public static final Map.Entry<String, Status> WIDGET_1_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.WIDGET_CODE_1, Status.DIFF);
    public static final Map.Entry<String, Status> WIDGET_2_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.WIDGET_CODE_2, Status.NEW);
    public static final Map.Entry<String, Status> FRAGMENT_1_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.FRAGMENT_CODE_1, Status.DIFF);
    public static final Map.Entry<String, Status> FRAGMENT_2_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.FRAGMENT_CODE_2, Status.NEW);
    public static final Map.Entry<String, Status> PAGE_1_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.PAGE_CODE_1, Status.DIFF);
    public static final Map.Entry<String, Status> PAGE_2_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.PAGE_CODE_2, Status.NEW);
    public static final Map.Entry<String, Status> PAGE_TEMPLATE_1_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.PAGE_TEMPL_CODE_1, Status.DIFF);
    public static final Map.Entry<String, Status> PAGE_TEMPLATE_2_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.PAGE_TEMPL_CODE_2, Status.NEW);
    public static final Map.Entry<String, Status> CONTENT_1_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.CONTENT_CODE_1, Status.DIFF);
    public static final Map.Entry<String, Status> CONTENT_2_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.CONTENT_CODE_2, Status.NEW);
    public static final Map.Entry<String, Status> CONTENT_TEMPLATE_1_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.CONTENT_TEMPL_CODE_1, Status.DIFF);
    public static final Map.Entry<String, Status> CONTENT_TEMPLATE_2_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.CONTENT_TEMPL_CODE_2, Status.NEW);
    public static final Map.Entry<String, Status> CONTENT_TYPE_1_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.CONTENT_TYPE_CODE_1, Status.DIFF);
    public static final Map.Entry<String, Status> CONTENT_TYPE_2_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.CONTENT_TYPE_CODE_2, Status.NEW);
    public static final Map.Entry<String, Status> ASSET_1_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.ASSET_CODE_1, Status.DIFF);
    public static final Map.Entry<String, Status> ASSET_2_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.ASSET_CODE_2, Status.NEW);
    public static final Map.Entry<String, Status> RESOURCE_1_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.RESOURCE_CODE_1, Status.DIFF);
    public static final Map.Entry<String, Status> RESOURCE_2_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.RESOURCE_CODE_2, Status.NEW);
    public static final Map.Entry<String, Status> PLUGIN_1_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.PLUGIN_CODE_1, Status.DIFF);
    public static final Map.Entry<String, Status> PLUGIN_2_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.PLUGIN_CODE_2, Status.NEW);
    public static final Map.Entry<String, Status> CATEGORY_1_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.CATEGORY_CODE_1, Status.DIFF);
    public static final Map.Entry<String, Status> CATEGORY_2_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.CATEGORY_CODE_2, Status.NEW);
    public static final Map.Entry<String, Status> GROUP_1_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.GROUP_CODE_1, Status.DIFF);
    public static final Map.Entry<String, Status> GROUP_2_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.GROUP_CODE_2, Status.NEW);
    public static final Map.Entry<String, Status> LABEL_1_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.LABEL_CODE_1, Status.DIFF);
    public static final Map.Entry<String, Status> LABEL_2_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.LABEL_CODE_2, Status.NEW);
    public static final Map.Entry<String, Status> LANGUAGE_1_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.LANG_CODE_1, Status.DIFF);
    public static final Map.Entry<String, Status> LANGUAGE_2_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.LANG_CODE_2, Status.NEW);
    public static final Map.Entry<String, Status> DIRECTORY_1_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.DIRECTORY_CODE_1, Status.DIFF);
    public static final Map.Entry<String, Status> DIRECTORY_2_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.DIRECTORY_CODE_2, Status.NEW);


    public static AnalysisReport stubFullEngineAnalysisReport() {
        return AnalysisReport.builder()
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

    public static AnalysisReport stubAnalysisReportWithPlugins() {
        return AnalysisReport.builder()
                .plugins(Map.ofEntries(PLUGIN_1_ENTRY, PLUGIN_2_ENTRY))
                .build();
    }


    public static AnalysisReport stubAnalysisReportWithFragmentsAndCategories() {
        return AnalysisReport.builder()
                .categories(Map.ofEntries(CATEGORY_1_ENTRY, CATEGORY_2_ENTRY))
                .fragments(Map.ofEntries(FRAGMENT_1_ENTRY, FRAGMENT_2_ENTRY))
                .build();
    }


    public static AnalysisReport getCmsAnalysisReport() {
        return AnalysisReport.builder()
                .contents(Map.ofEntries(CONTENT_1_ENTRY, CONTENT_2_ENTRY))
                .contentTemplates(Map.ofEntries(CONTENT_TEMPLATE_1_ENTRY, CONTENT_TEMPLATE_2_ENTRY))
                .contentTypes(Map.ofEntries(CONTENT_TYPE_1_ENTRY, CONTENT_TYPE_2_ENTRY))
                .assets(Map.ofEntries(ASSET_1_ENTRY, ASSET_2_ENTRY))
                .build();
    }


    public static AnalysisReport stubFullK8SServiceAnalysisReport() {
        return AnalysisReport.builder()
                .plugins(Map.ofEntries(PLUGIN_1_ENTRY, PLUGIN_2_ENTRY))
                .build();
    }


    public static AnalysisReport stubFullAnalysisReport() {
        return AnalysisReport.builder()
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
}
