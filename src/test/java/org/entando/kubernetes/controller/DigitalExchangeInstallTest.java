package org.entando.kubernetes.controller;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.io.IOUtils;
import org.entando.kubernetes.DatabaseCleaner;
import org.entando.kubernetes.KubernetesClientMocker;
import org.entando.kubernetes.KubernetesPluginMocker;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.service.digitalexchange.model.DigitalExchange;
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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
                "    \"rating\": 5 \n" +
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

        stubFor(WireMock.post(urlEqualTo("/entando-app/api/widgets"))
                .willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.post(urlEqualTo("/entando-app/api/fileBrowser/file"))
                .willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.post(urlEqualTo("/entando-app/api/fileBrowser/directory"))
                .willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.post(urlEqualTo("/entando-app/api/pageModels"))
                .willReturn(aResponse().withStatus(200)));

        final KubernetesPluginMocker pluginMocker = new KubernetesPluginMocker();
        mocker.mockResult("todomvc", pluginMocker.plugin);
        mocker.mockResult("avatar", null);

        mockMvc.perform(post(String.format("%s/%s/install/todomvc", URL, digitalExchangeId)))
                .andDo(print()).andExpect(status().isOk());

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
        WireMock.verify(2, postRequestedFor(urlEqualTo("/entando-app/api/fileBrowser/directory")));
        WireMock.verify(3, postRequestedFor(urlEqualTo("/entando-app/api/fileBrowser/file")));

        final List<LoggedRequest> widgetRequests = findAll(postRequestedFor(urlEqualTo("/entando-app/api/widgets")));
        final List<LoggedRequest> pageModelRequests = findAll(postRequestedFor(urlEqualTo("/entando-app/api/pageModels")));
        final List<LoggedRequest> directoryRequests = findAll(postRequestedFor(urlEqualTo("/entando-app/api/fileBrowser/directory")));
        final List<LoggedRequest> fileRequests = findAll(postRequestedFor(urlEqualTo("/entando-app/api/fileBrowser/file")));

        checkRequests(widgetRequests, pageModelRequests, directoryRequests, fileRequests);
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

}
