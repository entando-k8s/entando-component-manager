package org.entando.kubernetes.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.client.WireMock;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.entando.kubernetes.assertionhelper.AnalysisReportAssertionHelper;
import org.entando.kubernetes.client.core.DefaultEntandoCoreClient;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.client.model.AnalysisReport;
import org.entando.kubernetes.exception.digitalexchange.ReportAnalysisException;
import org.entando.kubernetes.exception.web.HttpException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.ContentTemplateDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FragmentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FrameDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.LabelDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageTemplateConfigurationDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageTemplateDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.contenttype.ContentTypeDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.reportable.Reportable;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage;
import org.entando.kubernetes.stubhelper.AnalysisReportStubHelper;
import org.entando.kubernetes.utils.EntandoCoreMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

@Tag("unit")
class EntandoCoreClientTest {

    public static final String CODE = "code";
    private EntandoCoreClient client;
    private EntandoCoreMockServer coreMockServer;

    @BeforeEach
    public void setup() {
        coreMockServer = new EntandoCoreMockServer();
        String keycloakClientId = "clientId";
        String keycloakClientSecret = "clientSecret";
        String keycloakAuthEndpoint = coreMockServer.getApiRoot() + "/auth/protocol/openid-connect/auth";
        String entandoCoreUrl = coreMockServer.getApiRoot() + "/";
        this.client = new DefaultEntandoCoreClient(keycloakClientId, keycloakClientSecret, keycloakAuthEndpoint,
                entandoCoreUrl);
    }

    @AfterEach
    public void reset() {
        coreMockServer.getInnerServer().resetAll();
        coreMockServer.stop();
    }

    @Test
    @Disabled("Can't make wiremock generic endpoint read code from url")
    void shouldWorkWithGenericComponentUsage() {
        coreMockServer = coreMockServer.withGenericComponentsUsageSupport();
        EntandoCoreComponentUsage widgetUsage = this.client.getWidgetUsage("my-new-widget");
        assertThat(widgetUsage.getCode()).isEqualTo("my-new-widget");
        assertThat(widgetUsage.getType()).isEqualTo("widgets");
        assertThat(widgetUsage.getUsage()).isEqualTo(1);

    }

    @Test
    void shouldGetUsageForWidgets() {
        coreMockServer = coreMockServer.withComponentUsageSupport(ComponentType.WIDGET, "my-widget", 11);
        EntandoCoreComponentUsage widgetUsage = this.client.getWidgetUsage("my-widget");
        assertThat(widgetUsage.getCode()).isEqualTo("my-widget");
        assertThat(widgetUsage.getType()).isEqualTo("widgets");
        assertThat(widgetUsage.getUsage()).isEqualTo(11);
    }

    @Test
    void shouldGetUsageForPage() {
        coreMockServer = coreMockServer.withComponentUsageSupport(ComponentType.PAGE, "my-page", 3);
        EntandoCoreComponentUsage widgetUsage = this.client.getPageUsage("my-page");
        assertThat(widgetUsage.getCode()).isEqualTo("my-page");
        assertThat(widgetUsage.getType()).isEqualTo("pages");
        assertThat(widgetUsage.getUsage()).isEqualTo(3);
    }

    @Test
    void shouldGetUsageForPageTemplates() {
        coreMockServer = coreMockServer.withComponentUsageSupport(ComponentType.PAGE_TEMPLATE, "my-pagemodel", 1);
        EntandoCoreComponentUsage widgetUsage = this.client.getPageModelUsage("my-pagemodel");
        assertThat(widgetUsage.getCode()).isEqualTo("my-pagemodel");
        assertThat(widgetUsage.getType()).isEqualTo("pageModels");
        assertThat(widgetUsage.getUsage()).isEqualTo(1);
    }

    @Test
    void shouldGetUsageForFragments() {
        coreMockServer = coreMockServer.withComponentUsageSupport(ComponentType.FRAGMENT, "fragment-101", 1);
        EntandoCoreComponentUsage widgetUsage = this.client.getFragmentUsage("fragment-101");
        assertThat(widgetUsage.getCode()).isEqualTo("fragment-101");
        assertThat(widgetUsage.getType()).isEqualTo("fragments");
        assertThat(widgetUsage.getUsage()).isEqualTo(1);
    }

