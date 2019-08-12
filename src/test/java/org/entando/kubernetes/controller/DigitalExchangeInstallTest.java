package org.entando.kubernetes.controller;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.jayway.jsonpath.JsonPath;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.io.IOUtils;
import org.entando.kubernetes.DatabaseCleaner;
import org.entando.kubernetes.KubernetesClientMocker;
import org.entando.kubernetes.KubernetesPluginMocker;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.service.digitalexchange.model.DigitalExchange;
import org.entando.kubernetes.service.digitalexchange.signature.SignatureUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.entando.kubernetes.DigitalExchangeTestUtils.checkRequest;
import static org.entando.kubernetes.DigitalExchangeTestUtils.getTestPrivateKey;
import static org.entando.kubernetes.DigitalExchangeTestUtils.getTestPublicKey;
import static org.entando.kubernetes.DigitalExchangeTestUtils.readFile;
import static org.entando.kubernetes.DigitalExchangeTestUtils.readFileAsBase64;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 8099)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DigitalExchangeInstallTest {

    private static final String URL = "/digitalExchange";

    @Autowired private MockMvc mockMvc;
    @Autowired private DatabaseCleaner databaseCleaner;
    @Autowired private DigitalExchangeTestApi digitalExchangeTestApi;
    @Autowired private KubernetesClient client;

    private KubernetesClientMocker mocker;

    @Before
    public void setUp() {
        mocker = new KubernetesClientMocker(client);
    }

    @After
    public void cleanup() throws SQLException {
        databaseCleaner.cleanup();
    }

    @Test
    public void testInstallComponent() throws Exception {
        final DigitalExchange digitalExchange = new DigitalExchange();
        digitalExchange.setName("Community");
        digitalExchange.setUrl("http://localhost:8099/community");
        digitalExchange.setTimeout(10000);
        digitalExchange.setActive(true);
        digitalExchange.setPublicKey(getTestPublicKey());

        final URI dePKGPath = DigitalExchangeInstallTest.class.getResource("/bundle.depkg").toURI();
        final InputStream in = Files.newInputStream(Paths.get(dePKGPath), StandardOpenOption.READ);
        final String signature = SignatureUtil.signPackage(in, SignatureUtil.privateKeyFromPEM(getTestPrivateKey()));

        final String digitalExchangeId = digitalExchangeTestApi.createDigitalExchange(digitalExchange);

        final String pluginA =
                "{ \n" +
                "    \"id\": \"todomvc\", \n" +
                "    \"name\": \"Todo MVC\", \n" +
                "    \"type\": \"PLUGIN\", \n" +
                "    \"lastUpdate\": \"2019-07-17 16:50:05\", \n" +
                "    \"version\": \"latest\", \n" +
                "    \"image\": \"http://todomvc.com/site-assets/logo-icon.png\", \n" +
                "    \"description\": \"A great example to show a widget working\", \n" +
                "    \"rating\": 5, \n" +
                "    \"signature\": \"" + signature + "\" \n" +
                "}";

        final String response =
                "{ \n" +
                "    \"payload\": " + pluginA + ", \n" +
                "    \"metadata\": {}, \n" +
                "    \"errors\": [] \n" +
                "}";

        stubFor(WireMock.get(urlEqualTo("/community/api/digitalExchange/components/todomvc"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(response)));

        stubFor(WireMock.get(urlEqualTo("/community/api/digitalExchange/components/todomvc/package"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/octet-stream")
                        .withBody(readFromFile("bundle.depkg"))));

        stubFor(WireMock.post(urlEqualTo("/auth/protocol/openid-connect/auth"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"access_token\": \"iddqd\" }")));

        stubFor(WireMock.post(urlEqualTo("/entando-app/api/widgets")).willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.post(urlEqualTo("/entando-app/api/fileBrowser/file")).willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.post(urlEqualTo("/entando-app/api/fileBrowser/directory")).willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.post(urlEqualTo("/entando-app/api/pageModels")).willReturn(aResponse().withStatus(200)));

        final KubernetesPluginMocker pluginMocker = new KubernetesPluginMocker();
        mocker.mockResult("todomvc", pluginMocker.plugin);
        mocker.mockResult("avatar", null);

        mockMvc.perform(post(String.format("%s/%s/install/todomvc", URL, digitalExchangeId)))
                .andDo(print()).andExpect(status().isOk());

        waitFor(2000);

        final ArgumentCaptor<EntandoPlugin> captor = ArgumentCaptor.forClass(EntandoPlugin.class);
        verify(mocker.operation, times(1)).create(captor.capture());
        final EntandoPlugin plugin = captor.getValue();

        assertThat(plugin.getSpec().getDigitalExchangeId()).isEqualTo(digitalExchangeId);
        assertThat(plugin.getSpec().getDigitalExchangeUrl()).isEqualTo(digitalExchange.getUrl());

        assertThat(plugin.getSpec().getIngressPath()).isEqualTo("/todomvc");
        assertThat(plugin.getSpec().getDbms()).isEqualTo("mysql");
        assertThat(plugin.getSpec().getImage()).isEqualTo("entando/todomvc");
        assertThat(plugin.getSpec().getHealthCheckPath()).isEqualTo("/api/v1/todos");
        assertThat(plugin.getSpec().getReplicas()).isEqualTo(1);
        assertThat(plugin.getMetadata().getName()).isEqualTo("todomvc");

        assertThat(plugin.getSpec().getRoles()).hasSize(0);
        assertThat(plugin.getSpec().getPermissions()).hasSize(0);

        WireMock.verify(2, postRequestedFor(urlEqualTo("/entando-app/api/widgets")));
        WireMock.verify(2, postRequestedFor(urlEqualTo("/entando-app/api/pageModels")));
        WireMock.verify(3, postRequestedFor(urlEqualTo("/entando-app/api/fileBrowser/directory")));
        WireMock.verify(3, postRequestedFor(urlEqualTo("/entando-app/api/fileBrowser/file")));

        final List<LoggedRequest> widgetRequests = findAll(postRequestedFor(urlEqualTo("/entando-app/api/widgets")));
        final List<LoggedRequest> pageModelRequests = findAll(postRequestedFor(urlEqualTo("/entando-app/api/pageModels")));
        final List<LoggedRequest> directoryRequests = findAll(postRequestedFor(urlEqualTo("/entando-app/api/fileBrowser/directory")));
        final List<LoggedRequest> fileRequests = findAll(postRequestedFor(urlEqualTo("/entando-app/api/fileBrowser/file")));

        checkRequests(widgetRequests, pageModelRequests, directoryRequests, fileRequests);

        widgetRequests.sort(Comparator.comparing(DigitalExchangeInstallTest::requestCode));
        pageModelRequests.sort(Comparator.comparing(DigitalExchangeInstallTest::requestCode));
        directoryRequests.sort(Comparator.comparing(DigitalExchangeInstallTest::requestPath));
        fileRequests.sort(Comparator.comparing(DigitalExchangeInstallTest::requestPath));

        checkRequest(widgetRequests.get(0))
                .expectEqual("code", "another_todomvc_widget")
                .expectEqual("group", "free")
                .expectEqual("customUi", readFile("/bundle/widgets/widget.ftl"));

        checkRequest(widgetRequests.get(1))
                .expectEqual("code", "todomvc_widget")
                .expectEqual("group", "free")
                .expectEqual("customUi", "<h2>Bundle 1 Widget</h2>");

        checkRequest(pageModelRequests.get(0))
                .expectEqual("code", "todomvc_another_page_model")
                .expectEqual("descr", "TODO MVC another page model")
                .expectEqual("group", "free")
                .expectEqual("configuration.frames[0].pos", "0")
                .expectEqual("configuration.frames[0].descr", "Simple Frame")
                .expectEqual("template", readFile("/bundle/pagemodels/page.ftl"));

        checkRequest(pageModelRequests.get(1))
                .expectEqual("code", "todomvc_page_model")
                .expectEqual("descr", "TODO MVC basic page model")
                .expectEqual("group", "free")
                .expectEqual("configuration.frames[0].pos", "0")
                .expectEqual("configuration.frames[0].descr", "Header")
                .expectEqual("configuration.frames[0].sketch.x1", "0")
                .expectEqual("configuration.frames[0].sketch.y1", "0")
                .expectEqual("configuration.frames[0].sketch.x2", "11")
                .expectEqual("configuration.frames[0].sketch.y2", "0")
                .expectEqual("configuration.frames[1].pos", "1")
                .expectEqual("configuration.frames[1].descr", "Breadcrumb")
                .expectEqual("configuration.frames[1].sketch.x1", "0")
                .expectEqual("configuration.frames[1].sketch.y1", "1")
                .expectEqual("configuration.frames[1].sketch.x2", "11")
                .expectEqual("configuration.frames[1].sketch.y2", "1");

        checkRequest(directoryRequests.get(0))
                .expectEqual("path", "/todomvc")
                .expectEqual("protectedFolder", false);

        checkRequest(directoryRequests.get(1))
                .expectEqual("path", "/todomvc/css")
                .expectEqual("protectedFolder", false);

        checkRequest(directoryRequests.get(2))
                .expectEqual("path", "/todomvc/js")
                .expectEqual("protectedFolder", false);

        checkRequest(fileRequests.get(0))
                .expectEqual("filename", "custom.css")
                .expectEqual("path", "/todomvc/css/custom.css")
                .expectEqual("base64", readFileAsBase64("/bundle/resources/css/custom.css"));

        checkRequest(fileRequests.get(1))
                .expectEqual("filename", "style.css")
                .expectEqual("path", "/todomvc/css/style.css")
                .expectEqual("base64", readFileAsBase64("/bundle/resources/css/style.css"));

        checkRequest(fileRequests.get(2))
                .expectEqual("filename", "script.js")
                .expectEqual("path", "/todomvc/js/script.js")
                .expectEqual("base64", readFileAsBase64("/bundle/resources/js/script.js"));

        WireMock.reset();

        stubFor(WireMock.delete(urlEqualTo("/entando-app/api/widgets/todomvc_widget")).willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.delete(urlEqualTo("/entando-app/api/widgets/another_todomvc_widget")).willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.delete(urlEqualTo("/entando-app/api/pageModels/todomvc_page_model")).willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.delete(urlEqualTo("/entando-app/api/pageModels/todomvc_another_page_model")).willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.delete(urlEqualTo("/entando-app/api/fileBrowser/directory/todomvc")).willReturn(aResponse().withStatus(200)));

        mockMvc.perform(get(String.format("%s/install/todomvc", URL)))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("payload.componentId").value("todomvc"))
                .andExpect(jsonPath("payload.status").value("COMPLETED"));

        mockMvc.perform(post(String.format("%s/uninstall/todomvc", URL)))
                .andDo(print()).andExpect(status().isOk());

        waitFor(2000);

        WireMock.verify(1, deleteRequestedFor(urlEqualTo("/entando-app/api/widgets/todomvc_widget")));
        WireMock.verify(1, deleteRequestedFor(urlEqualTo("/entando-app/api/widgets/another_todomvc_widget")));
        WireMock.verify(1, deleteRequestedFor(urlEqualTo("/entando-app/api/pageModels/todomvc_page_model")));
        WireMock.verify(1, deleteRequestedFor(urlEqualTo("/entando-app/api/pageModels/todomvc_another_page_model")));
        WireMock.verify(1, deleteRequestedFor(urlEqualTo("/entando-app/api/fileBrowser/directory/todomvc")));

        verify(mocker.operation, times(1)).delete(same(pluginMocker.plugin));
    }

    private byte [] readFromFile(final String fileName) throws IOException {
        try (final InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                Assert.assertNotNull(inputStream);
                IOUtils.copy(inputStream, outputStream);
                return outputStream.toByteArray();
            }
        }
    }

    @SafeVarargs
    private final void checkRequests(final List<LoggedRequest> ... requests) {
        final List<LoggedRequest> allRequests = new ArrayList<>();
        for (List<LoggedRequest> request : requests) {
            allRequests.addAll(request);
        }

        for (final LoggedRequest req : allRequests) {
            assertThat(req.getHeader("Authorization")).isEqualTo("Bearer iddqd");
        }
    }

    public void waitFor(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) { e.printStackTrace(); }
    }

    private static String requestProperty(final LoggedRequest request, final String property) {
        return JsonPath.read(new String(request.getBody()), property);
    }

    private static String requestCode(final LoggedRequest request) {
        return requestProperty(request, "code");
    }

    private static String requestPath(final LoggedRequest request) {
        return requestProperty(request, "path");
    }

}
