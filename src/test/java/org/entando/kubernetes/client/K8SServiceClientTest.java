package org.entando.kubernetes.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.hateoas.MediaTypes.HAL_JSON;
import static org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
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
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.link.EntandoAppPluginLink;
import org.entando.kubernetes.model.link.EntandoAppPluginLinkBuilder;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPluginBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.client.Traverson;
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
    private String k8sServiceUrlBaseUrl;
    private static int port = 9080;

    static {
//        port = findFreePort().orElse(9080);
//        port = findFreePort().orElse(9080);
    }

    WireMockServer wireMockServer;

    @BeforeEach
    public void setup() {
        k8sServiceUrlBaseUrl = String.format("http://localhost:%d/", port);
        client = new DefaultK8SServiceClient(k8sServiceUrlBaseUrl, CLIENT_ID, CLIENT_SECRET, TOKEN_URI);
        client.setRestTemplate(noOAuthRestTemplate());
        wireMockServer = new WireMockServer(options().port(port));
        wireMockServer.start();
    }

    @AfterEach
    public void teardown() {
        wireMockServer.resetAll();
        wireMockServer.stop();
    }

    @Test
    public void testTraversonWithWiremock() {
        String rootResponse = this.readResourceAsString("/payloads/k8s-svc/root.json");
        wireMockServer.stubFor(
                get(urlMatching("/?"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(rootResponse)));

        Traverson t = client.newTraverson();
        Link l = t.follow("app-plugin-links").asLink();
        assertThat(l).isNotNull();
        assertThat(l.getRel().value()).isEqualTo("app-plugin-links");
        assertThat(l.getHref()).isEqualTo("http://localhost:9080/app-plugin-links");

    }

    @Test
    public void shouldReturnLinksToApp() {
        String rootResponse = this.readResourceAsString("/payloads/k8s-svc/root.json");
        String linksResponse = this.readResourceAsString("/payloads/k8s-svc/app-plugin-links/app-plugin-links.json");
        String singlePluginResponse = this.readResourceAsString("/payloads/k8s-svc/plugins/plugin.json");

        wireMockServer.stubFor(get(urlMatching("/?"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(rootResponse)));
        wireMockServer.stubFor(get(urlMatching("/app-plugin-links/?"))
                .withRequestBody(new AnythingPattern())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(linksResponse)));
        wireMockServer.stubFor(get(urlEqualTo("/app-plugin-links?app=my-app"))
                .withRequestBody(new AnythingPattern())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(linksResponse)));
        wireMockServer.stubFor(get(urlEqualTo("/plugins/plugin"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(singlePluginResponse)));

        List<EntandoAppPluginLink> returnedLink = client.getAppLinks("my-app");
        wireMockServer.verify(1, getRequestedFor(urlMatching("/app-plugin-links/?")));
        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/app-plugin-links?app=my-app")));
        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/plugin/plugin")));
        assertThat(returnedLink.size()).isEqualTo(1);
        assertThat(returnedLink.get(0).getSpec().getEntandoAppName()).isEqualTo("my-app");
        assertThat(returnedLink.get(0).getSpec().getEntandoAppNamespace()).isEqualTo("my-namespace");
    }

    @Test
    public void shouldLinkAnAppWithAPlugin() {

        String rootResponse = this.readResourceAsString("/payloads/k8s-svc/root.json");
        String appsListResponse = this.readResourceAsString("/payloads/k8s-svc/apps/apps.json");
        String singleAppResponse = this.readResourceAsString("/payloads/k8s-svc/apps/app.json");
        String createdLinkResponse = this.readResourceAsString("/payloads/k8s-svc/app-plugin-links/app-plugin-link.json");
        EntandoPlugin testPlugin = getTestEntandoPlugin();

        wireMockServer.stubFor(get(urlMatching("/?"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(rootResponse)));
        wireMockServer.stubFor(get(urlMatching("/apps/?"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(appsListResponse)));
        wireMockServer.stubFor(get(urlMatching("/apps/my-app/?"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(singleAppResponse)));
        wireMockServer.stubFor(post(urlEqualTo("/apps/my-app/links"))
                .withRequestBody(new AnythingPattern())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(createdLinkResponse)));

        client.linkAppWithPlugin("my-app", "my-namespace", testPlugin);
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/apps/my-app/links")));
        List<LoggedRequest> loggedRequests = wireMockServer.findAll(postRequestedFor(urlEqualTo("/apps/my-app/links")));
        loggedRequests.get(0).getBodyAsString().contains("name: " + testPlugin.getMetadata().getName());
        loggedRequests.get(0).getBodyAsString().contains("namespace: " + testPlugin.getMetadata().getNamespace());

    }

    @Test
    public void shouldUnlinkThePlugin() {
        String rootResponse = this.readResourceAsString("/payloads/k8s-svc/root.json");
        String linksResponse = this.readResourceAsString("/payloads/k8s-svc/app-plugin-links/app-plugin-links.json");
        String singleLinkResponse = this.readResourceAsString("/payloads/k8s-svc/app-plugin-links/app-plugin-link.json");
        EntandoAppPluginLink testLink = getTestEntandoAppPluginLink();

        wireMockServer.stubFor(get(urlMatching("/?"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(rootResponse)));
        wireMockServer.stubFor(get(urlMatching("/app-plugin-links/?"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(linksResponse)));
        wireMockServer.stubFor(delete(urlMatching("/app-plugin-links/[a-z\\-]+"))
                .withRequestBody(new AnythingPattern())
                .willReturn(aResponse().withStatus(204)));

        client.unlink(getTestEntandoAppPluginLink());
        String name = testLink.getMetadata().getName();
        wireMockServer.verify(1, deleteRequestedFor(urlEqualTo("/app-plugin-links/"+ name)));

    }

    @Test
    public void shouldThrowExceptionIfResponseHasNoBody() {
        ResponseEntity expectedResponse =
                ResponseEntity.ok(null);

        RestTemplate mockRt = mock(RestTemplate.class);
        when(mockRt.exchange(any(String.class), eq(HttpMethod.GET), eq(null), any(ParameterizedTypeReference.class)))
                .thenReturn(expectedResponse);

        Assertions.assertThrows(RuntimeException.class, () -> {
            client.getAppLinks("my-app");
        });
    }

    @Test
    public void shouldThrowExceptionWhenResponseStatusIsError() {
        ResponseEntity expectedResponse = ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        RestTemplate mockRt = mock(RestTemplate.class);
        when(mockRt.exchange(any(String.class), eq(HttpMethod.GET), eq(null), any(ParameterizedTypeReference.class)))
                .thenReturn(expectedResponse);

        Assertions.assertThrows(RuntimeException.class, () -> {
            client.getAppLinks("my-app");
        });
    }

    @Test
    public void shouldParseEntandoAppPluginCorrectly() {

        String rootResponse = this.readResourceAsString("/payloads/k8s-svc/root.json");
        String linksResponse = this.readResourceAsString("/payloads/k8s-svc/app-plugin-links/app-plugin-links.json");
        String singlePluginResponse = this.readResourceAsString("/payloads/k8s-svc/plugins/plugin.json");

        wireMockServer.stubFor(get(urlMatching("/?"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(rootResponse)));
        wireMockServer.stubFor(get(urlMatching("/app-plugin-links/?"))
                .withRequestBody(new AnythingPattern())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(linksResponse)));
        wireMockServer.stubFor(get(urlEqualTo("/app-plugin-links?app=my-app"))
                .withRequestBody(new AnythingPattern())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(linksResponse)));
        wireMockServer.stubFor(get(urlEqualTo("/plugins/plugin"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(singlePluginResponse)));

        List<EntandoAppPluginLink> links = client.getAppLinks("my-app");
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
                .withNewMetadata()
                .withName("my-link")
                .endMetadata()
                .withNewSpec()
                .withEntandoPlugin("plugin-namespace", "plugin")
                .withEntandoApp("dummy", "dummy")
                .endSpec()
                .build();

        String rootResponse = this.readResourceAsString("/payloads/k8s-svc/root.json");
        String linkListResponse = this.readResourceAsString("/payloads/k8s-svc/app-plugin-links/app-plugin-links.json");
        String linkResponse = readResourceAsString("/payloads/k8s-svc/app-plugin-links/app-plugin-link.json");
        String pluginResponse = readResourceAsString("/payloads/k8s-svc/plugins/plugin.json");

        wireMockServer.stubFor(get(urlMatching("/?"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(rootResponse)));
        wireMockServer.stubFor(get(urlMatching("/app-plugin-links/?"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(linkListResponse)));
        wireMockServer.stubFor(get(urlEqualTo("/app-plugin-links/my-link"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(linkResponse)));
        wireMockServer.stubFor(get(urlEqualTo("/plugins/plugin"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(pluginResponse)));

        EntandoPlugin plugin = client.getPluginForLink(eapl);
        assertThat(plugin.getMetadata().getName()).isEqualTo("plugin");
        assertThat(plugin.getMetadata().getNamespace()).isEqualTo("plugin-namespace");
        assertThat(plugin.getSpec().getImage()).isEqualTo("entando/some-image:6.0.0");

    }


    @Test
    public void shouldGetBundlesFromAllObservedNamespaces() {
        String stubResponse = readResourceAsString("/payloads/k8s-svc/de-bundles/list.json");
        wireMockServer.stubFor(get(urlMatching("/de-bundles/?"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(stubResponse)));
        List<EntandoDeBundle> bundles = client.getBundlesInObservedNamespaces();
        assertThat(bundles).hasSize(1);
        assertThat(bundles.get(0).getMetadata().getName()).isEqualTo("my-bundle");
        assertThat(bundles.get(0).getSpec().getDetails().getName()).isEqualTo("my-bundle");
    }

    @Test
    public void shouldGetBundlesFromSingleNamespace() {
        String stubResponse = readResourceAsString("/payloads/k8s-svc/de-bundles/list.json");
        wireMockServer.stubFor(get(urlMatching("/de-bundles/namespaces/entando-de-bundles"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(stubResponse)));
        List<EntandoDeBundle> bundles = client.getBundlesInNamespace("entando-de-bundles");
        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/de-bundles/namespaces/entando-de-bundles")));
        assertThat(bundles).hasSize(1);
    }

    @Test
    public void shouldGetBundlesFromMultipleNamespaces() {
        String stubResponse = readResourceAsString("/payloads/k8s-svc/de-bundles/empty-list.json");
        wireMockServer.stubFor(get(urlMatching("/de-bundles/namespaces/first"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(stubResponse)));
        wireMockServer.stubFor(get(urlMatching("/de-bundles/namespaces/second"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(stubResponse)));
        wireMockServer.stubFor(get(urlMatching("/de-bundles/namespaces/third"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(stubResponse)));
        List<EntandoDeBundle> bundles = client.getBundlesInNamespaces(Arrays.asList("first", "second", "third"));
        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/de-bundles/namespaces/first")));
        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/de-bundles/namespaces/second")));
        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/de-bundles/namespaces/third")));
        assertThat(bundles).isEmpty();
    }

    @Test
    public void shouldGetBundleWithName() {
        String stubResponse = readResourceAsString("/payloads/k8s-svc/de-bundles/list.json");
        wireMockServer.stubFor(get(urlMatching("/de-bundles/?"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(stubResponse)));
        Optional<EntandoDeBundle> bundle = client.getBundleWithName("my-bundle");
        assertThat(bundle.isPresent()).isTrue();
        assertThat(bundle.get().getSpec().getDetails().getName()).isEqualTo("my-bundle");
    }

    @Test
    public void shouldNotFindBundleWithName() {
        String stubResponse = readResourceAsString("/payloads/k8s-svc/de-bundles/empty-list.json");
        wireMockServer.stubFor(get(urlMatching("/de-bundles/?"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(stubResponse)));
        Optional<EntandoDeBundle> bundle = client.getBundleWithName("my-bundle");
        assertThat(bundle.isPresent()).isFalse();
    }

    @Test
    public void shouldNotFindBundleWithNameInNamespace() {
        wireMockServer.stubFor(get(urlMatching("/de-bundles/namespaces/my-namespace/my-bundle"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("{}")
                        .withHeader("Content-Type", "application/json")));
        Optional<EntandoDeBundle> bundle = client.getBundleWithNameAndNamespace("my-bundle", "my-namespace");
        assertThat(bundle.isPresent()).isFalse();
    }

    @Test
    public void shouldGetBundleWithNameAndNamespace() {
        String stubResponse = readResourceAsString("/payloads/k8s-svc/de-bundles/my-bundle.json");
        wireMockServer.stubFor(get(urlMatching("/de-bundles/namespaces/entando-de-bundle/my-bundle"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(stubResponse)));
        Optional<EntandoDeBundle> bundle = client.getBundleWithNameAndNamespace("my-bundle", "entando-de-bundle");
        assertThat(bundle.isPresent()).isTrue();
        assertThat(bundle.get().getSpec().getDetails().getName()).isEqualTo("my-bundle");
    }

    private RestTemplate noOAuthRestTemplate() {
        RestTemplate template = new RestTemplate();
        List<HttpMessageConverter<?>> converters = template.getMessageConverters();
        template.setMessageConverters(Traverson.getDefaultMessageConverters(HAL_JSON));
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
                .withName("my-app-to-plugin-link")
                .withNamespace("my-namespace")
                .endMetadata()
                .withNewSpec()
                .withEntandoApp("my-namespace", "my-app")
                .withEntandoPlugin("plugin-namespace", "plugin")
                .endSpec()
                .build();
    }

    private EntandoPlugin getTestEntandoPlugin() {
       return new EntandoPluginBuilder()
               .withNewMetadata()
               .withName("plugin")
               .withNamespace("plugin-namespace")
               .endMetadata()
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
