package org.entando.kubernetes.controller.digitalexchange.job.model;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.entando.kubernetes.assertionhelper.InstallPlanAssertionHelper.assertOnNormalizedComponentInstallPlan;

import org.entando.kubernetes.assertionhelper.InstallPlanAssertionHelper;
import org.entando.kubernetes.stubhelper.InstallPlanStubHelper;
import org.entando.kubernetes.stubhelper.ReportableStubHelper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class InstallWithPlansRequestTest {

    @Test
    @SuppressWarnings("java:S5961")
    void shouldNormalizeThoseComponentsWithStatusNewAndNoActionInTheReceivedInstallWithPlansRequest() {

        // given an InstallWithPlansRequest with some components with status NEw and no action
        InstallWithPlansRequest installWithPlansRequest = InstallPlanStubHelper.stubInstallWithPlanRequestToNormalize();

        // when we normalize it
        InstallWithPlansRequest normalized = installWithPlansRequest.normalize();

        // we should obtain a cloned InstallWithPlansRequest
        assertThat(normalized).isNotSameAs(installWithPlansRequest);
        assertThat(normalized.getVersion()).isEqualTo(installWithPlansRequest.getVersion());

        // with only those components with status NEW normalized with the default strategy CREATE
        assertOnNonNormalizedComponentInstallPlan(normalized.getWidgets().get(ReportableStubHelper.WIDGET_CODE_1));
        assertOnNormalizedComponentInstallPlan(normalized.getWidgets().get(ReportableStubHelper.WIDGET_CODE_2));
        assertOnNonNormalizedComponentInstallPlan(normalized.getFragments().get(ReportableStubHelper.FRAGMENT_CODE_1));
        assertOnNormalizedComponentInstallPlan(normalized.getFragments().get(ReportableStubHelper.FRAGMENT_CODE_2));
        assertOnNonNormalizedComponentInstallPlan(normalized.getPages().get(ReportableStubHelper.PAGE_CODE_1));
        assertOnNormalizedComponentInstallPlan(normalized.getPages().get(ReportableStubHelper.PAGE_CODE_2));
        assertOnNonNormalizedComponentInstallPlan(normalized.getPageTemplates().get(ReportableStubHelper.PAGE_TEMPL_CODE_1));
        assertOnNormalizedComponentInstallPlan(normalized.getPageTemplates().get(ReportableStubHelper.PAGE_TEMPL_CODE_2));
        assertOnNonNormalizedComponentInstallPlan(normalized.getContents().get(ReportableStubHelper.CONTENT_CODE_1));
        assertOnNormalizedComponentInstallPlan(normalized.getContents().get(ReportableStubHelper.CONTENT_CODE_2));
        assertOnNonNormalizedComponentInstallPlan(normalized.getContentTemplates().get(ReportableStubHelper.CONTENT_TEMPL_CODE_1));
        assertOnNormalizedComponentInstallPlan(normalized.getContentTemplates().get(ReportableStubHelper.CONTENT_TEMPL_CODE_2));
        assertOnNonNormalizedComponentInstallPlan(normalized.getContentTypes().get(ReportableStubHelper.CONTENT_TYPE_CODE_1));
        assertOnNormalizedComponentInstallPlan(normalized.getContentTypes().get(ReportableStubHelper.CONTENT_TYPE_CODE_2));
        assertOnNonNormalizedComponentInstallPlan(normalized.getAssets().get(ReportableStubHelper.ASSET_CODE_1));
        assertOnNormalizedComponentInstallPlan(normalized.getAssets().get(ReportableStubHelper.ASSET_CODE_2));
        assertOnNonNormalizedComponentInstallPlan(normalized.getResources().get(ReportableStubHelper.RESOURCE_CODE_1));
        assertOnNormalizedComponentInstallPlan(normalized.getResources().get(ReportableStubHelper.RESOURCE_CODE_2));
        assertOnNonNormalizedComponentInstallPlan(normalized.getPlugins().get(ReportableStubHelper.PLUGIN_CODE_1));
        assertOnNormalizedComponentInstallPlan(normalized.getPlugins().get(ReportableStubHelper.PLUGIN_CODE_2));
        assertOnNonNormalizedComponentInstallPlan(normalized.getCategories().get(ReportableStubHelper.CATEGORY_CODE_1));
        assertOnNormalizedComponentInstallPlan(normalized.getCategories().get(ReportableStubHelper.CATEGORY_CODE_2));
        assertOnNonNormalizedComponentInstallPlan(normalized.getGroups().get(ReportableStubHelper.GROUP_CODE_1));
        assertOnNormalizedComponentInstallPlan(normalized.getGroups().get(ReportableStubHelper.GROUP_CODE_2));
        assertOnNonNormalizedComponentInstallPlan(normalized.getLabels().get(ReportableStubHelper.LABEL_CODE_1));
        assertOnNormalizedComponentInstallPlan(normalized.getLabels().get(ReportableStubHelper.LABEL_CODE_2));
        assertOnNonNormalizedComponentInstallPlan(normalized.getLanguages().get(ReportableStubHelper.LANG_CODE_1));
        assertOnNormalizedComponentInstallPlan(normalized.getLanguages().get(ReportableStubHelper.LANG_CODE_2));
    }

    private void assertOnNonNormalizedComponentInstallPlan(ComponentInstallPlan componentInstallPlan) {
        InstallPlanAssertionHelper.assertOnComponentInstallPlan(componentInstallPlan, Status.DIFF, InstallAction.OVERRIDE);
    }
}
