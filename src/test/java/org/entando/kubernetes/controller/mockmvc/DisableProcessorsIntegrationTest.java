package org.entando.kubernetes.controller.mockmvc;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.util.function.Supplier;
import org.entando.kubernetes.DatabaseCleaner;
import org.entando.kubernetes.EntandoKubernetesJavaApplication;
import org.entando.kubernetes.client.K8SServiceClientTestDouble;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.config.TestAppConfiguration;
import org.entando.kubernetes.config.TestKubernetesConfig;
import org.entando.kubernetes.config.TestSecurityConfiguration;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloader;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderFactory;
import org.entando.kubernetes.model.common.EntandoDeploymentPhase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@AutoConfigureWireMock(port = 8098)
@AutoConfigureMockMvc
@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        classes = {
                EntandoKubernetesJavaApplication.class,
                TestSecurityConfiguration.class,
                TestKubernetesConfig.class,
                TestAppConfiguration.class
        },
        properties = {"entando.componentManager.processor.page.enabled=false"})
@ActiveProfiles({"test"})
@Tag("component")
@WithMockUser
public class DisableProcessorsIntegrationTest {

    private MockMvc mockMvc;
    @Autowired
    private WebApplicationContext context;
    @Autowired
    private DatabaseCleaner databaseCleaner;
    @Autowired
    private K8SServiceClient k8SServiceClient;
    @Autowired
    private BundleDownloaderFactory downloaderFactory;
    @MockBean
    private EntandoCoreClient coreClient;
    private Supplier<BundleDownloader> defaultBundleDownloaderSupplier;

    @BeforeEach
    public void setup() {
        ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger("WireMock").setLevel(Level.OFF);
        defaultBundleDownloaderSupplier = downloaderFactory.getDefaultSupplier();
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    public void cleanup() {
        WireMock.reset();
        databaseCleaner.cleanup();
        ((K8SServiceClientTestDouble) k8SServiceClient).cleanInMemoryDatabases();
        ((K8SServiceClientTestDouble) k8SServiceClient).setDeployedLinkPhase(EntandoDeploymentPhase.SUCCESSFUL);
        downloaderFactory.setDefaultSupplier(defaultBundleDownloaderSupplier);
    }
}
