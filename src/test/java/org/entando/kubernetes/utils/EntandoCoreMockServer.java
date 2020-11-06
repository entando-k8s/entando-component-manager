package org.entando.kubernetes.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.Getter;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.UriComponentsBuilder;

public class EntandoCoreMockServer extends EntandoGenericMockServer {

    public static final String WIDGET_ENDPOINT = "/api/widgets";
    public static final String FRAGMENT_ENDPOINT = "/api/fragments";
    public static final String LABEL_ENDPOINT = "/api/labels";
    public static final String PAGE_ENDPOINT = "/api/pages";
    public static final String PAGE_TEMPLATE_ENDPOINT = "/api/pageModels";
    public static final String CONTENT_TEMPLATE_ENDPOINT = "/api/plugins/cms/contentmodels";
    public static final String CONTENT_TYPE_ENDPOINT = "/api/plugins/cms/contentTypes";
    public static final String ENGINE_ANALYSIS_REPORT_ENDPOINT = "/api/analysis/components/diff";
    public static final String CMS_ANALYSIS_REPORT_ENDPOINT = "/api/analysis/cms/components/diff";
    public static final String CODE_PATH_PARAM = "/{code}";

    public EntandoCoreMockServer() {
        super();
    }

    @Override
    protected void init(WireMockServer wireMockServer) {
        addKeycloakEndpoints();
    }

    private void addKeycloakEndpoints() {
        this.wireMockServer.stubFor(WireMock.post(urlEqualTo("/auth/protocol/openid-connect/auth"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"access_token\": \"iddqd\" }")));
    }

