package org.entando.kubernetes.controller.mockmvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.entando.kubernetes.utils.TestInstallUtils.INSTALL_PLANS_ENDPOINT;
import static org.entando.kubernetes.utils.TestInstallUtils.getTestBundleV5;
import static org.entando.kubernetes.utils.TestInstallUtils.mockAnalysisReportV5;
import static org.entando.kubernetes.utils.TestInstallUtils.mockBundle;
import static org.entando.kubernetes.utils.TestInstallUtils.verifyJobHasComponentAndStatus;
import static org.entando.kubernetes.utils.TestInstallUtils.verifyJobHasComponentAndStatusV5;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.entando.kubernetes.DatabaseCleaner;
import org.entando.kubernetes.EntandoKubernetesJavaApplication;
import org.entando.kubernetes.assertionhelper.InstallFlowAssertionHelper;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.config.TestAppConfiguration;
import org.entando.kubernetes.config.TestKubernetesConfig;
import org.entando.kubernetes.config.TestSecurityConfiguration;
import org.entando.kubernetes.controller.digitalexchange.job.model.ComponentInstallPlan;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloader;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderFactory;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.model.job.PluginDataEntity;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.repository.PluginDataRepository;
import org.entando.kubernetes.utils.TestInstallUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@AutoConfigureWireMock(port = 8091)
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
class InstallFlowTestBundleVFive {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @Autowired
    private EntandoBundleComponentJobRepository componentJobRepository;

    @Autowired
    private EntandoBundleJobRepository jobRepository;

    @Autowired
    private PluginDataRepository pluginDataRepository;

    @Autowired
    private BundleDownloaderFactory downloaderFactory;

    @MockBean
    private K8SServiceClient k8SServiceClient;

    @MockBean
    private EntandoCoreClient coreClient;

    private InstallFlowAssertionHelper installFlowAssertionHelper;

    private Supplier<BundleDownloader> defaultBundleDownloaderSupplier;

    @BeforeEach
    public void setup() {
        ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger("WireMock").setLevel(Level.OFF);
        defaultBundleDownloaderSupplier = downloaderFactory.getDefaultSupplier();
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        installFlowAssertionHelper =
                new InstallFlowAssertionHelper(k8SServiceClient, coreClient, jobRepository, componentJobRepository);

        PluginDataEntity pluginData = new PluginDataEntity()
                .setBundleId("abcdefgh")
                .setPluginId("a1b2c3d4")
                .setPluginName("ms1")
                .setPluginCode("pn-abcdefgh-a1b2c3d4-ms1")
                .setEndpoint("my-path");
        pluginDataRepository.save(pluginData);
    }

    @AfterEach
    public void cleanup() {
        WireMock.reset();
        databaseCleaner.cleanup();
        downloaderFactory.setDefaultSupplier(defaultBundleDownloaderSupplier);
    }


    @Test
    void shouldCallCoreToInstallComponents() {
        simulateSuccessfullyCompletedInstall();
        installFlowAssertionHelper.verifyCoreCallsV5();
    }

    @Test
    void shouldCallCoreToInstallComponentsWithInstallPlan() {
        simulateSuccessfullyCompletedInstallWithInstallPlan();
        installFlowAssertionHelper.verifyCoreCallsV5();
    }

    @Test
    void shouldCallCoreToInstallComponentsWithInstallPlanRequest() throws JsonProcessingException {
        simulateSuccessfullyCompletedInstallWithInstallPlanAndInstallPlanRequest();

        installFlowAssertionHelper.verifyPluginInstallRequestsWithInstallPlanRequestV5(k8SServiceClient);
        installFlowAssertionHelper.verifyWidgetsInstallRequestsV5(coreClient);
        installFlowAssertionHelper.verifyDirectoryInstallRequestsWithInstallPlanRequest(coreClient);
        installFlowAssertionHelper.verifyFileInstallRequestsWithInstallPlanRequestV5(coreClient);

        // check that db install_plan column is correctly populated
        List<EntandoBundleJobEntity> bundleJobEntityList = jobRepository.findAll();
        assertThat(bundleJobEntityList).hasSize(1);
        String stringInstallPlan = new ObjectMapper().writeValueAsString(
                TestInstallUtils.mockInstallWithPlansRequestWithActionsV5());
        EntandoBundleJobEntity entandoBundleJobEntity = bundleJobEntityList.get(0);
        assertThat(entandoBundleJobEntity.getInstallPlan()).isEqualTo(stringInstallPlan);

        // check that db custom_install column is correctly populated
        assertThat(entandoBundleJobEntity.getCustomInstallation()).isTrue();
    }


