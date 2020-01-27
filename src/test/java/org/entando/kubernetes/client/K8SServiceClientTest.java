package org.entando.kubernetes.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.entando.kubernetes.client.k8ssvc.DefaultK8SServiceClient;
import org.entando.kubernetes.model.link.EntandoAppPluginLink;
import org.entando.kubernetes.model.link.EntandoAppPluginLinkBuilder;
import org.entando.kubernetes.model.web.response.RestResponse;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.hateoas.server.LinkRelationProvider;
import org.springframework.hateoas.server.core.DefaultLinkRelationProvider;
import org.springframework.hateoas.server.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;


@RunWith(SpringRunner.class)
public class K8SServiceClientTest {

    private final String CLIENT_ID = "test-entando-de";
    private final String CLIENT_SECRET = "0fdb9047-e121-4aa4-837d-8d51c1822b8a";
//    private final String TOKEN_URI = "http://test-keycloak.192.168.1.9.nip.io/auth/realms/entando/protocol/openid-connect/token";
    private final String TOKEN_URI = "http://someurl.com";
    private DefaultK8SServiceClient client;
    private static int port;

    static {
       port = findFreePort().orElse(8080);
    }

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(port);

    @Before
    public void setup() {
       client = new DefaultK8SServiceClient(String.format("http://localhost:%d",port), CLIENT_ID, CLIENT_SECRET, TOKEN_URI);
    }

    @Test
    public void shouldReturnLinkCorrectly() {
        EntandoAppPluginLink appPluginLink = getTestEntandoAppPluginLink();
        ResponseEntity<CollectionModel<EntityModel<EntandoAppPluginLink>>> expectedResponse =
                ResponseEntity.ok(new CollectionModel<>(Collections.singletonList(new EntityModel<>(appPluginLink))));

        RestTemplate mockRt = mock(RestTemplate.class);
        when(mockRt.exchange(any(String.class), eq(HttpMethod.GET), eq(null), any(ParameterizedTypeReference.class)))
                .thenReturn(expectedResponse);

        client.setRestTemplate(mockRt);

        List<EntandoAppPluginLink> returnedLink = client.getAppLinkedPlugins("my-app", "my-namespace");
        assertThat(returnedLink.size()).isEqualTo(1);
        assertThat(returnedLink.get(0).getSpec().getEntandoAppName()).isEqualTo("my-app");
        assertThat(returnedLink.get(0).getSpec().getEntandoAppNamespace()).isEqualTo("my-namespace");
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionIfResponseHasNoBody() {
        ResponseEntity expectedResponse =
                ResponseEntity.ok(null);

        RestTemplate mockRt = mock(RestTemplate.class);
        when(mockRt.exchange(any(String.class), eq(HttpMethod.GET), eq(null), any(ParameterizedTypeReference.class)))
                .thenReturn(expectedResponse);

        client.getAppLinkedPlugins("my-app", "my-namespace");
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionWhenResponseStatusIsError() {
        ResponseEntity expectedResponse = ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        RestTemplate mockRt = mock(RestTemplate.class);
        when(mockRt.exchange(any(String.class), eq(HttpMethod.GET), eq(null), any(ParameterizedTypeReference.class)))
                .thenReturn(expectedResponse);

        client.getAppLinkedPlugins("my-app", "my-namespace");

    }

    @Test
    public void shouldParseEntandoAppPluginCorrectly() throws JsonProcessingException {

        CollectionModel<EntityModel<EntandoAppPluginLink>> halResources = new CollectionModel<>(Collections.singletonList(new EntityModel<>(getTestEntandoAppPluginLink())));
        client.setRestTemplate(noOAuthRestTemplate());

        stubFor(get(urlPathMatching("/apps/my-namespace/my-app/links"))
                .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(getHalReadyObjectMapper().writeValueAsString(halResources))));

        List<EntandoAppPluginLink> links = client.getAppLinkedPlugins("my-app", "my-namespace");
        assertThat(links).hasSize(1);
        EntandoAppPluginLink appPluginLink = links.get(0);
        assertThat(appPluginLink.getMetadata().getNamespace()).isEqualTo("my-namespace");
        assertThat(appPluginLink.getMetadata().getName()).isEqualTo("my-app-to-plugin-link");
        assertThat(appPluginLink.getSpec().getEntandoAppNamespace()).isEqualTo("my-namespace");
        assertThat(appPluginLink.getSpec().getEntandoAppName()).isEqualTo("my-app");
        assertThat(appPluginLink.getSpec().getEntandoPluginName()).isEqualTo("plugin");
        assertThat(appPluginLink.getSpec().getEntandoPluginNamespace()).isEqualTo("plugin-namespace");

    }

    private RestTemplate noOAuthRestTemplate() {
        RestTemplate template = new RestTemplate();
        List<HttpMessageConverter<?>> converters = template.getMessageConverters();
        ObjectMapper mapper = getHalReadyObjectMapper();
        MappingJackson2HttpMessageConverter halConverter = new TypeConstrainedMappingJackson2HttpMessageConverter(
                RestResponse.class);
        halConverter.setObjectMapper(mapper);
        halConverter.setSupportedMediaTypes(Arrays.asList(MediaType.APPLICATION_JSON, MediaTypes.HAL_JSON));
        converters.add(0, halConverter);

        return template;
    }

    private ObjectMapper getHalReadyObjectMapper() {
        LinkRelationProvider provider = new DefaultLinkRelationProvider();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jackson2HalModule());
        mapper.setHandlerInstantiator(new Jackson2HalModule.HalHandlerInstantiator(provider, null, null));
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }


    private EntandoAppPluginLink getTestEntandoAppPluginLink() {
        return new EntandoAppPluginLinkBuilder()
                    .withNewMetadata()
                        .withName("my-app-to-plugin-link")
                        .withNamespace("my-namespace")
                    .endMetadata()
                .withNewSpec()
                    .withEntandoApp("my-namespace", "my-app")
                    .withEntandoPlugin("plugin-namespace", "plugin")
                .endSpec()
                .build();
    }

    private static Optional<Integer> findFreePort() {
        Integer port = null;
        try {
            // Get a free port
            ServerSocket s = new ServerSocket(0);
            port = s.getLocalPort();
            s.close();


        } catch (IOException e) {
            // No OPS
        }
        return Optional.ofNullable(port);
    }

}
