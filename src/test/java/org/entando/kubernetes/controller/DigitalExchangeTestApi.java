package org.entando.kubernetes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.entando.kubernetes.service.digitalexchange.model.DigitalExchange;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static com.jayway.jsonpath.JsonPath.parse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Component
@RequiredArgsConstructor
public class DigitalExchangeTestApi {

    static final String EXCHANGES_URL = "/exchanges";

    private final ObjectMapper objectMapper;
    private final MockMvc mockMvc;

    String createDigitalExchange(final DigitalExchange digitalExchange) throws Exception {
        final ResultActions resultActions = mockMvc.perform(post(EXCHANGES_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(digitalExchange)))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("payload.id").exists())
                .andExpect(jsonPath("payload.name").value(digitalExchange.getName()))
                .andExpect(jsonPath("payload.url").value(digitalExchange.getUrl()))
                .andExpect(jsonPath("payload.timeout").value(digitalExchange.getTimeout()))
                .andExpect(jsonPath("payload.active").value(digitalExchange.isActive()));

        if (digitalExchange.getClientId() != null) {
            resultActions.andExpect(jsonPath("payload.clientId").value(digitalExchange.getClientId()));
        } else {
            resultActions.andExpect(jsonPath("payload.clientId").doesNotExist());
        }
        if (digitalExchange.getClientSecret() != null) {
            resultActions.andExpect(jsonPath("payload.clientSecret").value(digitalExchange.getClientSecret()));
        } else {
            resultActions.andExpect(jsonPath("payload.clientSecret").doesNotExist());
        }
        if (digitalExchange.getPublicKey() != null) {
            resultActions.andExpect(jsonPath("payload.publicKey").value(digitalExchange.getPublicKey()));
        } else {
            resultActions.andExpect(jsonPath("payload.publicKey").doesNotExist());
        }

        final String jsonResponse = resultActions.andReturn().getResponse().getContentAsString();
        return parse(jsonResponse).read("$.payload.id").toString();
    }
}
