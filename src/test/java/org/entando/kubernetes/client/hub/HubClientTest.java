package org.entando.kubernetes.client.hub;

import static org.entando.kubernetes.utils.EntandoHubMockServer.BUNDLEGROUP_RESPONSE_JSON;
import static org.entando.kubernetes.utils.EntandoHubMockServer.BUNDLE_RESPONSE_JSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.entando.kubernetes.client.hub.domain.BundleDto;
import org.entando.kubernetes.client.hub.domain.BundleGroupVersionFilteredResponseView;
import org.entando.kubernetes.client.hub.domain.HubDescriptorVersion;
import org.entando.kubernetes.client.hub.domain.PagedContent;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.entandohub.EntandoHubRegistry;
import org.entando.kubernetes.stubhelper.EntandoHubRegistryStubHelper;
import org.entando.kubernetes.stubhelper.HubStubHelper;
import org.entando.kubernetes.utils.EntandoHubMockServer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import wiremock.org.apache.http.HttpResponse;
import wiremock.org.apache.http.client.methods.HttpGet;
import wiremock.org.apache.http.impl.client.CloseableHttpClient;
import wiremock.org.apache.http.impl.client.HttpClients;

@Tag("unit")
@AutoConfigureMockMvc
class HubClientTest {

    private static EntandoHubMockServer mockServer;
    @Spy
    private DefaultHubClient hubClient = new DefaultHubClient();

    private EntandoHubRegistry registry;

    @BeforeEach
    public void setup() throws Exception {
        mockServer = new EntandoHubMockServer();
        registry = EntandoHubRegistry.builder().url(mockServer.getApiRoot()).build();
    }

    @AfterEach
    public void reset() {
        mockServer.tearDown();
    }

