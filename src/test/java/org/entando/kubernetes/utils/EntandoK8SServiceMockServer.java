package org.entando.kubernetes.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.stubhelper.BundleInfoStubHelper;
import org.springframework.http.HttpStatus;

@Slf4j
public class EntandoK8SServiceMockServer extends EntandoGenericMockServer {

    public EntandoK8SServiceMockServer() {
        super();
    }

    @Override
    protected void init(WireMockServer wireMockServer) {
        addApiRoot(wireMockServer);
        addAppPluginLinksResource(wireMockServer);
        addAppsResource(wireMockServer);
        addPluginsResource(wireMockServer);
        addBundlesResource(wireMockServer);
        addNamespacesResource(wireMockServer);
        addDeployDeBundleSuccess(wireMockServer);
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
        String myNamespaceResponse = readResourceAsString(
                "/payloads/k8s-svc/observed-namespaces/namespace-my-namespace.json");
        String entandoDeBundlesNamespaceResponse = readResourceAsString(
                "/payloads/k8s-svc/observed-namespaces/namespace-entando-de-bundles.json");
        String pluginNamespaceResponse = readResourceAsString(
                "/payloads/k8s-svc/observed-namespaces/namespace-plugin-namespace.json");
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
        String createdLinkResponse = this
                .readResourceAsString("/payloads/k8s-svc/app-plugin-links/app-plugin-link.json");
        String appIngressResponse = this.readResourceAsString("/payloads/k8s-svc/apps/app-ingress.json");

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
        wireMockServer.stubFor(get(urlMatching("/apps/my-app/ingress/?"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", HAL_JSON_VALUE)
                        .withBody(appIngressResponse)));
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
        wireMockServer.stubFor(delete(urlMatching("/plugins/[a-z0-9A-Z\\-]+/ingress"))
                .withRequestBody(new AnythingPattern())
                .willReturn(aResponse()
                        .withStatus(204)));


    }

    private void addAppPluginLinksResource(WireMockServer wireMockServer) {
        String linkListResponse = this.readResourceAsString("/payloads/k8s-svc/app-plugin-links/app-plugin-links.json");
        String singleLinkResponse = this
                .readResourceAsString("/payloads/k8s-svc/app-plugin-links/app-plugin-link.json");

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
        wireMockServer.stubFor(delete(urlMatching("/app-plugin-links/delete-and-scale-down/[a-zA-Z\\-]+/?"))
                .withRequestBody(new AnythingPattern())
                .willReturn(aResponse().withStatus(204)));
    }

    public void addApiRoot(WireMockServer server) {
        String rootResponse = this.readResourceAsString("/payloads/k8s-svc/root.json");
        server.stubFor(
                get(urlMatching("/?"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", HAL_JSON_VALUE)
                                .withBody(rootResponse)));

    }

    private void addDeployDeBundleSuccess(WireMockServer server) {
        addDeployDeBundle(server, HttpStatus.OK.value());
    }

    public void addDeployDeBundleFail(WireMockServer server) {
        addDeployDeBundle(server, HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    private void addDeployDeBundle(WireMockServer server, int status) {
        server.stubFor(post(urlMatching("/bundles"))
                        .withRequestBody(new AnythingPattern())
                        .willReturn(aResponse()
                                .withStatus(status)
                                .withHeader("Content-Type", HAL_JSON_VALUE)));
    }

    public void addUndeployDeBundle(WireMockServer server) {
        server.stubFor(delete(urlMatching("/bundles/" + BundleInfoStubHelper.NAME))
                .willReturn(aResponse()
                        .withStatus(200)));
    }

}