    @Test
    void shouldCallCoreToUninstallComponents() throws Exception {
        String jobId = simulateSuccessfullyCompletedInstall();

        verifyJobHasComponentAndStatusV5(mockMvc, jobId, JobStatus.INSTALL_COMPLETED);

        final String uninstallJobId = simulateSuccessfullyCompletedUninstall();

        ArgumentCaptor<String> ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(1)).deleteWidget(ac.capture());
        assertThat(ac.getValue()).isEqualTo("todomvc_widget-ece8f6f0");

        ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(0)).deletePageModel(ac.capture());

        ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(0)).disableLanguage(ac.capture());

        ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(0)).deleteLabel(ac.capture());

        ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(1)).deleteFolder(ac.capture());
        assertThat(ac.getValue()).isEqualTo("bundles/something-ece8f6f0");

        ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(0)).deleteFragment(ac.capture());

        ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(0)).deleteContentType(ac.capture());

        ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(0)).deleteContent(ac.capture());

        ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(0)).deleteAsset(ac.capture());

        ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(0)).deleteContentType(ac.capture());

        ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(0)).setPageStatus(ac.capture(), ac.capture());

        ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(0)).deletePage(ac.capture());

        verify(k8SServiceClient, times(2)).unlinkAndScaleDown(any());

        verifyJobHasComponentAndStatus(mockMvc, TestInstallUtils.MOCK_BUNDLE_NAME_V5, uninstallJobId, JobStatus.UNINSTALL_COMPLETED);
    }


    @Test
    void shouldReturnAValidInstallPlan() throws Exception {
        mockAnalysisReportV5(coreClient, k8SServiceClient);
        mockBundle(k8SServiceClient, getTestBundleV5());

        InstallPlan expected = TestInstallUtils.mockInstallPlanV5();
        MvcResult response = mockMvc.perform(post(INSTALL_PLANS_ENDPOINT.build()))
                .andExpect(status().isOk())
                .andReturn();

        Configuration conf = Configuration.builder()
                .jsonProvider(new JacksonJsonProvider(new ObjectMapper()))
                .build();

        InstallPlan result = JsonPath.using(conf).parse(response.getResponse().getContentAsString())
                .read("$.payload", InstallPlan.class);

        assertOnInstallPlanComponents(result.getPlugins(), expected.getPlugins());
        assertOnInstallPlanComponents(result.getResources(), expected.getResources());
        assertOnInstallPlanComponents(result.getWidgets(), expected.getWidgets());
    }


    private void assertOnInstallPlanComponents(Map<String, ComponentInstallPlan> current,
            Map<String, ComponentInstallPlan> expected) {

        assertThat(current).hasSameSizeAs(expected);
        assertThat(current).containsAllEntriesOf(expected);
    }

    private String simulateSuccessfullyCompletedInstall() {
        return TestInstallUtils
                .simulateSuccessfullyCompletedInstallV5(mockMvc, coreClient, k8SServiceClient,
                        TestInstallUtils.MOCK_BUNDLE_V5_NAME_TGZ);
    }

    private String simulateSuccessfullyCompletedInstallWithInstallPlan() {
        return TestInstallUtils
                .simulateSuccessfullyCompletedInstallWithInstallPlanV5(mockMvc, coreClient, k8SServiceClient,
                        TestInstallUtils.MOCK_BUNDLE_V5_NAME_TGZ);
    }

    private String simulateSuccessfullyCompletedInstallWithInstallPlanAndInstallPlanRequest() {
        return TestInstallUtils
                .simulateSuccessfullyCompletedInstallWithInstallPlanAndInstallPlanRequestV5(mockMvc, coreClient,
                        k8SServiceClient,
                        TestInstallUtils.MOCK_BUNDLE_V5_NAME_TGZ);
    }

    private String simulateSuccessfullyCompletedUninstall() {
        return TestInstallUtils.simulateSuccessfullyCompletedUninstallV5(mockMvc, coreClient);
    }
}
