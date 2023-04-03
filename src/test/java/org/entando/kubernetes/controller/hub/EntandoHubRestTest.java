package org.entando.kubernetes.controller.hub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.entando.kubernetes.EntandoKubernetesJavaApplication;
import org.entando.kubernetes.config.TestKubernetesConfig;
import org.entando.kubernetes.config.TestSecurityConfiguration;
import org.entando.kubernetes.service.HubService;
import org.entando.kubernetes.stubhelper.HubStubHelper;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {EntandoKubernetesJavaApplication.class, TestSecurityConfiguration.class, TestKubernetesConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles({"test"})
@Tag("component")
@WithMockUser
public class EntandoHubRestTest {

    @Autowired
    private WebApplicationContext context;
    private MockMvc mockMvc;

    @MockBean
    private HubService hubService;


    @BeforeEach
    public void setup() {
        Assert.assertNotNull(context);
        Assert.assertNotNull(context.getBean("entandoHubController"));
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        when(hubService.searchBundleGroupVersions(anyString(), any()))
                .thenReturn(HubStubHelper.stubBundleGroupVersionsProxiedPayload());
        when(hubService.getBundles(anyString(), any()))
                .thenReturn(HubStubHelper.stubBundleDtosProxiedPayload());
    }

    @Test
    void shouldStart() {
        assertThat(mockMvc).isNotNull();
    }

    @Test
    void testBundleGroupController() {
        try {
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/hub/bundlegroups/123-abop-4560/?page=1&descriptorVersions=v5&descriptorVersions=v1&pageSize=1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.payload[0].bundleGroupId", is(10)))
                    .andExpect(jsonPath("$.payload[0].bundleGroupVersionId", is(100)))
                    .andExpect(jsonPath("$.payload[0].name", is("entando bundle")))
                    .andExpect(jsonPath("$.payload[0].description", is("my descr")))
                    .andExpect(jsonPath("$.payload[0].documentationUrl", is("http://www.entando.com")))
                    .andExpect(jsonPath("$.payload[0].version", is("v1.0.0")))
                    .andExpect(jsonPath("$.payload[0].organisationId", is(5)))
                    .andExpect(jsonPath("$.payload[0].publicCatalog", is(Boolean.TRUE)))
                    .andExpect(jsonPath("$.payload[0].bundleGroupUrl", is("http://www.mybundle.com")))
                    .andExpect(jsonPath("$.payload[0].status", is("PUBLISHED")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testBundleController() {
        try {
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/hub/bundles/123-abop-4560/?page=1&descriptorVersions=v5&descriptorVersions=v1&pageSize=1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.payload[0].bundleId", is("2677")))
                    .andExpect(jsonPath("$.payload[0].name", is("Yet another bundle")))
                    .andExpect(jsonPath("$.payload[0].description", is("my descr")))
                    .andExpect(jsonPath("$.payload[0].descriptionImage", is("wonderful image")))
                    .andExpect(jsonPath("$.payload[0].descriptorVersion", is("V5")))
                    .andExpect(jsonPath("$.payload[0].gitRepoAddress", is("http://www.github.com/entando/test")))
                    .andExpect(jsonPath("$.payload[0].gitSrcRepoAddress", is("http://www.github.com/entando/srctest")))
                    .andExpect(jsonPath("$.payload[0].dependencies[0]", is("my")))
                    .andExpect(jsonPath("$.payload[0].bundleGroups[0]", is("10")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
