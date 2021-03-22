package org.entando.kubernetes.stubhelper;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import org.entando.kubernetes.controller.digitalexchange.job.model.ComponentInstallPlan;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.controller.digitalexchange.job.model.Status;

public class InstallPlanStubHelper {

    public static final Map.Entry<String, ComponentInstallPlan> WIDGET_1_COMP_INSTALL_PLAN_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.WIDGET_CODE_1, stubComponentInstallPlan(Status.DIFF));
    public static final Map.Entry<String, ComponentInstallPlan> WIDGET_2_COMP_INSTALL_PLAN_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.WIDGET_CODE_2, stubComponentInstallPlan(Status.NEW));
    public static final Map.Entry<String, ComponentInstallPlan> FRAGMENT_1_COMP_INSTALL_PLAN_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.FRAGMENT_CODE_1, stubComponentInstallPlan(Status.DIFF));
    public static final Map.Entry<String, ComponentInstallPlan> FRAGMENT_2_COMP_INSTALL_PLAN_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.FRAGMENT_CODE_2, stubComponentInstallPlan(Status.NEW));
    public static final Map.Entry<String, ComponentInstallPlan> PAGE_1_COMP_INSTALL_PLAN_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.PAGE_CODE_1, stubComponentInstallPlan(Status.DIFF));
    public static final Map.Entry<String, ComponentInstallPlan> PAGE_2_COMP_INSTALL_PLAN_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.PAGE_CODE_2, stubComponentInstallPlan(Status.NEW));
    public static final Map.Entry<String, ComponentInstallPlan> PAGE_TEMPLATE_1_COMP_INSTALL_PLAN_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.PAGE_TEMPL_CODE_1, stubComponentInstallPlan(Status.DIFF));
    public static final Map.Entry<String, ComponentInstallPlan> PAGE_TEMPLATE_2_COMP_INSTALL_PLAN_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.PAGE_TEMPL_CODE_2, stubComponentInstallPlan(Status.NEW));
    public static final Map.Entry<String, ComponentInstallPlan> CONTENT_1_COMP_INSTALL_PLAN_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.CONTENT_CODE_1, stubComponentInstallPlan(Status.DIFF));
    public static final Map.Entry<String, ComponentInstallPlan> CONTENT_2_COMP_INSTALL_PLAN_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.CONTENT_CODE_2, stubComponentInstallPlan(Status.NEW));
    public static final Map.Entry<String, ComponentInstallPlan> CONTENT_TEMPLATE_1_COMP_INSTALL_PLAN_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.CONTENT_TEMPL_CODE_1,
                    stubComponentInstallPlan(Status.DIFF));
    public static final Map.Entry<String, ComponentInstallPlan> CONTENT_TEMPLATE_2_COMP_INSTALL_PLAN_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.CONTENT_TEMPL_CODE_2, stubComponentInstallPlan(Status.NEW));
    public static final Map.Entry<String, ComponentInstallPlan> CONTENT_TYPE_1_COMP_INSTALL_PLAN_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.CONTENT_TYPE_CODE_1, stubComponentInstallPlan(Status.DIFF));
    public static final Map.Entry<String, ComponentInstallPlan> CONTENT_TYPE_2_COMP_INSTALL_PLAN_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.CONTENT_TYPE_CODE_2, stubComponentInstallPlan(Status.NEW));
    public static final Map.Entry<String, ComponentInstallPlan> ASSET_1_COMP_INSTALL_PLAN_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.ASSET_CODE_1, stubComponentInstallPlan(Status.DIFF));
    public static final Map.Entry<String, ComponentInstallPlan> ASSET_2_COMP_INSTALL_PLAN_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.ASSET_CODE_2, stubComponentInstallPlan(Status.NEW));
    public static final Map.Entry<String, ComponentInstallPlan> RESOURCE_1_COMP_INSTALL_PLAN_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.RESOURCE_CODE_1, stubComponentInstallPlan(Status.DIFF));
    public static final Map.Entry<String, ComponentInstallPlan> RESOURCE_2_COMP_INSTALL_PLAN_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.RESOURCE_CODE_2, stubComponentInstallPlan(Status.NEW));
    public static final Map.Entry<String, ComponentInstallPlan> PLUGIN_1_COMP_INSTALL_PLAN_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.PLUGIN_CODE_1, stubComponentInstallPlan(Status.DIFF));
    public static final Map.Entry<String, ComponentInstallPlan> PLUGIN_2_COMP_INSTALL_PLAN_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.PLUGIN_CODE_2, stubComponentInstallPlan(Status.NEW));
    public static final Map.Entry<String, ComponentInstallPlan> CATEGORY_1_COMP_INSTALL_PLAN_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.CATEGORY_CODE_1, stubComponentInstallPlan(Status.DIFF));
    public static final Map.Entry<String, ComponentInstallPlan> CATEGORY_2_COMP_INSTALL_PLAN_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.CATEGORY_CODE_2, stubComponentInstallPlan(Status.NEW));
    public static final Map.Entry<String, ComponentInstallPlan> GROUP_1_COMP_INSTALL_PLAN_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.GROUP_CODE_1, stubComponentInstallPlan(Status.DIFF));
    public static final Map.Entry<String, ComponentInstallPlan> GROUP_2_COMP_INSTALL_PLAN_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.GROUP_CODE_2, stubComponentInstallPlan(Status.NEW));
    public static final Map.Entry<String, ComponentInstallPlan> LABEL_1_COMP_INSTALL_PLAN_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.LABEL_CODE_1, stubComponentInstallPlan(Status.DIFF));
    public static final Map.Entry<String, ComponentInstallPlan> LABEL_2_COMP_INSTALL_PLAN_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.LABEL_CODE_2, stubComponentInstallPlan(Status.NEW));
    public static final Map.Entry<String, ComponentInstallPlan> LANGUAGE_1_COMP_INSTALL_PLAN_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.LANG_CODE_1, stubComponentInstallPlan(Status.DIFF));
    public static final Map.Entry<String, ComponentInstallPlan> LANGUAGE_2_COMP_INSTALL_PLAN_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.LANG_CODE_2, stubComponentInstallPlan(Status.NEW));
    public static final Map.Entry<String, ComponentInstallPlan> DIRECTORY_1_COMP_INSTALL_PLAN_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.DIRECTORY_CODE_1, stubComponentInstallPlan(Status.DIFF));
    public static final Map.Entry<String, ComponentInstallPlan> DIRECTORY_2_COMP_INSTALL_PLAN_ENTRY =
            new SimpleEntry<>(ReportableStubHelper.DIRECTORY_CODE_2, stubComponentInstallPlan(Status.NEW));

    public static ComponentInstallPlan stubComponentInstallPlan(Status status) {
        return ComponentInstallPlan.builder().status(status).build();
    }

    public static ComponentInstallPlan stubComponentInstallPlan(Status status, InstallAction action) {
        return ComponentInstallPlan.builder().status(status).action(action).build();
    }

    public static InstallPlan stubInstallPlanWithCategories() {
        return InstallPlan.builder()
                .categories(Map.ofEntries(CATEGORY_1_COMP_INSTALL_PLAN_ENTRY, CATEGORY_2_COMP_INSTALL_PLAN_ENTRY
                ))
                .build();
    }

    public static InstallPlan stubInstallPlanWithFragments() {
        return InstallPlan.builder()
                .hasConflicts(true)
                .fragments(Map.ofEntries(FRAGMENT_1_COMP_INSTALL_PLAN_ENTRY, FRAGMENT_2_COMP_INSTALL_PLAN_ENTRY))
                .build();
    }


    public static InstallPlan stubInstallPlanWithFragmentsAndCategories() {
        return InstallPlan.builder()
                .categories(Map.ofEntries(CATEGORY_1_COMP_INSTALL_PLAN_ENTRY, CATEGORY_2_COMP_INSTALL_PLAN_ENTRY))
                .fragments(Map.ofEntries(FRAGMENT_1_COMP_INSTALL_PLAN_ENTRY, FRAGMENT_2_COMP_INSTALL_PLAN_ENTRY))
                .build();
    }

    public static InstallPlan stubFullInstallPlan() {
        return InstallPlan.builder()
                .hasConflicts(true)
                .widgets(Map.ofEntries(WIDGET_1_COMP_INSTALL_PLAN_ENTRY, WIDGET_2_COMP_INSTALL_PLAN_ENTRY))
                .fragments(Map.ofEntries(FRAGMENT_1_COMP_INSTALL_PLAN_ENTRY, FRAGMENT_2_COMP_INSTALL_PLAN_ENTRY))
                .pages(Map.ofEntries(PAGE_1_COMP_INSTALL_PLAN_ENTRY, PAGE_2_COMP_INSTALL_PLAN_ENTRY))
                .pageTemplates(Map.ofEntries(PAGE_TEMPLATE_1_COMP_INSTALL_PLAN_ENTRY,
                        PAGE_TEMPLATE_2_COMP_INSTALL_PLAN_ENTRY))
                .contents(Map.ofEntries(CONTENT_1_COMP_INSTALL_PLAN_ENTRY, CONTENT_2_COMP_INSTALL_PLAN_ENTRY))
                .contentTemplates(Map.ofEntries(CONTENT_TEMPLATE_1_COMP_INSTALL_PLAN_ENTRY,
                        CONTENT_TEMPLATE_2_COMP_INSTALL_PLAN_ENTRY))
                .contentTypes(
                        Map.ofEntries(CONTENT_TYPE_1_COMP_INSTALL_PLAN_ENTRY, CONTENT_TYPE_2_COMP_INSTALL_PLAN_ENTRY))
                .assets(Map.ofEntries(ASSET_1_COMP_INSTALL_PLAN_ENTRY, ASSET_2_COMP_INSTALL_PLAN_ENTRY))
                // files and directories are both managed as resources by the remote handler
                .resources(Map.ofEntries(RESOURCE_1_COMP_INSTALL_PLAN_ENTRY, RESOURCE_2_COMP_INSTALL_PLAN_ENTRY,
                        DIRECTORY_1_COMP_INSTALL_PLAN_ENTRY, DIRECTORY_2_COMP_INSTALL_PLAN_ENTRY))
                .plugins(Map.ofEntries(PLUGIN_1_COMP_INSTALL_PLAN_ENTRY, PLUGIN_2_COMP_INSTALL_PLAN_ENTRY))
                .categories(Map.ofEntries(CATEGORY_1_COMP_INSTALL_PLAN_ENTRY, CATEGORY_2_COMP_INSTALL_PLAN_ENTRY))
                .groups(Map.ofEntries(GROUP_1_COMP_INSTALL_PLAN_ENTRY, GROUP_2_COMP_INSTALL_PLAN_ENTRY))
                .labels(Map.ofEntries(LABEL_1_COMP_INSTALL_PLAN_ENTRY, LABEL_2_COMP_INSTALL_PLAN_ENTRY))
                .languages(Map.ofEntries(LANGUAGE_1_COMP_INSTALL_PLAN_ENTRY, LANGUAGE_2_COMP_INSTALL_PLAN_ENTRY))
                .build();
    }


}
