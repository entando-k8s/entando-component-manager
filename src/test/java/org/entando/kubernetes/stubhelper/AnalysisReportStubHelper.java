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
    public static final Map.Entry<String, Status> CATEGORY_1_ENTRY = new SimpleEntry<>(CATEGORY_CODE_1, Status.CONFLICT);
    public static final Map.Entry<String, Status>CATEGORY_2_ENTRY = new SimpleEntry<>(CATEGORY_CODE_2, Status.NOT_FOUND);
    public static final Map.Entry<String, Status> FRAGMENT_1_ENTRY = new SimpleEntry<>(FRAGMENT_CODE_1, Status.CONFLICT);
    public static final Map.Entry<String, Status> FRAGMENT_2_ENTRY = new SimpleEntry<>(FRAGMENT_CODE_2, Status.NOT_FOUND);

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
}
