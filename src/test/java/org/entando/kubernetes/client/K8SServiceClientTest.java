package org.entando.kubernetes.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.entando.kubernetes.client.k8ssvc.DefaultK8SServiceClient;
import org.entando.kubernetes.model.link.EntandoAppPluginLink;
import org.entando.kubernetes.model.link.EntandoAppPluginLinkBuilder;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.mediatype.hal.DefaultCurieProvider;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.hateoas.server.LinkRelationProvider;
import org.springframework.hateoas.server.core.DefaultLinkRelationProvider;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Tag("unit")
public class K8SServiceClientTest {

    private final String CLIENT_ID = "test-entando-de";
    private final String CLIENT_SECRET = "0fdb9047-e121-4aa4-837d-8d51c1822b8a";
    private final String TOKEN_URI = "http://someurl.com";
    private DefaultK8SServiceClient client;
    private static int port;

    static {
        port = findFreePort().orElse(9080);
    }

    WireMockServer wireMockServer;

    @BeforeEach
    public void setup() {
        client = new DefaultK8SServiceClient(String.format("http://localhost:%d", port), CLIENT_ID, CLIENT_SECRET, TOKEN_URI);
        wireMockServer = new WireMockServer(options().port(port));
        wireMockServer.start();
    }

    @AfterEach
    public void teardown() {
        wireMockServer.resetAll();
        wireMockServer.stop();
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

    @Test
    public void shouldThrowExceptionIfResponseHasNoBody() {
        ResponseEntity expectedResponse =
                ResponseEntity.ok(null);

        RestTemplate mockRt = mock(RestTemplate.class);
        when(mockRt.exchange(any(String.class), eq(HttpMethod.GET), eq(null), any(ParameterizedTypeReference.class)))
                .thenReturn(expectedResponse);

        Assertions.assertThrows(RuntimeException.class, () -> {
            client.getAppLinkedPlugins("my-app", "my-namespace");
        });
    }

    @Test
    public void shouldThrowExceptionWhenResponseStatusIsError() {
        ResponseEntity expectedResponse = ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        RestTemplate mockRt = mock(RestTemplate.class);
        when(mockRt.exchange(any(String.class), eq(HttpMethod.GET), eq(null), any(ParameterizedTypeReference.class)))
                .thenReturn(expectedResponse);

        Assertions.assertThrows(RuntimeException.class, () -> {
            client.getAppLinkedPlugins("my-app", "my-namespace");
        });
    }

    @Test
    public void shouldParseEntandoAppPluginCorrectly() {

        client.setRestTemplate(noOAuthRestTemplate());

        String wiremockResponse = this.readResourceAsString("/payloads/k8s-svc/app-links-to-plugin.json");

        wireMockServer.stubFor(get(urlEqualTo("/apps/my-namespace/my-app/links"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/hal+json")
                        .withBody(wiremockResponse)));

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

    @Test
    public void shouldReadPluginFromLink() {
        EntandoAppPluginLink eapl = new EntandoAppPluginLinkBuilder()
                .withNewSpec()
                .withEntandoPlugin("plugin-namespace", "plugin")
                .withEntandoApp("dummy", "dummy")
                .endSpec()
                .build();

        String stubResponse = readResourceAsString("/payloads/k8s-svc/plugin-linked-to-an-app.json");

        client.setRestTemplate(noOAuthRestTemplate());
        wireMockServer.stubFor(get(urlEqualTo("/plugins/plugin-namespace/plugin"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/hal+json")
                        .withBody(stubResponse)));

        EntandoPlugin plugin = client.getPluginForLink(eapl);
        assertThat(plugin.getMetadata().getName()).isEqualTo("plugin");
        assertThat(plugin.getMetadata().getNamespace()).isEqualTo("plugin-namespace");
        assertThat(plugin.getSpec().getImage()).isEqualTo("entando/some-image:6.0.0");

    }

    private RestTemplate noOAuthRestTemplate() {
        RestTemplate template = new RestTemplate();
        List<HttpMessageConverter<?>> converters = template.getMessageConverters();
        converters.add(0, getHalConverter());
        template.setMessageConverters(converters);
        return template;
    }

    private HttpMessageConverter<?> getHalConverter() {
        List<MediaType> supportedMediatypes = Arrays.asList(MediaType.APPLICATION_JSON, MediaTypes.HAL_JSON);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jackson2HalModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();

        converter.setObjectMapper(mapper);
        converter.setSupportedMediaTypes(supportedMediatypes);

        return converter;
    }

    private ObjectMapper getHalReadyObjectMapper(WireMockServer wireMockServer) {
        LinkRelationProvider provider = new DefaultLinkRelationProvider();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jackson2HalModule());
        mapper.setHandlerInstantiator(new Jackson2HalModule.HalHandlerInstantiator(provider,
                new DefaultCurieProvider("default", UriTemplate.of(wireMockServer.url("/apps/my-namespace/{app}"))),
                MessageSourceResolvable::getDefaultMessage));
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    private EntandoAppPluginLink getTestEntandoAppPluginLink() {
        return new EntandoAppPluginLinkBuilder()
                .withNewMetadata()
                .withName("my-app-to-pluin-link")
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

    private String readResourceAsString(String resourcePath) {

        try
        {
            Path rp = Paths.get(this.getClass().getResource(resourcePath).toURI());
            return new String ( Files.readAllBytes(rp) );
        }
        catch (IOException | URISyntaxException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
