package org.entando.kubernetes.controller.hub;

import org.entando.kubernetes.EntandoKubernetesJavaApplication;
import org.entando.kubernetes.client.hub.HubClientService;
import org.entando.kubernetes.config.TestKubernetesConfig;
import org.entando.kubernetes.config.TestSecurityConfiguration;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @SpyBean
    private HubClientService clientService;
    @BeforeEach
    public void setup() {
        Assert.assertNotNull(context);
        Assert.assertNotNull(context.getBean("entandoHubController"));
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    public void shouldStart() {
        assertThat(mockMvc).isNotNull();

    }

    @Test
    public void testBundleGroupController() {
        try {
            mockMvc.perform(MockMvcRequestBuilders
                            .get("/hub/bundlegroups/?host=http%3A%2F%2Flocalhost%3A8080&page=1&descriptorVersions=v5&descriptorVersions=v1&pageSize=1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                            .string(org.hamcrest.Matchers.containsString("Hello, World")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
