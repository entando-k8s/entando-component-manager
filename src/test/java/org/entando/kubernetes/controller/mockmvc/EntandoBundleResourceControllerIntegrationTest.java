package org.entando.kubernetes.controller.mockmvc;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.MimeType;
import org.entando.kubernetes.EntandoKubernetesJavaApplication;
import org.entando.kubernetes.TestEntitiesGenerator;
import org.entando.kubernetes.config.TestAppConfiguration;
import org.entando.kubernetes.config.TestKubernetesConfig;
import org.entando.kubernetes.config.TestSecurityConfiguration;
import org.entando.kubernetes.model.bundle.BundleType;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleDetails;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@AutoConfigureWireMock(port = 8100)
@AutoConfigureMockMvc
@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        classes = {
                EntandoKubernetesJavaApplication.class,
                TestSecurityConfiguration.class,
                TestKubernetesConfig.class,
                TestAppConfiguration.class
        })
@ActiveProfiles({"test"})
@Tag("component")
@WithMockUser
class EntandoBundleResourceControllerIntegrationTest {

    private final String componentsUrl = "/components";

    private MockMvc mockMvc;
    private ObjectMapper mapper;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    public void setup() {
        ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger("WireMock").setLevel(Level.OFF);
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        mapper = new ObjectMapper();
    }

    @AfterEach
    public void cleanup() {
        WireMock.reset();
    }

    @Test
    void shouldCorrectlyDeployAnEntandoDeBundle() throws Exception {

        final EntandoDeBundle deBundle = TestEntitiesGenerator.getTestBundle();
        final EntandoDeBundleDetails details = deBundle.getSpec().getDetails();

        String payload = mapper.writeValueAsString(deBundle);
        mockMvc.perform(post(componentsUrl)
                .content(payload)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.code", is(deBundle.getMetadata().getName())))
                .andExpect(jsonPath("$.payload.title", is(details.getName())))
                .andExpect(jsonPath("$.payload.description", is(details.getDescription())))
                .andExpect(jsonPath("$.payload.bundleType", is(BundleType.STANDARD_BUNDLE.getType())))
                .andExpect(jsonPath("$.payload.thumbnail", IsNull.nullValue()))
                .andExpect(jsonPath("$.payload.componentTypes", hasSize(1)))
                .andExpect(jsonPath("$.payload.componentTypes.[0]", is("bundle")))
                .andExpect(jsonPath("$.payload.installedJob", IsNull.nullValue()))
                .andExpect(jsonPath("$.payload.lastJob", IsNull.nullValue()))
                .andExpect(jsonPath("$.payload.customInstallation", IsNull.nullValue()))
                .andExpect(jsonPath("$.payload.latestVersion.version", is("0.0.15")));
    }

}
