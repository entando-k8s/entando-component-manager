package org.entando.kubernetes.stubhelper;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import org.entando.kubernetes.controller.digitalexchange.job.model.AnalysisReport;
import org.entando.kubernetes.controller.digitalexchange.job.model.AnalysisReport.Status;

public class AnalysisReportStubHelper {

    public static final String CATEGORY_CODE_1 = "CatONE";
    public static final String CATEGORY_CODE_2 = "CatTWO";
    public static final String FRAGMENT_CODE_1 = "FragONE";
    public static final String FRAGMENT_CODE_2 = "FragTWO";
    public static final String PLUGIN_CODE_1 = "PlugONE";
    public static final String PLUGIN_CODE_2 = "PlugTWO";
    public static final String ASSET_CODE_1 = "AsseONE";
    public static final String ASSET_CODE_2 = "AsseTWO";
    public static final String CONTENT_CODE_1 = "ContentONE";
    public static final String CONTENT_CODE_2 = "ContentTWO";
    public static final Map.Entry<String, Status> CATEGORY_1_ENTRY = new SimpleEntry<>(CATEGORY_CODE_1, Status.DIFF);
    public static final Map.Entry<String, Status>CATEGORY_2_ENTRY = new SimpleEntry<>(CATEGORY_CODE_2, Status.NEW);
    public static final Map.Entry<String, Status> FRAGMENT_1_ENTRY = new SimpleEntry<>(FRAGMENT_CODE_1, Status.DIFF);
    public static final Map.Entry<String, Status> FRAGMENT_2_ENTRY = new SimpleEntry<>(FRAGMENT_CODE_2, Status.NEW);
    public static final Map.Entry<String, Status> PLUGIN_1_ENTRY = new SimpleEntry<>(PLUGIN_CODE_1, Status.DIFF);
    public static final Map.Entry<String, Status> PLUGIN_2_ENTRY = new SimpleEntry<>(PLUGIN_CODE_2, Status.NEW);
    public static final Map.Entry<String, Status> ASSET_1_ENTRY = new SimpleEntry<>(ASSET_CODE_1, Status.DIFF);
    public static final Map.Entry<String, Status> ASSET_2_ENTRY = new SimpleEntry<>(ASSET_CODE_2, Status.NEW);
    public static final Map.Entry<String, Status> CONTENT_1_ENTRY = new SimpleEntry<>(CONTENT_CODE_1, Status.DIFF);
    public static final Map.Entry<String, Status> CONTENT_2_ENTRY = new SimpleEntry<>(CONTENT_CODE_2, Status.NEW);

    public static AnalysisReport stubAnalysisReportWithCategories() {
        return AnalysisReport.builder()
                .categories(Map.ofEntries(CATEGORY_1_ENTRY, CATEGORY_2_ENTRY))
                .build();
    }

    public static AnalysisReport stubAnalysisReportWithFragments() {
        return AnalysisReport.builder()
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
                .categories(Map.ofEntries(CATEGORY_1_ENTRY, CATEGORY_2_ENTRY))
                .fragments(Map.ofEntries(FRAGMENT_1_ENTRY, FRAGMENT_2_ENTRY))
                .build();
    }

    public static AnalysisReport stubAnalysisReportWithContents() {
        return AnalysisReport.builder()
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

}
