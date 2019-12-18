package org.entando.kubernetes.controller;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.entando.kubernetes.DatabaseCleaner;
import org.entando.kubernetes.controller.digitalexchange.model.DigitalExchange;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
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
public class DigitalExchangeRatingTest {

    private static final String URL = "/components/rate";

    @Autowired private MockMvc mockMvc;
    @Autowired private DatabaseCleaner databaseCleaner;
    @Autowired private DigitalExchangeTestApi digitalExchangeTestApi;

    @After
    public void cleanup() throws SQLException {
        databaseCleaner.cleanup();
    }

    @Test
    public void testRateSuccess() throws Exception {
        final DigitalExchange digitalExchange = DigitalExchange.builder()
                .name("Community")
                .url("http://localhost:8099/community")
                .timeout(10000)
                .active(true).build();

        final String digitalExchangeId = digitalExchangeTestApi.createDigitalExchange(digitalExchange);
        final String rateResponse =
                "{ \n" +
                "    \"payload\": {" +
                "        \"componentId\": \"avatar\", \n" +
                "        \"rating\": 4.5, \n" +
                "        \"numberOfRatings\": 6984 \n" +
                "    }, \n" +
                "    \"metadata\": {}, \n" +
                "    \"errors\": [] \n" +
                "  }";

        stubFor(WireMock.post(urlEqualTo("/community/api/digitalExchange/components/avatar/rate"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody(rateResponse)));

        mockMvc.perform(post(String.format("%s/%s/%s", URL, digitalExchangeId, "avatar"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"rating\": 4 }"))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("payload.componentId").value("avatar"))
                .andExpect(jsonPath("payload.rating").value(4.5))
                .andExpect(jsonPath("payload.numberOfRatings").value(6984));
    }

    @Test
    public void testRateNotSupported() throws Exception {
        final DigitalExchange digitalExchange = DigitalExchange.builder()
                .name("Community")
                .url("http://localhost:8099/community")
                .timeout(10000)
                .active(true).build();

        final String digitalExchangeId = digitalExchangeTestApi.createDigitalExchange(digitalExchange);
        final String rateResponse =
                "{ \n" +
                "    \"payload\": {}, \n" +
                "    \"metadata\": {}, \n" +
                "    \"errors\": [] \n" +
                "  }";

        WireMock.reset();
        stubFor(WireMock.post(urlEqualTo("/community/api/digitalExchange/components/avatar/rate"))
                .willReturn(aResponse().withStatus(HttpStatus.METHOD_NOT_ALLOWED.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(rateResponse)));

        mockMvc.perform(post(String.format("%s/%s/%s", URL, digitalExchangeId, "avatar"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"rating\": 4 }"))
                .andDo(print()).andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("errors", hasSize(1)))
                .andExpect(jsonPath("errors[0].code").value("2"))
                .andExpect(jsonPath("errors[0].message").value("digitalExchange.rating.notSupported"));
    }

}
