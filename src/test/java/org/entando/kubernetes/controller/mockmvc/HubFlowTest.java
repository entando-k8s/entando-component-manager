package org.entando.kubernetes.controller.mockmvc;

import static org.entando.kubernetes.utils.TestInstallUtils.DEPLOY_COMPONENT_ENDPOINT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.util.function.Supplier;
import org.entando.kubernetes.DatabaseCleaner;
import org.entando.kubernetes.EntandoKubernetesJavaApplication;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.config.TestAppConfiguration;
import org.entando.kubernetes.config.TestKubernetesConfig;
import org.entando.kubernetes.config.TestSecurityConfiguration;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloader;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderFactory;
import org.entando.kubernetes.security.AuthorizationChecker;
import org.entando.kubernetes.stubhelper.BundleInfoStubHelper;
import org.entando.kubernetes.utils.TenantSecurityKeycloakMockServerJunitExt;
import org.entando.kubernetes.utils.TestInstallUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@AutoConfigureWireMock(port = 8092)
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
//Sonar doesn't pick up MockMVC assertions
@SuppressWarnings("java:S2699")
@DirtiesContext
@ExtendWith(TenantSecurityKeycloakMockServerJunitExt.class)
class HubFlowTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @Autowired
    private BundleDownloaderFactory downloaderFactory;

    @MockBean
    private K8SServiceClient k8SServiceClient;

    @Autowired
    private AuthorizationChecker authorizationChecker;

    private Supplier<BundleDownloader> defaultBundleDownloaderSupplier;

    @BeforeEach
    public void setup() {
        ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger("WireMock").setLevel(Level.OFF);
        defaultBundleDownloaderSupplier = downloaderFactory.getDefaultSupplier();
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        TestInstallUtils.injectEntandoUrlInto(authorizationChecker, 8092);
    }

    @AfterEach
    public void cleanup() {
        WireMock.reset();
        databaseCleaner.cleanup();
        downloaderFactory.setDefaultSupplier(defaultBundleDownloaderSupplier);
    }

    @Test
    void shouldSuccessfullyDeployAnEntandoDeBundle() throws Exception {
        when(k8SServiceClient.deployDeBundle(any())).thenAnswer(i -> i.getArguments()[0]);

        TestInstallUtils.stubPermissionRequestReturningSuperuser();

        mockMvc.perform(post(DEPLOY_COMPONENT_ENDPOINT.build()).contentType(APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(BundleInfoStubHelper.stubBunbleInfo()))
                        .header(HttpHeaders.AUTHORIZATION, "jwt"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("{\"payload\":{\"code\":\"something-77b2b10e\",\"title\":"
                        + "\"something\",\"description\":\"bundle description\",\"repoUrl\":\"http://www.github.com/entan"
                        + "do/mybundle.git\",\"bundleType\":\"standard-bundle\",\"thumbnail\":\"data:image/png;base64,"
                        + "iVBORw0KGgoAAAANSUhEUgAAARkAAAEZCAYAAACjEFEXAAAPLElEQVR4nOzdD2yc5X3A8V\",\"componentTypes\""
                        + ":[\"widget\",\"contentTemplate\",\"pageTemplate\",\"language\",\"label\",\"content\",\"frag"
                        + "ment\",\"plugin\",\"page\",\"category\",\"asset\",\"bundle\",\"contentType\",\"group\"],\"i"
                        + "nstalledJob\":null,\"lastJob\":null,\"customInstallation\":null,\"latestVersion\":{\"versio"
                        + "n\":\"v1.2.0\"},\"versions\":[{\"version\":\"v1.0.0\"},{\"version\":\"v1.1.0\"},{\"version\""
                        + ":\"v1.2.0\"}],\"installed\":false,\"repoUrlAsURL\":\"http://www.github.com/entando/mybundle."
                        + "git\"},\"metaData\":null,\"errors\":[]}"));

        verify(k8SServiceClient, times(1)).deployDeBundle(any());
    }
}
