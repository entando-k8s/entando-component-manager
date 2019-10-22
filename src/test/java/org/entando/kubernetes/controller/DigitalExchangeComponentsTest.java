package org.entando.kubernetes.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.client.WireMock;
import java.sql.SQLException;
import org.entando.kubernetes.DatabaseCleaner;
import org.entando.kubernetes.KubernetesPluginMocker;
import org.entando.kubernetes.client.K8SServiceClient;
import org.entando.kubernetes.client.K8SServiceClientTestDouble;
import org.entando.kubernetes.model.link.EntandoAppPluginLink;
import org.entando.kubernetes.model.link.EntandoAppPluginLinkBuilder;
import org.entando.kubernetes.service.digitalexchange.model.DigitalExchange;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 8099)
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DigitalExchangeComponentsTest {

    private static final String URL = "/components";

    @Value("${entando.app.name}")
    private String entandoAppName;

    @Value("${entando.app.namespace}")
    private String entandoAppNamespace;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private DatabaseCleaner databaseCleaner;
    @Autowired
    private DigitalExchangeTestApi digitalExchangeTestApi;
    @Autowired
    private K8SServiceClient k8SServiceClient;


    @After
    public void cleanup() throws SQLException {
        databaseCleaner.cleanup();
        ((K8SServiceClientTestDouble) k8SServiceClient).cleanInMemoryDatabase();
    }

    @Test
    public void testListComponents() throws Exception {
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
        final String pluginB =
                "{ \n" +
                        "    \"id\": \"avatar\", \n" +
                        "    \"name\": \"Avatar\", \n" +
                        "    \"type\": \"PLUGIN\", \n" +
                        "    \"lastUpdate\": \"2019-07-17 16:50:05\", \n" +
                        "    \"version\": \"latest\", \n" +
                        "    \"image\": \"http://img.com/avatar-plugin.png\", \n" +
                        "    \"description\": \"The avatar plugin\", \n" +
                        "    \"rating\": 4.5 \n" +
                        "}";

        final String response =
                "{ \n" +
                        "    \"payload\": [" + pluginB + "," + pluginA + "], \n" +
                        "    \"metadata\": { \n" +
                        "      \"page\": 1, \n" +
                        "      \"pageSize\": 100, \n" +
                        "      \"lastPage\": 1, \n" +
                        "      \"totalItems\": 0, \n" +
                        "      \"sort\": \"id\", \n" +
                        "      \"direction\": \"ASC\", \n" +
                        "      \"filters\": [] \n" +
                        "    }, \n" +
                        "    \"errors\": [] \n" +
                        "  }";

        EntandoAppPluginLink linkBetweenAppAndTodoMvcPlugin = new EntandoAppPluginLinkBuilder()
                .withNewSpec()
                .withEntandoApp(entandoAppNamespace, entandoAppName)
                .withEntandoPlugin(entandoAppNamespace, "todomvc")
                .endSpec()
                .withNewMetadata()
                .withNamespace(entandoAppNamespace)
                .withName("link-" + entandoAppName + "-to-todomvc-plugin")
                .endMetadata()
                .build();

        K8SServiceClientTestDouble k8SServiceClientTestDouble = (K8SServiceClientTestDouble) k8SServiceClient;
        k8SServiceClientTestDouble.addInMemoryLink(linkBetweenAppAndTodoMvcPlugin);

        stubFor(WireMock.get(urlEqualTo(
                "/community/api/digitalExchange/components?page=1&pageSize=2147483647&direction=ASC&sort=id"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(response)));

        final KubernetesPluginMocker pluginMocker = new KubernetesPluginMocker();

        mockMvc.perform(get(URL))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("payload", hasSize(2)))
                .andExpect(jsonPath("payload[0].id").value("avatar"))
                .andExpect(jsonPath("payload[0].name").value("Avatar"))
                .andExpect(jsonPath("payload[0].type").value("PLUGIN"))
                .andExpect(jsonPath("payload[0].lastUpdate").value("2019-07-17 16:50:05"))
                .andExpect(jsonPath("payload[0].version").value("latest"))
                .andExpect(jsonPath("payload[0].description").value("The avatar plugin"))
                .andExpect(jsonPath("payload[0].image").value("http://img.com/avatar-plugin.png"))
                .andExpect(jsonPath("payload[0].rating").value(4.5))
                .andExpect(jsonPath("payload[0].installed").value(false))
                .andExpect(jsonPath("payload[0].digitalExchangeName").value("Community"))
                .andExpect(jsonPath("payload[0].digitalExchangeId").value(digitalExchangeId))
                .andExpect(jsonPath("payload[1].id").value("todomvc"))
                .andExpect(jsonPath("payload[1].name").value("Todo MVC"))
                .andExpect(jsonPath("payload[1].type").value("PLUGIN"))
                .andExpect(jsonPath("payload[1].lastUpdate").value("2019-07-17 16:50:05"))
                .andExpect(jsonPath("payload[1].version").value("latest"))
                .andExpect(jsonPath("payload[1].description").value("A great example to show a widget working"))
                .andExpect(jsonPath("payload[1].image").value("http://todomvc.com/site-assets/logo-icon.png"))
                .andExpect(jsonPath("payload[1].rating").value(5.0))
                .andExpect(jsonPath("payload[1].installed").value(true))
                .andExpect(jsonPath("payload[1].digitalExchangeName").value("Community"))
                .andExpect(jsonPath("payload[1].digitalExchangeId").value(digitalExchangeId));

    }

}