    public EntandoCoreMockServer withGenericComponentsUsageSupport() {
        for (ComponentUsageApiEndpoint ep : ComponentUsageApiEndpoint.values()) {
            String codeHandlebarTemplate = "{{request.path.[" + ep.getCodePathSegmentPosition() + "]}}";
            this.wireMockServer.stubFor(WireMock.get(urlPathMatching(ep.expandUrl()))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{ \"payload\" : {\n "
                                    + "\"type\": \"" + ep.getTypeValue() + "\",\n"
                                    + "\"code\": \"" + codeHandlebarTemplate + "\",\n"
                                    + "\"usage\": 1\n"
                                    + "},\n"
                                    + "\"metadata\": {},\n"
                                    + "\"errors\": []\n "
                                    + "}")
                            .withTransformers("response-template")
                    ));
        }
        return this;
    }

    public EntandoCoreMockServer withComponentUsageSupport(ComponentType type, String code, int usageCount) {

        ComponentUsageApiEndpoint ep = ComponentUsageApiEndpoint.getForComponentType(type);
        SimpleRestResponse<EntandoCoreComponentUsage> usageResponse = new SimpleRestResponse<>(
                new EntandoCoreComponentUsage(ep.getTypeValue(), code, usageCount));
        String response = null;
        try {
            response = new ObjectMapper().writeValueAsString(usageResponse);
            this.wireMockServer.stubFor(WireMock.get(urlPathMatching(ep.expandUrlWithCode(code)))
                    .willReturn(aResponse().withStatus(200).withBody(response)
                            .withHeader("Content-Type", "application/json")));
            return this;
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public EntandoCoreMockServer withFailingComponentUsageSupport(ComponentType type, String code,
            HttpStatus httpStatus) {

        ComponentUsageApiEndpoint ep = ComponentUsageApiEndpoint.getForComponentType(type);

        this.wireMockServer.stubFor(WireMock.get(urlPathMatching(ep.expandUrlWithCode(code)))
                .willReturn(aResponse().withStatus(httpStatus.value())
                        .withHeader("Content-Type", "application/json")));
        return this;

    }

    public EntandoCoreMockServer withWidgetRegistrationSupport() {
        return this.withGenericSupport(WIDGET_ENDPOINT, WireMock::post);
    }

    public EntandoCoreMockServer withWidgetDeletionSupport() {
        return this.withGenericSupport(WIDGET_ENDPOINT, WireMock::delete);
    }

    /**
     * generic method to stub a response to a particular REST request.
     *
     * @param urlPath            the url to match for the request
     * @param wireMockHttpMethod the Http method to use in the mocked request
     * @return this instance of the EntandoCoreMockServer
     */
    public EntandoCoreMockServer withGenericSupport(String urlPath,
            Function<UrlPattern, MappingBuilder> wireMockHttpMethod) {

        return this.withGenericSupport(urlPath, null, wireMockHttpMethod);
    }

    /**
     * generic method to stub a response to a particular REST request having a path param related to the code of the
     * component interested.
     *
     * @param urlPath            the url to match for the request
     * @param code               the path param to use
     * @param wireMockHttpMethod the Http method to use in the mocked request
     * @return this instance of the EntandoCoreMockServer
     */
    public EntandoCoreMockServer withGenericSupport(String urlPath, String code,
            Function<UrlPattern, MappingBuilder> wireMockHttpMethod) {

        String url = Optional.ofNullable(code)
                .map(c -> UriComponentsBuilder.newInstance().path(urlPath + CODE_PATH_PARAM).buildAndExpand(code)
                        .toUriString())
                .orElseGet(() -> UriComponentsBuilder.newInstance().path(urlPath).buildAndExpand().toUriString());

        this.wireMockServer.stubFor(wireMockHttpMethod.apply(urlEqualTo(url))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")));

        return this;
    }


    /**
     * stub a successful response for the Engine AnalysisReport endpoint.
     *
     * @param customStubResourcePath if present the content of the file identified by this param will be used as
     *                               response otherwise the default value will be used
     * @return this instance of the EntandoCoreMockServer
     */
    public EntandoCoreMockServer withEngineAnalysisReportSupport(String customStubResourcePath) {

        return this.withGenericAnalysisReportSupport(customStubResourcePath, ENGINE_ANALYSIS_REPORT_ENDPOINT);
    }

    /**
     * stub a successful response for the CMS AnalysisReport endpoint.
     *
     * @param customStubResourcePath if present the content of the file identified by this param will be used as
     *                               response, otherwise the default value will be used
     * @return this instance of the EntandoCoreMockServer
     */
    public EntandoCoreMockServer withCMSAnalysisReportSupport(String customStubResourcePath) {

        return this.withGenericAnalysisReportSupport(customStubResourcePath, CMS_ANALYSIS_REPORT_ENDPOINT);
    }

    /**
     * stub a successful response for the AnalysisReport endpoint identified by the received URL.
     *
     * @param customStubResourcePath if present the content of the file identified by this param will be used as
     *                               response, *                        otherwise the default value will be used
     * @param url                    the URL to use as AnalysisReport endpoint
     * @return this instance of the EntandoCoreMockServer
     */
    private EntandoCoreMockServer withGenericAnalysisReportSupport(String customStubResourcePath, String url) {

        String defaultStubResourcePath = "/payloads/entando-core/analysis-report/engine-analysis-report.json";
        String currentStubResourcePath = Optional.ofNullable(customStubResourcePath).orElse(defaultStubResourcePath);

        String stubResponse = readResourceAsString(currentStubResourcePath);

        this.wireMockServer.stubFor(WireMock.post(urlEqualTo(url))
                .willReturn(
                        aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(stubResponse)
                ));

        return this;
    }


    @Getter
    private enum ComponentUsageApiEndpoint {
        PAGE(ComponentType.PAGE, "pages", 3, "/api/pages/{code}/usage"),
        PAGE_MODEL(ComponentType.PAGE_TEMPLATE, "pageModels", 3, "/api/pageModels/{code}/usage"),
        WIDGET(ComponentType.WIDGET, "widgets", 3, "/api/widgets/{code}/usage"),
        FRAGMENTS(ComponentType.FRAGMENT, "fragments", 3, "/api/fragments/{code}/usage"),
        CONTENT_TYPE(ComponentType.CONTENT_TYPE, "contentTypes", 4, "/api/plugins/cms/contentTypes/{code}/usage"),
        CONTENT_TEMPLATE(ComponentType.CONTENT_TEMPLATE, "contentTemplates", 5,
                "/api/plugins/cms/contentmodels/{code}/usage");

        private final ComponentType componentType;
        private final String typeValue;
        private final String componentCode;
        private final int codePathSegmentPosition;
        private final String urlTemplate;

        ComponentUsageApiEndpoint(ComponentType type, String typeValue, int codePathSegmentPosition,
                String urlTemplate) {
            this.componentType = type;
            this.typeValue = typeValue;
            this.componentCode = "(\\S{3,})";
            this.codePathSegmentPosition = codePathSegmentPosition;
            this.urlTemplate = urlTemplate;
        }

        public static ComponentUsageApiEndpoint getForComponentType(ComponentType type) {
            return Arrays.stream(values())
                    .filter(ep -> ep.componentType.equals(type))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No usage endpoint for type " + type.toString()));
        }

        public String expandUrl() {
            return expandUrlWithCode(this.componentCode);
        }

        public String expandUrlWithCode(String code) {
            Map<String, Object> variables = new HashMap<>();
            variables.put("code", code);
            return UriComponentsBuilder.newInstance().path(urlTemplate).buildAndExpand(variables).toUriString();
        }
    }
}