    @Test
    void shouldGetUsageForContentTypes() {
        coreMockServer = coreMockServer.withComponentUsageSupport(ComponentType.CONTENT_TYPE, "CT092", 2);
        EntandoCoreComponentUsage widgetUsage = this.client.getContentTypeUsage("CT092");
        assertThat(widgetUsage.getCode()).isEqualTo("CT092");
        assertThat(widgetUsage.getType()).isEqualTo("contentTypes");
        assertThat(widgetUsage.getUsage()).isEqualTo(2);
    }

    @Test
    void shouldGetUsageForContentTemplate() {
        coreMockServer = coreMockServer.withComponentUsageSupport(ComponentType.CONTENT_TEMPLATE, "12345", 8);
        EntandoCoreComponentUsage contentModelUsage = this.client.getContentModelUsage("12345");
        assertThat(contentModelUsage.getCode()).isEqualTo("12345");
        assertThat(contentModelUsage.getType()).isEqualTo("contentTemplates");
        assertThat(contentModelUsage.getUsage()).isEqualTo(8);
    }

    @Test
    void getUsageReceiving3xxStatusCodeShouldBeManagedInTheCoreClient() {
        coreMockServer = coreMockServer
                .withFailingComponentUsageSupport(ComponentType.CONTENT_TEMPLATE, "12345", HttpStatus.NOT_MODIFIED);
        assertThrows(HttpException.class, () -> this.client.getContentModelUsage("12345"));
    }

    @Test
    void getUsageReceiving4xxStatusCodeShouldThrowAndExceptionByRestTemplateItself() {
        coreMockServer = coreMockServer
                .withFailingComponentUsageSupport(ComponentType.CONTENT_TEMPLATE, "12345", HttpStatus.NOT_FOUND);
        assertThrows(HttpClientErrorException.NotFound.class, () -> this.client.getContentModelUsage("12345"));
    }


    @Test
    void registerWidget() {
        WidgetDescriptor wd = new WidgetDescriptor();
        coreMockServer = coreMockServer.withGenericSupport(EntandoCoreMockServer.WIDGET_ENDPOINT, WireMock::post);
        this.client.createWidget(wd);
        coreMockServer.verify(EntandoCoreMockServer.WIDGET_ENDPOINT, WireMock::postRequestedFor);
    }


    @Test
    void deleteWidget() {
        coreMockServer = coreMockServer
                .withGenericSupport(EntandoCoreMockServer.WIDGET_ENDPOINT, CODE, WireMock::delete);
        this.client.deleteWidget(CODE);
        coreMockServer.verify(EntandoCoreMockServer.WIDGET_ENDPOINT + "/" + CODE, WireMock::deleteRequestedFor);
    }

    @Test
    void registerFragment() {
        FragmentDescriptor fd = new FragmentDescriptor();
        coreMockServer = coreMockServer.withGenericSupport(EntandoCoreMockServer.FRAGMENT_ENDPOINT, WireMock::post);
        this.client.createFragment(fd);
        coreMockServer.verify(EntandoCoreMockServer.FRAGMENT_ENDPOINT, WireMock::postRequestedFor);
    }


    @Test
    void deleteFragment() {
        WidgetDescriptor wd = new WidgetDescriptor();
        coreMockServer = coreMockServer
                .withGenericSupport(EntandoCoreMockServer.FRAGMENT_ENDPOINT, CODE, WireMock::delete);
        this.client.deleteFragment(CODE);
        coreMockServer.verify(EntandoCoreMockServer.FRAGMENT_ENDPOINT + "/" + CODE, WireMock::deleteRequestedFor);
    }

    @Test
    void registerLabel() {
        LabelDescriptor ld = new LabelDescriptor();
        coreMockServer = coreMockServer.withGenericSupport(EntandoCoreMockServer.LABEL_ENDPOINT, WireMock::post);
        this.client.createLabel(ld);
        coreMockServer.verify(EntandoCoreMockServer.LABEL_ENDPOINT, WireMock::postRequestedFor);
    }


    @Test
    void deleteLabel() {
        coreMockServer = coreMockServer
                .withGenericSupport(EntandoCoreMockServer.LABEL_ENDPOINT, CODE, WireMock::delete);
        this.client.deleteLabel(CODE);
        coreMockServer.verify(EntandoCoreMockServer.LABEL_ENDPOINT + "/" + CODE, WireMock::deleteRequestedFor);
    }

