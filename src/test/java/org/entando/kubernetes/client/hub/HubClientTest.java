package org.entando.kubernetes.client.hub;

import org.entando.kubernetes.client.hub.domain.BundleDto;
import org.entando.kubernetes.client.hub.domain.BundleGroupVersionFilteredResponseView;
import org.entando.kubernetes.client.hub.domain.HubDescriptorVersion;
import org.entando.kubernetes.client.hub.domain.PagedContent;
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
import org.springframework.http.HttpStatus;
import wiremock.org.apache.http.HttpResponse;
import wiremock.org.apache.http.client.methods.HttpGet;
import wiremock.org.apache.http.impl.client.CloseableHttpClient;
import wiremock.org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Scanner;

import static org.entando.kubernetes.utils.EntandoHubMockServer.BUNDLEGROUP_RESPONSE_JSON;
import static org.entando.kubernetes.utils.EntandoHubMockServer.BUNDLE_RESPONSE_JSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Tag("unit")
@AutoConfigureMockMvc
public class HubClientTest {

    @Spy
    private DefaultHubClient hubClientService = new DefaultHubClient();

    private static EntandoHubMockServer mockServer;

    @BeforeEach
    public void setup() throws Exception {
        mockServer = new EntandoHubMockServer();
    }

    @AfterEach
    public void reset() {
        mockServer.tearDown();
    }

    @Test
    public void testBundleGroupServerClient() throws Throwable {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet request = new HttpGet(mockServer.getApiRoot() + "/appbuilder/api/bundlegroups/?page=1&descriptorVersions=v5&descriptorVersions=v1&pageSize=1");
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
    public void testBundleServerClient() throws Throwable {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet request = new HttpGet(mockServer.getApiRoot() + "/appbuilder/api/bundles/?descriptorVersions=v1&descriptorVersions=v5&pageSize=1&page=1");
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
    public void testBundleServiceNoParams() {
        ProxiedPayload proxiedPayload =
                hubClientService.getBundles(mockServer.getApiRoot(), null);
        assertNotNull(proxiedPayload);
        assertThat(proxiedPayload.getPayload(), equalTo(null));
        assertThat(proxiedPayload.getStatus(), equalTo(null));
        assertThat(proxiedPayload.getExceptionMessage(), is(notNullValue()));
        assertThat(proxiedPayload.getExceptionClass(), is(notNullValue()));
    }

    @Test
    public void testBundleServiceNoData() {
        ProxiedPayload proxiedPayload =
                hubClientService.getBundles(null, null);
        assertNotNull(proxiedPayload);
        assertThat(proxiedPayload.getPayload(), equalTo(null));
        assertThat(proxiedPayload.getStatus(), equalTo(null));
        assertThat(proxiedPayload.getExceptionMessage(), is(notNullValue()));
        assertThat(proxiedPayload.getExceptionClass(), is(notNullValue()));
    }

    @Test
    public void testBundleService() throws JSONException {
        try {
            assertNotNull(hubClientService);
            LinkedHashMap<String, Object> params = new LinkedHashMap<>();
            params.put("descriptorVersions", new String[] {"v1", "v5"});
            params.put("pageSize", "1");
            params.put("page", "1");

            ProxiedPayload proxiedPayload =
                    hubClientService.getBundles(mockServer.getApiRoot(), params);
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
    public void testBundleGroupServiceNoParams() {
        ProxiedPayload proxiedPayload =
                hubClientService.searchBundleGroupVersions(mockServer.getApiRoot(), null);
        assertNotNull(proxiedPayload);
        assertThat(proxiedPayload.getPayload(), equalTo(null));
        assertThat(proxiedPayload.getStatus(), equalTo(null));
        assertThat(proxiedPayload.getExceptionMessage(), is(notNullValue()));
        assertThat(proxiedPayload.getExceptionClass(), is(notNullValue()));
    }

    @Test
    public void testBundleGroupServiceNoData() {
        ProxiedPayload proxiedPayload =
                hubClientService.searchBundleGroupVersions(null, null);
        assertNotNull(proxiedPayload);
        assertThat(proxiedPayload.getPayload(), equalTo(null));
        assertThat(proxiedPayload.getStatus(), equalTo(null));
        assertThat(proxiedPayload.getExceptionMessage(), is(notNullValue()));
        assertThat(proxiedPayload.getExceptionClass(), is(notNullValue()));
    }

    @Test
    public void testBundleGroupService() throws JSONException {
        try {
            assertNotNull(hubClientService);

//        ProxiedPayload proxiedPayload =
//                hubClientService.getHubBundleGroups(mockServer.getApiRoot(), Map.of(
//                        "page", "1",
//                        "descriptorVersions", new String[] {"v5","v1"},
//                        "pageSize", "1"));

            LinkedHashMap<String, Object> params = new LinkedHashMap<>();
            params.put("page", "1");
            params.put("descriptorVersions", new String[] {"v5","v1"});
            params.put("pageSize", "1");

            ProxiedPayload proxiedPayload =
                    hubClientService.searchBundleGroupVersions(mockServer.getApiRoot(), params);
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
            assertThat(bgv.getBundleGroupId(), equalTo((long)1));
            assertThat(bgv.getBundleGroupVersionId(), equalTo((long)4));
            assertThat(bgv.getDocumentationUrl(), equalTo("http://docm.me"));
            assertThat(bgv.isPublicCatalog(), equalTo(true));
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
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
        assertTrue(!group.has("bundleId"));
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
        assertTrue(!group.has("bundleGroupId"));
        assertTrue(!group.has("bundleGroupVersionId"));
    }

    private String convertResponseToString(HttpResponse response) throws IOException {
        InputStream responseStream = response.getEntity().getContent();
        Scanner scanner = new Scanner(responseStream, "UTF-8");
        String responseString = scanner.useDelimiter("\\Z").next();
        scanner.close();
        return responseString;
    }

}
