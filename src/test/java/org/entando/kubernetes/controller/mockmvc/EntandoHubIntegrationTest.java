package org.entando.kubernetes.controller.mockmvc;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.entando.kubernetes.utils.EntandoHubMockServer.BUNDLEGROUP_RESPONSE_JSON;
import static org.entando.kubernetes.utils.EntandoHubMockServer.BUNDLE_RESPONSE_JSON;
import static org.hamcrest.Matchers.is;
import static org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.SneakyThrows;
import org.entando.kubernetes.model.entandohub.EntandoHubRegistryEntity;
import org.entando.kubernetes.repository.EntandoHubRegistryRepository;
import org.entando.kubernetes.service.digitalexchange.entandohub.EntandoHubRegistryService;
import org.entando.kubernetes.stubhelper.EntandoHubRegistryStubHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@AutoConfigureWireMock(port = 7762)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles({"test"})
@Tag("component")
@WithMockUser
public class EntandoHubIntegrationTest {

    @Autowired
    private WebApplicationContext context;
    private MockMvc mockMvc;

    @Autowired
    private EntandoHubRegistryRepository registryRepository;
    @Autowired
    private EntandoHubRegistryService registryService;

    private final List<EntandoHubRegistryEntity> entityToSaveList = Collections.singletonList(getRegistryEntityForTesting());

    @BeforeEach
    public void setup() throws Exception {
        try {
            // db stuff
            registryRepository.saveAll(entityToSaveList);
            // wiremock stuff
            stubFor(get(urlMatching("/appbuilder/api/bundlegroups/.*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", HAL_JSON_VALUE)
                            .withBody(BUNDLEGROUP_RESPONSE_JSON)));
            stubFor(get(urlMatching("/appbuilder/api/bundles/.*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", HAL_JSON_VALUE)
                            .withBody(BUNDLE_RESPONSE_JSON)));
            // mock mvc stuff
            mockMvc = MockMvcBuilders
                    .webAppContextSetup(context)
                    .apply(springSecurity())
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @AfterEach
    public void tearDown() {
        registryRepository.deleteAll();
    }

    @Test
    void testBundleGroupController() {
        final String DEFAULT_REGISTRY_ID = getDefaultRegistryId();
        try {
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/hub/bundlegroups/" + DEFAULT_REGISTRY_ID
                                    + "/?page=1&descriptorVersions=v5&descriptorVersions=v1&pageSize=1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.payload[0].bundleGroupId", is(1)))
                    .andExpect(jsonPath("$.payload[0].bundleGroupVersionId", is(4)))
                    .andExpect(jsonPath("$.payload[0].name", is("Bundle")))
                    .andExpect(jsonPath("$.payload[0].description", is("Description for version #2")))
                    .andExpect(jsonPath("$.payload[0].documentationUrl", is("http://docm.me")))
                    .andExpect(jsonPath("$.payload[0].version", is("v0.0.2")))
                    .andExpect(jsonPath("$.payload[0].organisationId", is(1)))
                    .andExpect(jsonPath("$.payload[0].publicCatalog", is(Boolean.TRUE)))
                    .andExpect(jsonPath("$.payload[0].bundleGroupUrl",
                            is("http://localhost:3000/#/bundlegroup/versions/4")))
                    .andExpect(jsonPath("$.payload[0].status", is("PUBLISHED")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testBundleController() {
        final String DEFAULT_REGISTRY_ID = getDefaultRegistryId();
        try {
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/hub/bundles/" + DEFAULT_REGISTRY_ID
                                    + "/?page=1&descriptorVersions=v5&descriptorVersions=v1&pageSize=1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.payload[0].bundleId", is("13")))
                    .andExpect(jsonPath("$.payload[0].name", is("bundle-uri-1")))
                    .andExpect(jsonPath("$.payload[0].description", is("Description default")))
                    .andExpect(jsonPath("$.payload[0].descriptorVersion", is("V1")))
                    .andExpect(
                            jsonPath("$.payload[0].gitRepoAddress", is("https://github.com/account/bundle-uri-1.git")))
                    .andExpect(jsonPath("$.payload[0].gitSrcRepoAddress",
                            is("https://github.com/account/source-url-1.git")))
                    .andExpect(jsonPath("$.payload[0].bundleGroups[0]", is("15")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected String getDefaultRegistryId() {
        return registryService
                .listRegistries()
                .get(0)
                .getId();
    }

    @SneakyThrows
    protected EntandoHubRegistryEntity getRegistryEntityForTesting() {
        return EntandoHubRegistryStubHelper
                .stubEntandoHubRegistryEntity2()
                .setUrl(new URL("http://localhost:7762"));
    }
}
