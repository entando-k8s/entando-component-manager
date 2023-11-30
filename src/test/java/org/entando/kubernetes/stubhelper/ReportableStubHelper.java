package org.entando.kubernetes.stubhelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.reportable.Reportable;
import org.entando.kubernetes.model.bundle.reportable.ReportableRemoteHandler;

public class ReportableStubHelper {

    public static final String WIDGET_CODE_1 = "WidONE";
    public static final String WIDGET_CODE_2 = "WidTWO";
    public static final String FRAGMENT_CODE_1 = "FragONE";
    public static final String FRAGMENT_CODE_2 = "FragTWO";
    public static final String PAGE_CODE_1 = "PagedONE";
    public static final String PAGE_CODE_2 = "PageTWO";
    public static final String PAGE_TEMPL_CODE_1 = "PageTemONE";
    public static final String PAGE_TEMPL_CODE_2 = "PageTemTWO";
    public static final String CONTENT_CODE_1 = "ContentONE";
    public static final String CONTENT_CODE_2 = "ContentTWO";
    public static final String CONTENT_TEMPL_CODE_1 = "ContentTempONE";
    public static final String CONTENT_TEMPL_CODE_2 = "ContentTempTWO";
    public static final String CONTENT_TYPE_CODE_1 = "ContentTypeONE";
    public static final String CONTENT_TYPE_CODE_2 = "ContentTypeTWO";
    public static final String ASSET_CODE_1 = "AsseONE";
    public static final String ASSET_CODE_2 = "AsseTWO";
    public static final String RESOURCE_CODE_1 = "ResouONE";
    public static final String RESOURCE_CODE_2 = "ResouTWO";
    public static final String PLUGIN_CODE_1 = "PlugONE";
    public static final String PLUGIN_CODE_2 = "PlugTWO";
    public static final String PLUGIN_CODE_3 = "Plug3";
    public static final String CATEGORY_CODE_1 = "CatONE";
    public static final String CATEGORY_CODE_2 = "CatTWO";
    public static final String GROUP_CODE_1 = "GroupNE";
    public static final String GROUP_CODE_2 = "GroupTWO";
    public static final String LABEL_CODE_1 = "LabelONE";
    public static final String LABEL_CODE_2 = "LabelTWO";
    public static final String LANG_CODE_1 = "LangONE";
    public static final String LANG_CODE_2 = "LangTWO";
    public static final String DIRECTORY_CODE_1 = "DirONE";
    public static final String DIRECTORY_CODE_2 = "DirTWO";

    public static List<Reportable> stubAllReportableListWithSha() {

        var reportableList = stubAllReportableList();
        reportableList.add(new Reportable(ComponentType.PLUGIN, ReportableRemoteHandler.ENTANDO_K8S_SERVICE,
                Arrays.asList(new Reportable.Component(PLUGIN_CODE_1, PluginStubHelper.PLUGIN_IMAGE_SHA),
                        new Reportable.Component(PLUGIN_CODE_2,
                                "sha256:AAAAf3443c577db1b1df3b47593b07895acab5ef582e5a661c2c1ade6f19ZZZZ"))));
        return reportableList;
    }

    public static List<Reportable> stubAllReportableListWithNoLink() {

        var reportableList = stubAllReportableList();
        reportableList.add(new Reportable(ComponentType.PLUGIN, ReportableRemoteHandler.ENTANDO_K8S_SERVICE,
                Arrays.asList(new Reportable.Component(PLUGIN_CODE_3, PluginStubHelper.PLUGIN_IMAGE_SHA),
                        new Reportable.Component(PLUGIN_CODE_2,
                                "sha256:AAAAf3443c577db1b1df3b47593b07895acab5ef582e5a661c2c1ade6f19ZZZZ"))));
        return reportableList;
    }

    public static List<Reportable> stubAllReportableListWithTag() {

        var reportableList = stubAllReportableList();
        reportableList.add(new Reportable(ComponentType.PLUGIN, ReportableRemoteHandler.ENTANDO_K8S_SERVICE,
                Arrays.asList(new Reportable.Component(PLUGIN_CODE_1, "6.0.0"),
                        new Reportable.Component(PLUGIN_CODE_2, "3.0.0"))));
        return reportableList;
    }

    private static List<Reportable> stubAllReportableList() {

        return new ArrayList<>(Arrays.asList(
                new Reportable(ComponentType.ASSET, Arrays.asList(ASSET_CODE_1, ASSET_CODE_2),
                        ReportableRemoteHandler.ENTANDO_CMS),
                new Reportable(ComponentType.CATEGORY, Arrays.asList(CATEGORY_CODE_1, CATEGORY_CODE_2),
                        ReportableRemoteHandler.ENTANDO_ENGINE),
                new Reportable(ComponentType.CONTENT, Arrays.asList(CONTENT_CODE_1, CONTENT_CODE_2),
                        ReportableRemoteHandler.ENTANDO_CMS),
                new Reportable(ComponentType.CONTENT_TEMPLATE,
                        Arrays.asList(CONTENT_TEMPL_CODE_1, CONTENT_TEMPL_CODE_2),
                        ReportableRemoteHandler.ENTANDO_CMS),
                new Reportable(ComponentType.CONTENT_TYPE, Arrays.asList(CONTENT_TYPE_CODE_1, CONTENT_TYPE_CODE_2),
                        ReportableRemoteHandler.ENTANDO_CMS),
                new Reportable(ComponentType.DIRECTORY, Arrays.asList(DIRECTORY_CODE_1, DIRECTORY_CODE_2),
                        ReportableRemoteHandler.ENTANDO_ENGINE),
                new Reportable(ComponentType.RESOURCE, Arrays.asList(RESOURCE_CODE_1, RESOURCE_CODE_2),
                        ReportableRemoteHandler.ENTANDO_K8S_SERVICE),
                new Reportable(ComponentType.FRAGMENT, Arrays.asList(FRAGMENT_CODE_1, FRAGMENT_CODE_2),
                        ReportableRemoteHandler.ENTANDO_K8S_SERVICE),
                new Reportable(ComponentType.GROUP, Arrays.asList(GROUP_CODE_1, GROUP_CODE_2),
                        ReportableRemoteHandler.ENTANDO_ENGINE),
                new Reportable(ComponentType.LABEL, Arrays.asList(LABEL_CODE_1, LABEL_CODE_2),
                        ReportableRemoteHandler.ENTANDO_ENGINE),
                new Reportable(ComponentType.LANGUAGE, Arrays.asList(LANG_CODE_1, LANG_CODE_2),
                        ReportableRemoteHandler.ENTANDO_ENGINE),
                new Reportable(ComponentType.PAGE, Arrays.asList(PAGE_CODE_1, PAGE_CODE_2),
                        ReportableRemoteHandler.ENTANDO_ENGINE),
                new Reportable(ComponentType.PAGE_TEMPLATE, Arrays.asList(PAGE_TEMPL_CODE_1, PAGE_TEMPL_CODE_2),
                        ReportableRemoteHandler.ENTANDO_ENGINE),
                new Reportable(ComponentType.WIDGET, Arrays.asList(WIDGET_CODE_1, WIDGET_CODE_2),
                        ReportableRemoteHandler.ENTANDO_ENGINE)));
    }
}
