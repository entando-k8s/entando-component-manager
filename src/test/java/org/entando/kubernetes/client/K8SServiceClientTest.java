package org.entando.kubernetes.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.hateoas.MediaTypes.HAL_JSON;
import static org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.entando.kubernetes.client.k8ssvc.DefaultK8SServiceClient;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.link.EntandoAppPluginLink;
import org.entando.kubernetes.model.link.EntandoAppPluginLinkBuilder;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPluginBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Tag("unit")
public class K8SServiceClientTest {

    private static EntandoK8SServiceMockServer mockServer;
    private final String CLIENT_ID = "test-entando-de";
    private final String CLIENT_SECRET = "0fdb9047-e121-4aa4-837d-8d51c1822b8a";
    private final String TOKEN_URI = "http://someurl.com";
    private DefaultK8SServiceClient client;

    @BeforeEach
    public void setup() {
        mockServer = new EntandoK8SServiceMockServer();
        client = new DefaultK8SServiceClient(mockServer.getApiRoot(), CLIENT_ID, CLIENT_SECRET, TOKEN_URI);
        client.setRestTemplate(noOAuthRestTemplate());
    }

    @AfterEach
    public void reset() {
        mockServer.tearDown();
    }

    @Test
    public void testTraversonWithWiremock() {
        Traverson t = client.newTraverson();
        Link l = t.follow("app-plugin-links").asLink();
        assertThat(l).isNotNull();
        assertThat(l.getRel().value()).isEqualTo("app-plugin-links");
        assertThat(l.getHref()).isEqualTo(mockServer.getApiRoot() + "/app-plugin-links");

    }

    @Test
    public void shouldReturnLinksToApp() {
        List<EntandoAppPluginLink> returnedLink = client.getAppLinks("my-app");
        mockServer.getInnerServer().verify(1, getRequestedFor(urlMatching("/?")));
        mockServer.getInnerServer().verify(1, getRequestedFor(urlMatching("/app-plugin-links/?")));
        mockServer.getInnerServer().verify(1, getRequestedFor(urlEqualTo("/app-plugin-links?app=my-app")));
        assertThat(returnedLink.size()).isEqualTo(1);
        assertThat(returnedLink.get(0).getSpec().getEntandoAppName()).isEqualTo("my-app");
        assertThat(returnedLink.get(0).getSpec().getEntandoAppNamespace()).isEqualTo("my-namespace");
    }

    @Test
    public void shouldLinkAnAppWithAPlugin() {

        EntandoPlugin testPlugin = getTestEntandoPlugin();
        client.linkAppWithPlugin("my-app", "my-namespace", testPlugin);
        mockServer.getInnerServer().verify(1, postRequestedFor(urlEqualTo("/apps/my-app/links")));
        List<LoggedRequest> loggedRequests = mockServer.getInnerServer().findAll(postRequestedFor(urlEqualTo("/apps/my-app/links")));
        loggedRequests.get(0).getBodyAsString().contains("name: " + testPlugin.getMetadata().getName());
        loggedRequests.get(0).getBodyAsString().contains("namespace: " + testPlugin.getMetadata().getNamespace());

    }

    @Test
    public void shouldUnlinkThePlugin() {
        EntandoAppPluginLink testLink = getTestEntandoAppPluginLink();
        client.unlink(getTestEntandoAppPluginLink());
        String name = testLink.getMetadata().getName();
        mockServer.getInnerServer().verify(1, deleteRequestedFor(urlEqualTo("/app-plugin-links/"+ name)));

    }

    @Test
    public void shouldThrowExceptionWhenResponseStatusIsError() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            client.getAppLinks("not-existing-app");
        });
    }

    @Test
    public void shouldParseEntandoAppPluginCorrectly() {

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

        EntandoPlugin plugin = client.getPluginForLink(eapl);
        assertThat(plugin.getMetadata().getName()).isEqualTo("plugin");
        assertThat(plugin.getMetadata().getNamespace()).isEqualTo("plugin-namespace");
        assertThat(plugin.getSpec().getImage()).isEqualTo("entando/some-image:6.0.0");

    }


    @Test
    public void shouldGetBundlesFromAllObservedNamespaces() {
        List<EntandoDeBundle> bundles = client.getBundlesInObservedNamespaces();
        assertThat(bundles).hasSize(1);
        assertThat(bundles.get(0).getMetadata().getName()).isEqualTo("my-bundle");
        assertThat(bundles.get(0).getSpec().getDetails().getName()).isEqualTo("my-bundle");
    }

    @Test
    public void shouldGetBundlesFromSingleNamespace() {
        List<EntandoDeBundle> bundles = client.getBundlesInNamespace("entando-de-bundles");
        mockServer.getInnerServer().verify(1, getRequestedFor(urlEqualTo("/bundles?namespace=entando-de-bundles")));
        assertThat(bundles).hasSize(1);
    }

    @Test
    public void shouldGetBundlesFromMultipleNamespaces() {
        String stubResponse = mockServer.readResourceAsString("/payloads/k8s-svc/bundles/bundles-empty-list.json");
        mockServer.addStub(get(urlMatching("/bundles/namespaces/first"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(stubResponse)));
        mockServer.addStub(get(urlMatching("/bundles/namespaces/second"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(stubResponse)));
        mockServer.addStub(get(urlMatching("/bundles/namespaces/third"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(stubResponse)));
        List<EntandoDeBundle> bundles = client.getBundlesInNamespaces(Arrays.asList("first", "second", "third"));
        mockServer.getInnerServer().verify(1, getRequestedFor(urlEqualTo("/bundles?namespaces=first")));
        mockServer.getInnerServer().verify(1, getRequestedFor(urlEqualTo("/bundles?namespaces=second")));
        mockServer.getInnerServer().verify(1, getRequestedFor(urlEqualTo("/bundles?namespaces=third")));
        assertThat(bundles).isEmpty();
    }

    @Test
    public void shouldGetBundleWithName() {
        Optional<EntandoDeBundle> bundle = client.getBundleWithName("my-bundle");
        assertThat(bundle.isPresent()).isTrue();
        assertThat(bundle.get().getSpec().getDetails().getName()).isEqualTo("my-bundle");
    }

    @Test
    public void shouldNotFindBundleWithName() {
        String stubResponse = readResourceAsString("/payloads/k8s-svc/de-bundles/bundles-empty-list.json");
        mockServer.addStub(get(urlMatching("/de-bundles/?"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(stubResponse)));
        Optional<EntandoDeBundle> bundle = client.getBundleWithName("my-bundle");
        assertThat(bundle.isPresent()).isFalse();
    }

    @Test
    public void shouldNotFindBundleWithNameInNamespace() {
        mockServer.addStub(get(urlMatching("/de-bundles/namespaces/my-namespace/my-bundle"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("{}")
                        .withHeader("Content-Type", "application/json")));
        Optional<EntandoDeBundle> bundle = client.getBundleWithNameAndNamespace("my-bundle", "my-namespace");
        assertThat(bundle.isPresent()).isFalse();
    }

    @Test
    public void shouldGetBundleWithNameAndNamespace() {
        Optional<EntandoDeBundle> bundle = client.getBundleWithNameAndNamespace("my-bundle", "entando-de-bundle");
        assertThat(bundle.isPresent()).isTrue();
        assertThat(bundle.get().getSpec().getDetails().getName()).isEqualTo("my-bundle");
    }

    private RestTemplate noOAuthRestTemplate() {
        RestTemplate template = new RestTemplate();
        template.setMessageConverters(Traverson.getDefaultMessageConverters(HAL_JSON));
        return template;
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