    @Test
    void registerPage() {
        PageDescriptor pd = new PageDescriptor();
        coreMockServer = coreMockServer.withGenericSupport(EntandoCoreMockServer.PAGE_ENDPOINT, WireMock::post);
        this.client.createPage(pd);
        coreMockServer.verify(EntandoCoreMockServer.PAGE_ENDPOINT, WireMock::postRequestedFor);
    }


    @Test
    void deletePage() {
        coreMockServer = coreMockServer.withGenericSupport(EntandoCoreMockServer.PAGE_ENDPOINT, CODE, WireMock::delete);
        this.client.deletePage(CODE);
        coreMockServer.verify(EntandoCoreMockServer.PAGE_ENDPOINT + "/" + CODE, WireMock::deleteRequestedFor);
    }

    @Test
    void registerPageTemplate() {

        PageTemplateConfigurationDescriptor pageTemplateConfigurationDescriptor = PageTemplateConfigurationDescriptor
                .builder()
                .frames(Arrays.asList(new FrameDescriptor()))
                .build();
        PageTemplateDescriptor ptd = PageTemplateDescriptor.builder()
                .configuration(pageTemplateConfigurationDescriptor)
                .build();

        coreMockServer = coreMockServer
                .withGenericSupport(EntandoCoreMockServer.PAGE_TEMPLATE_ENDPOINT, WireMock::post);
        this.client.createPageTemplate(ptd);
        coreMockServer.verify(EntandoCoreMockServer.PAGE_TEMPLATE_ENDPOINT, WireMock::postRequestedFor);
    }


    @Test
    void deletePageTemplate() {
        coreMockServer = coreMockServer
                .withGenericSupport(EntandoCoreMockServer.PAGE_TEMPLATE_ENDPOINT, CODE, WireMock::delete);
        this.client.deletePageModel(CODE);
        coreMockServer.verify(EntandoCoreMockServer.PAGE_TEMPLATE_ENDPOINT + "/" + CODE, WireMock::deleteRequestedFor);
    }

    @Test
    void registerContentTemplate() {
        ContentTemplateDescriptor ctd = new ContentTemplateDescriptor();
        coreMockServer = coreMockServer
                .withGenericSupport(EntandoCoreMockServer.CONTENT_TEMPLATE_ENDPOINT, WireMock::post);
        this.client.createContentTemplate(ctd);
        coreMockServer.verify(EntandoCoreMockServer.CONTENT_TEMPLATE_ENDPOINT, WireMock::postRequestedFor);
    }


    @Test
    void deleteContentTemplate() {
        coreMockServer = coreMockServer
                .withGenericSupport(EntandoCoreMockServer.CONTENT_TEMPLATE_ENDPOINT, CODE, WireMock::delete);
        this.client.deleteContentModel(CODE);
        coreMockServer
                .verify(EntandoCoreMockServer.CONTENT_TEMPLATE_ENDPOINT + "/" + CODE, WireMock::deleteRequestedFor);
    }

    @Test
    void registerContentType() {
        ContentTypeDescriptor ctd = new ContentTypeDescriptor();
        coreMockServer = coreMockServer.withGenericSupport(EntandoCoreMockServer.CONTENT_TYPE_ENDPOINT, WireMock::post);
        this.client.createContentType(ctd);
        coreMockServer.verify(EntandoCoreMockServer.CONTENT_TYPE_ENDPOINT, WireMock::postRequestedFor);
    }


    @Test
    void deleteContentType() {
        coreMockServer = coreMockServer
                .withGenericSupport(EntandoCoreMockServer.CONTENT_TYPE_ENDPOINT, CODE, WireMock::delete);
        this.client.deleteContentType(CODE);
        coreMockServer.verify(EntandoCoreMockServer.CONTENT_TYPE_ENDPOINT + "/" + CODE, WireMock::deleteRequestedFor);
    }

    @Test
    void deleteNotFoundComponentShouldSucceed() {

        // TODO check better this test

        this.getTestDeleteEndpointMap().forEach((key, value) -> {
            stubForDeleteNotFoundComponent(key);
            assertAll(() -> value.accept(CODE));
            coreMockServer.verify(key + "/" + CODE, WireMock::deleteRequestedFor);
        });
    }

    @Test
    void deleteForbiddenComponentShouldSucceed() {

        // TODO check better this test

        this.getTestDeleteEndpointMap().forEach((key, value) -> {
            stubForDeleteForbiddenComponent(key);
            assertAll(() -> value.accept(CODE));
            coreMockServer.verify(key + "/" + CODE, WireMock::deleteRequestedFor);
        });
    }

