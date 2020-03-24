package org.entando.kubernetes.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.springframework.cloud.contract.wiremock.WireMockSpring.options;
import static org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EntandoK8SServiceMockServer {

    private WireMockServer wireMockServer;
    private static int port;


    static {
        port = findFreePort().orElse(9080);
    }

    public EntandoK8SServiceMockServer() {
        wireMockServer = new WireMockServer(options().port(port));
        populateServer(wireMockServer);
        if (!wireMockServer.isRunning()) {
            wireMockServer.start();
        }
    }

    private void populateServer(WireMockServer wireMockServer) {
        addApiRoot(wireMockServer);
        addAppPluginLinksResource(wireMockServer);
        addAppsResource(wireMockServer);
        addPluginsResource(wireMockServer);
        addBundlesResource(wireMockServer);
        addNamespacesResource(wireMockServer);
    }

    private void addBundlesResource(WireMockServer wireMockServer) {
        String bundleListResponse = readResourceAsString("/payloads/k8s-svc/bundles/bundles.json");
        String singleBundleResponse = readResourceAsString("/payloads/k8s-svc/bundles/bundle.json");

        wireMockServer.stubFor(get(urlMatching("/bundles/?"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(bundleListResponse)));
        wireMockServer.stubFor(get(urlEqualTo("/bundles?namespace=entando-de-bundles"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(bundleListResponse)));
        wireMockServer.stubFor(get(urlMatching("/bundles/my-bundle/?"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(singleBundleResponse)));
    }

    private void addNamespacesResource(WireMockServer wireMockServer) {
        String namespaceListResponse = readResourceAsString("/payloads/k8s-svc/observed-namespaces/namespaces.json");
        String myNamespaceResponse = readResourceAsString("/payloads/k8s-svc/observed-namespaces/namespace-my-namespace.json");
        String entandoDeBundlesNamespaceResponse = readResourceAsString("/payloads/k8s-svc/observed-namespaces/namespace-entando-de-bundles.json");
        String pluginNamespaceResponse = readResourceAsString("/payloads/k8s-svc/observed-namespaces/namespace-plugin-namespace.json");
        wireMockServer.stubFor(get(urlMatching("/namespace/?"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(namespaceListResponse)));
        wireMockServer.stubFor(get(urlMatching("/namespace/my-namespace/?"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(myNamespaceResponse)));
        wireMockServer.stubFor(get(urlMatching("/namespace/entando-de-bundles-namespace/?"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(entandoDeBundlesNamespaceResponse)));
        wireMockServer.stubFor(get(urlMatching("/namespace/plugin-namespace/?"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(pluginNamespaceResponse)));
    }

    private void addAppsResource(WireMockServer wireMockServer) {
        String appsListResponse = this.readResourceAsString("/payloads/k8s-svc/apps/apps.json");
        String singleAppResponse = this.readResourceAsString("/payloads/k8s-svc/apps/app.json");
        String createdLinkResponse = this.readResourceAsString("/payloads/k8s-svc/app-plugin-links/app-plugin-link.json");

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
                        .withStatus(201)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(createdLinkResponse)));
    }

    private void addPluginsResource(WireMockServer wireMockServer) {
        String pluginListResponse = this.readResourceAsString("/payloads/k8s-svc/plugins/plugins.json");
        String singlePluginResponse = this.readResourceAsString("/payloads/k8s-svc/plugins/plugin.json");

        wireMockServer.stubFor(get(urlMatching("/plugins/?"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(pluginListResponse)));
        wireMockServer.stubFor(get(urlMatching("/plugins/plugin/?"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(singlePluginResponse)));

    }

    private void addAppPluginLinksResource(WireMockServer wireMockServer) {
        String linkListResponse = this.readResourceAsString("/payloads/k8s-svc/app-plugin-links/app-plugin-links.json");
        String singleLinkResponse = this.readResourceAsString("/payloads/k8s-svc/app-plugin-links/app-plugin-link.json");

        wireMockServer.stubFor(get(urlMatching("/app-plugin-links/?"))
                .withRequestBody(new AnythingPattern())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(linkListResponse)));
        wireMockServer.stubFor(get(urlEqualTo("/app-plugin-links?app=my-app"))
                .withRequestBody(new AnythingPattern())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(linkListResponse)));
        wireMockServer.stubFor(get(urlEqualTo("/app-plugin-links\\?plugin=plugin"))
                .withRequestBody(new AnythingPattern())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(linkListResponse)));
        wireMockServer.stubFor(get(urlMatching("/app-plugin-links/my-app-to-plugin-link/?"))
                .withRequestBody(new AnythingPattern())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(singleLinkResponse)));
        wireMockServer.stubFor(get(urlMatching("/app-plugin-links/[a-zA-Z\\-]+/?"))
                .withRequestBody(new AnythingPattern())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(singleLinkResponse)));
        wireMockServer.stubFor(delete(urlMatching("/app-plugin-links/[a-zA-Z\\-]+/?"))
                .withRequestBody(new AnythingPattern())
                .willReturn(aResponse().withStatus(204)));
    }

    private void addApiRoot(WireMockServer server) {
        String rootResponse = this.readResourceAsString("/payloads/k8s-svc/root.json");
        server.stubFor(
                get(urlMatching("/?"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", HAL_JSON_VALUE)
                                .withBody(rootResponse)));

    }

    public void start() {
        wireMockServer.start();
    }

    public void stop() {
        wireMockServer.stop();
    }

    public void tearDown() {
        wireMockServer.resetAll();
        wireMockServer.stop();
    }

    public void resetRequests() {
        wireMockServer.resetRequests();
    }

    public void resetMappings() {
        wireMockServer.resetAll();
        populateServer(wireMockServer);
    }

    public WireMockServer getInnerServer() {
       return wireMockServer;
    }

    public void addStub(MappingBuilder stub) {
        wireMockServer.stubFor(stub);
    }

    public String getApiRoot() {
        return "http://localhost:" + port;
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

    public String readResourceAsString(String resourcePath) {

        try
        {
            Path rp = Paths.get(this.getClass().getResource(resourcePath).toURI());
            String content = new String ( Files.readAllBytes(rp) );
            content = content.replaceAll("localhost:9080", "localhost:"+port);
            return content;
        }
        catch (IOException | URISyntaxException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
