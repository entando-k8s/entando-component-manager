package org.entando.kubernetes.client.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.entando.kubernetes.model.bundle.reportable.Reportable;
import org.entando.kubernetes.stubhelper.ReportableStubHelper;
import org.junit.jupiter.api.Test;

class InstallPlanClientRequestFactoryTest {

    private final List<Reportable> reportableList = ReportableStubHelper.stubAllReportableListWithTag();

    /**********************************************************************************************************
     * ENGINE REQUEST.
     *********************************************************************************************************/

    @Test
    void createEngineAnalysisReportRequestShouldReturnTheRightBeanProperlyPopulated() {

        EngineAnalysisReportClientRequest engineAnalysisReportRequest = (EngineAnalysisReportClientRequest) AnalysisReportClientRequestFactory
                .anAnalysisReportClientRequest()
                .reportableList(reportableList)
                .createEngineAnalysisReportRequest();

        assertThat(engineAnalysisReportRequest.getWidgets()).containsExactly(
                ReportableStubHelper.WIDGET_CODE_1, ReportableStubHelper.WIDGET_CODE_2);
        assertThat(engineAnalysisReportRequest.getFragments()).containsExactly(
                ReportableStubHelper.FRAGMENT_CODE_1, ReportableStubHelper.FRAGMENT_CODE_2);
        assertThat(engineAnalysisReportRequest.getPages()).containsExactly(
                ReportableStubHelper.PAGE_CODE_1, ReportableStubHelper.PAGE_CODE_2);
        assertThat(engineAnalysisReportRequest.getPageTemplates()).containsExactly(
                ReportableStubHelper.PAGE_TEMPL_CODE_1, ReportableStubHelper.PAGE_TEMPL_CODE_2);
        // files and directories are both managed as resources by the remote handler
        assertThat(engineAnalysisReportRequest.getResources()).containsExactlyInAnyOrder(
                ReportableStubHelper.RESOURCE_CODE_1, ReportableStubHelper.RESOURCE_CODE_2);
        assertThat(engineAnalysisReportRequest.getDirectories()).containsExactlyInAnyOrder(
                ReportableStubHelper.DIRECTORY_CODE_1, ReportableStubHelper.DIRECTORY_CODE_2);
        assertThat(engineAnalysisReportRequest.getCategories()).containsExactly(
                ReportableStubHelper.CATEGORY_CODE_1, ReportableStubHelper.CATEGORY_CODE_2);
        assertThat(engineAnalysisReportRequest.getGroups()).containsExactly(
                ReportableStubHelper.GROUP_CODE_1, ReportableStubHelper.GROUP_CODE_2);
        assertThat(engineAnalysisReportRequest.getLabels()).containsExactly(
                ReportableStubHelper.LABEL_CODE_1, ReportableStubHelper.LABEL_CODE_2);
        assertThat(engineAnalysisReportRequest.getLanguages()).containsExactly(
                ReportableStubHelper.LANG_CODE_1, ReportableStubHelper.LANG_CODE_2);
    }

    @Test
    void createEngineAnalysisReportRequestWithNullOrEmptyReportableListShouldReturnAnEmptyRequest() {

        EngineAnalysisReportClientRequest engineAnalysisReportRequestNull = (EngineAnalysisReportClientRequest) AnalysisReportClientRequestFactory
                .anAnalysisReportClientRequest()
                .reportableList(null)
                .createEngineAnalysisReportRequest();

        assertOnEmptyEngineAnalysisReportRequest(engineAnalysisReportRequestNull);

        EngineAnalysisReportClientRequest engineAnalysisReportRequestEmpty = (EngineAnalysisReportClientRequest) AnalysisReportClientRequestFactory
                .anAnalysisReportClientRequest()
                .reportableList(new ArrayList<>())
                .createEngineAnalysisReportRequest();

        assertOnEmptyEngineAnalysisReportRequest(engineAnalysisReportRequestEmpty);
    }

    private void assertOnEmptyEngineAnalysisReportRequest(EngineAnalysisReportClientRequest reportClientRequest) {
        assertThat(reportClientRequest.getWidgets()).isEmpty();
        assertThat(reportClientRequest.getFragments()).isEmpty();
        assertThat(reportClientRequest.getPages()).isEmpty();
        assertThat(reportClientRequest.getPageTemplates()).isEmpty();
        assertThat(reportClientRequest.getResources()).isEmpty();
        assertThat(reportClientRequest.getCategories()).isEmpty();
        assertThat(reportClientRequest.getGroups()).isEmpty();
        assertThat(reportClientRequest.getLabels()).isEmpty();
        assertThat(reportClientRequest.getLanguages()).isEmpty();
    }

    /**********************************************************************************************************
     * CMS REQUEST.
     *********************************************************************************************************/

    @Test
    void createCMSAnalysisReportRequestShouldReturnTheRightBeanProperlyPopulated() {

        CMSAnalysisReportClientRequest cmsAnalysisReportRequest = (CMSAnalysisReportClientRequest) AnalysisReportClientRequestFactory
                .anAnalysisReportClientRequest()
                .reportableList(reportableList)
                .createCMSAnalysisReportRequest();

        assertThat(cmsAnalysisReportRequest.getContents()).containsExactly(
                ReportableStubHelper.CONTENT_CODE_1, ReportableStubHelper.CONTENT_CODE_2);
        assertThat(cmsAnalysisReportRequest.getContentTemplates()).containsExactly(
                ReportableStubHelper.CONTENT_TEMPL_CODE_1, ReportableStubHelper.CONTENT_TEMPL_CODE_2);
        assertThat(cmsAnalysisReportRequest.getContentTypes()).containsExactly(
                ReportableStubHelper.CONTENT_TYPE_CODE_1, ReportableStubHelper.CONTENT_TYPE_CODE_2);
        assertThat(cmsAnalysisReportRequest.getAssets()).containsExactly(
                ReportableStubHelper.ASSET_CODE_1, ReportableStubHelper.ASSET_CODE_2);
    }

    @Test
    void createCMSAnalysisReportRequestWithNullOrEmptyReportableListShouldReturnAnEmptyRequest() {

        CMSAnalysisReportClientRequest cmsAnalysisReportRequestNull = (CMSAnalysisReportClientRequest) AnalysisReportClientRequestFactory
                .anAnalysisReportClientRequest()
                .reportableList(null)
                .createCMSAnalysisReportRequest();

        assertOnEmptyCMSAnalysisReportRequest(cmsAnalysisReportRequestNull);

        CMSAnalysisReportClientRequest cmsAnalysisReportRequestEmpty = (CMSAnalysisReportClientRequest) AnalysisReportClientRequestFactory
                .anAnalysisReportClientRequest()
                .reportableList(new ArrayList<>())
                .createCMSAnalysisReportRequest();

        assertOnEmptyCMSAnalysisReportRequest(cmsAnalysisReportRequestEmpty);
    }

    private void assertOnEmptyCMSAnalysisReportRequest(CMSAnalysisReportClientRequest reportClientRequest) {
        assertThat(reportClientRequest.getAssets()).isEmpty();
        assertThat(reportClientRequest.getContentTypes()).isEmpty();
        assertThat(reportClientRequest.getContentTemplates()).isEmpty();
        assertThat(reportClientRequest.getContents()).isEmpty();
    }
}
