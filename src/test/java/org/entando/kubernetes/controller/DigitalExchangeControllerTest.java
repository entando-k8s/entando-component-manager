package org.entando.kubernetes.controller;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.entando.kubernetes.DatabaseCleaner;
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
import static com.jayway.jsonpath.JsonPath.parse;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
public class DigitalExchangeControllerTest {

    private static final String EXCHANGES_URL = "/digitalExchange/exchanges";

    @Autowired private MockMvc mockMvc;
    @Autowired private DatabaseCleaner databaseCleaner;

    @After
    public void cleanup() throws SQLException {
        databaseCleaner.cleanup();
    }

    @Test
    public void testDigitalExchangeCreation() throws Exception {
        final String json =
                "{" +
                "  \"name\": \"Community\", \n" +
                "  \"url\": \"http://localhost:8099/community\", \n" +
                "  \"timeout\": 10000, \n" +
                "  \"active\": true \n" +
                "}";

        final String jsonResponse = mockMvc.perform(post(EXCHANGES_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("payload.id").exists())
                .andExpect(jsonPath("payload.name").value("Community"))
                .andExpect(jsonPath("payload.url").value("http://localhost:8099/community"))
                .andExpect(jsonPath("payload.timeout").value(10000))
                .andExpect(jsonPath("payload.active").value(true))
                .andReturn().getResponse().getContentAsString();

        final String uuid = parse(jsonResponse).read("$.payload.id").toString();

        mockMvc.perform(get(String.format("%s/test/%s", EXCHANGES_URL, uuid)))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("payload").value(""))
                .andExpect(jsonPath("errors", hasSize(1)))
                .andExpect(jsonPath("errors[0].code").value("1"))
                .andExpect(jsonPath("errors[0].message").value("The Digital Exchange 'Community' answered with HTTP status '404'"));

        stubFor(WireMock.get(urlEqualTo("/community/api/digitalExchange/exchanges/test"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"payload\": {}, \"metadata\": {}, \"errors\": [] }")));

        mockMvc.perform(get(String.format("%s/test/%s", EXCHANGES_URL, uuid)))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("payload").value("OK"))
                .andExpect(jsonPath("errors", hasSize(0)));

        mockMvc.perform(get(EXCHANGES_URL))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("payload", hasSize(1)))
                .andExpect(jsonPath("payload[0].id").exists())
                .andExpect(jsonPath("payload[0].name").value("Community"))
                .andExpect(jsonPath("payload[0].url").value("http://localhost:8099/community"))
                .andExpect(jsonPath("payload[0].timeout").value(10000))
                .andExpect(jsonPath("payload[0].active").value(true));

        mockMvc.perform(get(String.format("%s/test", EXCHANGES_URL)))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath(String.format("payload['%s']", uuid), hasSize(0)))
                .andExpect(jsonPath("errors", hasSize(0)));

        final String anotherJson =
                "{" +
                "  \"name\": \"Community Made\", \n" +
                "  \"url\": \"http://localhost:8099/community-made\", \n" +
                "  \"timeout\": 20000, \n" +
                "  \"active\": true \n" +
                "}";

        mockMvc.perform(put(String.format("%s/%s", EXCHANGES_URL, uuid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(anotherJson))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("payload.id").value(uuid))
                .andExpect(jsonPath("payload.name").value("Community Made"))
                .andExpect(jsonPath("payload.url").value("http://localhost:8099/community-made"))
                .andExpect(jsonPath("payload.timeout").value(20000))
                .andExpect(jsonPath("payload.active").value(true));

        mockMvc.perform(get(String.format("%s/%s", EXCHANGES_URL, uuid)))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("payload.id").value(uuid))
                .andExpect(jsonPath("payload.name").value("Community Made"))
                .andExpect(jsonPath("payload.url").value("http://localhost:8099/community-made"))
                .andExpect(jsonPath("payload.timeout").value(20000))
                .andExpect(jsonPath("payload.active").value(true));

        mockMvc.perform(delete(String.format("%s/%s", EXCHANGES_URL, uuid)))
                .andDo(print()).andExpect(status().isOk());

        mockMvc.perform(get(EXCHANGES_URL))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("payload", hasSize(0)));
    }

}
