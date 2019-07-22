package org.entando.kubernetes.controller;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.entando.kubernetes.DatabaseCleaner;
import org.entando.kubernetes.service.digitalexchange.model.DigitalExchange;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.SQLException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 8099)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DigitalExchangeCategoriesTest {

    private static final String URL = "/digitalExchange/categories";

    @Autowired private MockMvc mockMvc;
    @Autowired private DatabaseCleaner databaseCleaner;
    @Autowired private DigitalExchangeTestApi digitalExchangeTestApi;

    @After
    public void cleanup() throws SQLException {
        databaseCleaner.cleanup();
    }

    @Test
    public void testListCategories() throws Exception {
        final DigitalExchange digitalExchange = new DigitalExchange();
        digitalExchange.setName("Community");
        digitalExchange.setUrl("http://localhost:8099/community");
        digitalExchange.setTimeout(10000);
        digitalExchange.setActive(true);

        digitalExchangeTestApi.createDigitalExchange(digitalExchange);

        stubFor(WireMock.get(urlEqualTo("/community/api/digitalExchange/categories"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"payload\": [\"Widgets\"], \"metadata\": {}, \"errors\": [] }")));

        mockMvc.perform(get(URL))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("payload", hasSize(1)))
                .andExpect(jsonPath("payload[0]").value("Widgets"));
    }

}
