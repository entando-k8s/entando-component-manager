package org.entando.kubernetes.controller.mockmvc;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.entando.kubernetes.DigitalExchangeTestUtils.readFile;
import static org.entando.kubernetes.DigitalExchangeTestUtils.readFileAsBase64;
import static org.entando.kubernetes.utils.TestInstallUtils.ALL_COMPONENTS_ENDPOINT;
import static org.entando.kubernetes.utils.TestInstallUtils.INSTALL_COMPONENT_ENDPOINT;
import static org.entando.kubernetes.utils.TestInstallUtils.UNINSTALL_COMPONENT_ENDPOINT;
import static org.entando.kubernetes.utils.TestInstallUtils.getJobStatus;
import static org.entando.kubernetes.utils.TestInstallUtils.getTestBundle;
import static org.entando.kubernetes.utils.TestInstallUtils.readFromDEPackage;
import static org.entando.kubernetes.utils.TestInstallUtils.simulateBundleDownloadError;
import static org.entando.kubernetes.utils.TestInstallUtils.simulateFailingInstall;
import static org.entando.kubernetes.utils.TestInstallUtils.simulateFailingUninstall;
import static org.entando.kubernetes.utils.TestInstallUtils.simulateInProgressInstall;
import static org.entando.kubernetes.utils.TestInstallUtils.simulateInProgressUninstall;
import static org.entando.kubernetes.utils.TestInstallUtils.simulateSuccessfullyCompletedInstall;
import static org.entando.kubernetes.utils.TestInstallUtils.simulateSuccessfullyCompletedUninstall;
import static org.entando.kubernetes.utils.TestInstallUtils.verifyJobHasComponentAndStatus;
import static org.entando.kubernetes.utils.TestInstallUtils.waitForInstallStatus;
import static org.entando.kubernetes.utils.TestInstallUtils.waitForJobStatus;
import static org.entando.kubernetes.utils.TestInstallUtils.waitForUninstallStatus;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.jayway.jsonpath.JsonPath;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.entando.kubernetes.DatabaseCleaner;
import org.entando.kubernetes.EntandoKubernetesJavaApplication;
import org.entando.kubernetes.client.K8SServiceClientTestDouble;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.config.TestAppConfiguration;
import org.entando.kubernetes.config.TestKubernetesConfig;
import org.entando.kubernetes.config.TestSecurityConfiguration;
import org.entando.kubernetes.model.EntandoDeploymentPhase;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FragmentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageTemplateDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloader;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderFactory;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.EntandoBundle;
import org.entando.kubernetes.model.job.EntandoBundleComponentJob;
import org.entando.kubernetes.model.job.EntandoBundleJob;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.model.job.JobType;
import org.entando.kubernetes.model.link.EntandoAppPluginLink;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@AutoConfigureWireMock(port = 8099)
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

    private static final String MOCK_BUNDLE_NAME = "bundle.tgz";

    @BeforeEach
    public void setup() {
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

    @Test
    public void shouldSkipPagesWhenPageProcessorIsDisabled() {
        simulateSuccessfullyCompletedInstall(mockMvc, coreClient, k8SServiceClient, MOCK_BUNDLE_NAME);

        K8SServiceClientTestDouble k8SServiceClientTestDouble = (K8SServiceClientTestDouble) k8SServiceClient;
        // Verify interaction with mocks
        Set<EntandoAppPluginLink> createdLinks = k8SServiceClientTestDouble.getInMemoryLinks();
        Optional<EntandoAppPluginLink> appPluginLinkForTodoMvc = createdLinks.stream()
                .filter(link -> link.getSpec().getEntandoPluginName().equals("todomvc")).findAny();

        verify(coreClient, times(0)).registerPage(any());
    }
}
