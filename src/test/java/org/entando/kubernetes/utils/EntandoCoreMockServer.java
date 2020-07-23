package org.entando.kubernetes.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.springframework.web.util.UriComponentsBuilder;

public class EntandoCoreMockServer extends EntandoGenericMockServer {

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
// + codeHandlebarTemplate + "\",\n"
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

    @Getter
    private enum ComponentUsageApiEndpoint {
        PAGE(ComponentType.PAGE, "pages", 3, "/api/pages/{code}/usage"),
        PAGE_MODEL(ComponentType.PAGE_TEMPLATE, "pageModels", 3, "/api/pageModels/{code}/usage"),
        WIDGET(ComponentType.WIDGET, "widgets", 3, "/api/widgets/{code}/usage"),
        FRAGMENTS(ComponentType.FRAGMENT, "fragments", 3, "/api/fragments/{code}/usage"),
        CONTENT_TYPE(ComponentType.CONTENT_TYPE, "contentTypes", 4, "/api/plugins/cms/contentTypes/{code}/usage"),
        CONTENT_TEMPLATE(ComponentType.CONTENT_TEMPLATE, "contentTemplates", 5, "/api/plugins/cms/contentmodels/{code}/usage");

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

        public String expandUrl() {
            return expandUrlWithCode(this.componentCode);
        }

        public String expandUrlWithCode(String code) {
            Map<String, Object> variables = new HashMap<>();
            variables.put("code", code);
            return UriComponentsBuilder.newInstance().path(urlTemplate).buildAndExpand(variables).toUriString();
        }

        public static ComponentUsageApiEndpoint getForComponentType(ComponentType type) {
            return Arrays.stream(values())
                    .filter(ep -> ep.componentType.equals(type))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No usage endpoint for type " + type.toString()));
        }
    }
}