    @Test
    void testBundleGroupServerClient() throws Throwable {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet request = new HttpGet(mockServer.getApiRoot()
                + "/bundlegroups/?page=1&descriptorVersions=v5&descriptorVersions=v1&pageSize=1");
        try {
            HttpResponse httpResponse = httpClient.execute(request);
            String responseString = convertResponseToString(httpResponse);
            testBundleGroupPayload(responseString);
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    void testBundleServerClient() throws Throwable {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet request = new HttpGet(mockServer.getApiRoot()
                + "/bundles/?descriptorVersions=v1&descriptorVersions=v5&pageSize=1&page=1");
        try {
            HttpResponse httpResponse = httpClient.execute(request);
            String responseString = convertResponseToString(httpResponse);
            testBundlePayload(responseString);
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    void testBundleServiceNoParams() {
        ProxiedPayload proxiedPayload =
                hubClient.getBundles(registry, null);
        assertNotNull(proxiedPayload);
        assertThat(proxiedPayload.getPayload(), equalTo(null));
        assertThat(proxiedPayload.getStatus(), equalTo(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThat(proxiedPayload.getExceptionMessage(), is(notNullValue()));
        assertThat(proxiedPayload.getExceptionClass(), is(notNullValue()));
    }

    @Test
    void testBundleServiceNoData() {
        ProxiedPayload proxiedPayload =
                hubClient.getBundles(registry, null);
        assertNotNull(proxiedPayload);
        assertThat(proxiedPayload.getPayload(), equalTo(null));
        assertThat(proxiedPayload.getStatus(), equalTo(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThat(proxiedPayload.getExceptionMessage(), is(notNullValue()));
        assertThat(proxiedPayload.getExceptionClass(), is(notNullValue()));
    }

    @Test
    void testBundleService() throws JSONException {
        try {
            assertNotNull(hubClient);
            LinkedHashMap<String, Object> params = new LinkedHashMap<>();
            params.put("descriptorVersions", new String[]{"v1", "v5"});
            params.put("pageSize", "1");
            params.put("page", "1");

            ProxiedPayload proxiedPayload =
                    hubClient.getBundles(registry, params);
            assertThat(proxiedPayload.getStatus(), equalTo(HttpStatus.OK));
            assertNotNull(proxiedPayload);
            assertNotNull(proxiedPayload.getPayload());
            assertNotNull(proxiedPayload.getStatus());
            assertThat(proxiedPayload.getStatus(), equalTo(HttpStatus.OK));
            assertThat(proxiedPayload.getPayload(), instanceOf(PagedContent.class));
            PagedContent pc = (PagedContent) proxiedPayload.getPayload();
            assertNotNull(pc.getMetadata());
            assertNotNull(pc.getPayload());
            assertThat(pc.getPayload(), instanceOf(List.class));

            Object elem = pc.getPayload().get(0);
            assertThat(elem, instanceOf(BundleDto.class));

            BundleDto bundle = (BundleDto) elem;
            assertThat(bundle.getBundleId(), equalTo("13"));
            assertThat(bundle.getName(), equalTo("bundle-uri-1"));
            assertThat(bundle.getDescription(), equalTo("Description default"));
            assertThat(bundle.getGitRepoAddress(), equalTo("https://github.com/account/bundle-uri-1.git"));
            assertThat(bundle.getGitSrcRepoAddress(), equalTo("https://github.com/account/source-url-1.git"));
            assertThat(bundle.getDescriptorVersion(), equalTo(HubDescriptorVersion.V1.toString()));
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }

    @Test
    void testBundleGroupServiceNoParams() {
        ProxiedPayload proxiedPayload =
                hubClient.searchBundleGroupVersions(registry, null);
        assertNotNull(proxiedPayload);
        assertThat(proxiedPayload.getPayload(), equalTo(null));
        assertThat(proxiedPayload.getStatus(), equalTo(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThat(proxiedPayload.getExceptionMessage(), is(notNullValue()));
        assertThat(proxiedPayload.getExceptionClass(), is(notNullValue()));
    }

    @Test
    void testBundleGroupServiceNoData() {
        assertThrows(EntandoComponentManagerException.class,
                () -> hubClient.searchBundleGroupVersions(null, null));
    }

    @Test
    void testBundleGroupService() throws JSONException {
        try {
            LinkedHashMap<String, Object> params = new LinkedHashMap<>();
            params.put("page", "1");
            params.put("descriptorVersions", new String[]{"v5", "v1"});
            params.put("pageSize", "1");

            ProxiedPayload proxiedPayload =
                    hubClient.searchBundleGroupVersions(registry, params);
            assertThat(proxiedPayload.getStatus(), equalTo(HttpStatus.OK));
            assertNotNull(proxiedPayload);
            assertNotNull(proxiedPayload.getPayload());
            assertThat(proxiedPayload.getPayload(), instanceOf(PagedContent.class));

            PagedContent pc = (PagedContent) proxiedPayload.getPayload();
            assertNotNull(pc.getMetadata());
            assertNotNull(pc.getPayload());
            assertThat(pc.getPayload(), instanceOf(List.class));

            Object elem = pc.getPayload().get(0);
            assertThat(elem, instanceOf(BundleGroupVersionFilteredResponseView.class));
            BundleGroupVersionFilteredResponseView bgv = (BundleGroupVersionFilteredResponseView) elem;
            assertThat(bgv.getBundleGroupId(), equalTo((long) 1));
            assertThat(bgv.getBundleGroupVersionId(), equalTo((long) 4));
            assertThat(bgv.getDocumentationUrl(), equalTo("http://docm.me"));
            assertThat(bgv.isPublicCatalog(), equalTo(true));
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }

    @Test
    void shouldThrowExceptionWhileReceivingANullRegistryOrEmptyUrl() {
        final Map<String, Object> emptyParams = new LinkedHashMap<>();
        final EntandoHubRegistry emptyRegistry = new EntandoHubRegistry();
        assertThrows(EntandoComponentManagerException.class, () -> hubClient.getBundles(null, emptyParams));
        assertThrows(EntandoComponentManagerException.class, () -> hubClient.getBundles(emptyRegistry, emptyParams));
        assertThrows(EntandoComponentManagerException.class, () -> hubClient.searchBundleGroupVersions(null, emptyParams));
        assertThrows(EntandoComponentManagerException.class, () -> hubClient.searchBundleGroupVersions(emptyRegistry, emptyParams));
    }

    private void testBundleGroupPayload(String payload) throws JSONException {
        assertThat(payload, equalTo(BUNDLEGROUP_RESPONSE_JSON));
        assertNotNull(payload);
        JSONObject json = new JSONObject(payload);
        assertTrue(json.has("payload"));
        JSONArray array = json.getJSONArray("payload");
        assertThat(array.length(), equalTo(1));
        // identify bundle group payload
        JSONObject group = array.getJSONObject(0);
        assertTrue(group.has("bundleGroupId"));
        assertTrue(group.has("bundleGroupVersionId"));
        assertFalse(group.has("bundleId"));
    }

    private void testBundlePayload(String payload) throws JSONException {
        assertThat(payload, equalTo(BUNDLE_RESPONSE_JSON));
        assertNotNull(payload);
        JSONObject json = new JSONObject(payload);
        assertTrue(json.has("payload"));
        JSONArray array = json.getJSONArray("payload");
        assertThat(array.length(), equalTo(1));
        // identify boundle group payload
        JSONObject group = array.getJSONObject(0);
        assertTrue(group.has("bundleId"));
        assertFalse(group.has("bundleGroupId"));
        assertFalse(group.has("bundleGroupVersionId"));
    }

    private String convertResponseToString(HttpResponse response) throws IOException {
        InputStream responseStream = response.getEntity().getContent();
        Scanner scanner = new Scanner(responseStream, StandardCharsets.UTF_8);
        String responseString = scanner.useDelimiter("\\Z").next();
        scanner.close();
        return responseString;
    }

    @Test
    void shouldAddTheHeaderWhenAnApiKeyIsPresent() {
        EntandoHubRegistry registry = EntandoHubRegistryStubHelper.stubEntandoHubRegistry4();
        final HttpEntity<Void> httpEntity = hubClient.composeWithApiKeyHeader(registry);
        final HttpHeaders headers = httpEntity.getHeaders();
        assertThat(headers.size(), equalTo(1));
        assertThat(headers.get(HubStubHelper.API_KEY_HEADER_NAME), contains(registry.getApiKey()));
    }

    @Test
    void shouldNOTAddTheHeaderWhenAnApiKeyIsNOTPresent() {
        EntandoHubRegistry registry = EntandoHubRegistryStubHelper.stubEntandoHubRegistry1();
        final HttpEntity<Void> httpEntity = hubClient.composeWithApiKeyHeader(registry);
        assertNull(httpEntity);
    }

    @Test
    void testBundleGroupServerClientWithApiKey() throws Throwable {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet request = new HttpGet(mockServer.getApiRoot()
                + "/appbuilder/api/bundlegroups/?catalogId=1&page=1&descriptorVersions=v5&descriptorVersions=v1&pageSize=1");
        request.addHeader(HubStubHelper.API_KEY_HEADER_NAME, HubStubHelper.API_KEY_HEADER_VALUE);
        try {
            HttpResponse httpResponse = httpClient.execute(request);
            String responseString = convertResponseToString(httpResponse);
            testBundleGroupPayload(responseString);
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    void testBundleServerClientWithApiKey() throws Throwable {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet request = new HttpGet(mockServer.getApiRoot()
                + "/appbuilder/api/bundles/?catalogId=1&descriptorVersions=v1&descriptorVersions=v5&pageSize=1&page=1");
        request.addHeader(HubStubHelper.API_KEY_HEADER_NAME, HubStubHelper.API_KEY_HEADER_VALUE);
        try {
            HttpResponse httpResponse = httpClient.execute(request);
            String responseString = convertResponseToString(httpResponse);
            testBundlePayload(responseString);
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }
}