    @Test
    void disablingALanguageThatReturns409TheDisableWillSucceed() {

        coreMockServer.getInnerServer()
                .stubFor(WireMock.put(urlEqualTo(EntandoCoreMockServer.LANGUAGE_ENDPOINT + "/" + CODE))
                        .willReturn(
                                aResponse()
                                        .withStatus(409)
                                        .withHeader("Content-Type", "application/json")
                        ));

        assertAll(() -> client.disableLanguage(CODE));
    }

    @Test
    void receivingAValidResponseFromEntandoEngineShouldReturnValidAnalysisReport() {

        coreMockServer.withEngineAnalysisReportSupport(null);

        AnalysisReport engineAnalysisReport = this.client.getEngineAnalysisReport(new ArrayList<>());
        AnalysisReport expected = AnalysisReportStubHelper.stubAnalysisReportWithFragmentsAndCategories();
        AnalysisReportAssertionHelper.assertOnAnalysisReports(expected, engineAnalysisReport);
    }


    @Test
    void receivingAValidResponseFromCMSEngineShouldReturnValidAnalysisReport() {

        coreMockServer.withCMSAnalysisReportSupport(null);

        AnalysisReport engineAnalysisReport = this.client.getCMSAnalysisReport(new ArrayList<>());
        AnalysisReport expected = AnalysisReportStubHelper.stubAnalysisReportWithFragmentsAndCategories();
        AnalysisReportAssertionHelper.assertOnAnalysisReports(expected, engineAnalysisReport);
    }

    @Test
    void shouldThrowAReportAnalysisExceptionIfARequestFails() {

        coreMockServer.withFailingEngineAnalysisReportSupport();
        List<Reportable> reportableList = new ArrayList<>();

        Assertions.assertThrows(ReportAnalysisException.class,
                () -> this.client.getEngineAnalysisReport(reportableList));
    }




    private Map<String, Consumer<String>> getTestDeleteEndpointMap() {
        return Map.ofEntries(
                new AbstractMap.SimpleEntry<String, Consumer<String>>(EntandoCoreMockServer.CATEGORY_ENDPOINT, client::deleteCategory),
                new AbstractMap.SimpleEntry<String, Consumer<String>>(EntandoCoreMockServer.GROUPS_ENDPOINT, client::deleteGroup),
                new AbstractMap.SimpleEntry<String, Consumer<String>>(EntandoCoreMockServer.WIDGET_ENDPOINT, client::deleteWidget),
                new AbstractMap.SimpleEntry<String, Consumer<String>>(EntandoCoreMockServer.FRAGMENT_ENDPOINT, client::deleteFragment),
                new AbstractMap.SimpleEntry<String, Consumer<String>>(EntandoCoreMockServer.LABEL_ENDPOINT, client::deleteLabel),
                new AbstractMap.SimpleEntry<String, Consumer<String>>(EntandoCoreMockServer.PAGE_ENDPOINT, client::deletePage),
                new AbstractMap.SimpleEntry<String, Consumer<String>>(EntandoCoreMockServer.PAGE_TEMPLATE_ENDPOINT, client::deletePageModel),
                new AbstractMap.SimpleEntry<String, Consumer<String>>(EntandoCoreMockServer.CONTENT_ENDPOINT, client::deleteContent),
                new AbstractMap.SimpleEntry<String, Consumer<String>>(EntandoCoreMockServer.CONTENT_TEMPLATE_ENDPOINT, client::deleteContentModel),
                new AbstractMap.SimpleEntry<String, Consumer<String>>(EntandoCoreMockServer.CONTENT_TYPE_ENDPOINT, client::deleteContentType)
        );

    }

    /**
     * stub a 404 response when invoking the received url.
     * @param url the url to bind to the 404 response
     */
    private void stubForDeleteNotFoundComponent(String url) {
        stubForDeleteComponent(url, 404);
    }

    /**
     * stub a 403 response when invoking the received url.
     * @param url the url to bind to the 403 response
     */
    private void stubForDeleteForbiddenComponent(String url) {
        stubForDeleteComponent(url, 403);
    }

    private void stubForDeleteComponent(String url, int statusCode) {

        coreMockServer.getInnerServer()
                .stubFor(WireMock.delete(urlEqualTo(url + "/" + CODE))
                        .willReturn(
                                aResponse()
                                        .withStatus(statusCode)
                                        .withHeader("Content-Type", "application/json")
                        ));
    }
}
