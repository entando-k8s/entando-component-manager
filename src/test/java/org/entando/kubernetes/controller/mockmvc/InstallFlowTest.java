package org.entando.kubernetes.controller.mockmvc;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.entando.kubernetes.model.common.EntandoDeploymentPhase.FAILED;
import static org.entando.kubernetes.utils.TestInstallUtils.ALL_COMPONENTS_ENDPOINT;
import static org.entando.kubernetes.utils.TestInstallUtils.INSTALL_COMPONENT_ENDPOINT;
import static org.entando.kubernetes.utils.TestInstallUtils.INSTALL_PLANS_ENDPOINT;
import static org.entando.kubernetes.utils.TestInstallUtils.MOCK_BUNDLE_NAME;
import static org.entando.kubernetes.utils.TestInstallUtils.UNINSTALL_COMPONENT_ENDPOINT;
import static org.entando.kubernetes.utils.TestInstallUtils.getJobStatus;
import static org.entando.kubernetes.utils.TestInstallUtils.mockAnalysisReportV1;
import static org.entando.kubernetes.utils.TestInstallUtils.mockBundle;
import static org.entando.kubernetes.utils.TestInstallUtils.mockPlugins;
import static org.entando.kubernetes.utils.TestInstallUtils.readFromDEPackage;
import static org.entando.kubernetes.utils.TestInstallUtils.verifyJobHasComponentAndStatus;
import static org.entando.kubernetes.utils.TestInstallUtils.waitForInstallStatus;
import static org.entando.kubernetes.utils.TestInstallUtils.waitForJobStatus;
import static org.entando.kubernetes.utils.TestInstallUtils.waitForUninstallStatus;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.entando.kubernetes.DatabaseCleaner;
import org.entando.kubernetes.EntandoKubernetesJavaApplication;
import org.entando.kubernetes.assertionhelper.InstallFlowAssertionHelper;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.client.model.EntandoCoreComponentDeleteRequest;
import org.entando.kubernetes.config.TestAppConfiguration;
import org.entando.kubernetes.config.TestKubernetesConfig;
import org.entando.kubernetes.config.TestSecurityConfiguration;
import org.entando.kubernetes.config.tenant.TenantFilter;
import org.entando.kubernetes.config.tenant.TestTenantConfig;
import org.entando.kubernetes.controller.digitalexchange.job.model.ComponentInstallPlan;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloader;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderFactory;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.model.job.EntandoBundleEntity;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.model.job.JobType;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
import org.entando.kubernetes.service.digitalexchange.concurrency.BundleOperationsConcurrencyManager;
import org.entando.kubernetes.service.digitalexchange.crane.CraneCommand;
import org.entando.kubernetes.stubhelper.PluginStubHelper;
import org.entando.kubernetes.utils.TenantContextJunitExt;
import org.entando.kubernetes.utils.TestInstallUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;

@AutoConfigureWireMock(port = 8099)
@AutoConfigureMockMvc
@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        classes = {
                EntandoKubernetesJavaApplication.class,
                TestSecurityConfiguration.class,
                TestKubernetesConfig.class,
                TestAppConfiguration.class,
                TenantContextJunitExt.class,
                TestTenantConfig.class
        })
@ActiveProfiles({"test"})
@Tag("component")
@WithMockUser
//Sonar doesn't pick up MockMVC assertions
@SuppressWarnings("java:S2699")
@DirtiesContext
public class InstallFlowTest {

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
    private InstalledEntandoBundleRepository installedCompRepo;

    @Autowired
    private Map<ComponentType, ComponentProcessor> processorMap;

    @Autowired
    private BundleDownloaderFactory downloaderFactory;

    @Autowired
    private TenantFilter tenantFilter;

    @MockBean
    private K8SServiceClient k8SServiceClient;

    @MockBean
    private EntandoCoreClient coreClient;

    @MockBean
    private BundleOperationsConcurrencyManager bundleOperationsConcurrencyManager;

    @MockBean
    private CraneCommand craneCommand;

    private InstallFlowAssertionHelper installFlowAssertionHelper;

    private Supplier<BundleDownloader> defaultBundleDownloaderSupplier;

    @BeforeEach
    public void setup() {
        ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger("WireMock").setLevel(Level.OFF);
        defaultBundleDownloaderSupplier = downloaderFactory.getDefaultSupplier();
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .addFilters(tenantFilter)
                .apply(springSecurity())
                .build();

        installFlowAssertionHelper =
                new InstallFlowAssertionHelper(k8SServiceClient, coreClient, jobRepository, componentJobRepository);

        when(craneCommand.getImageDigest(anyString())).thenReturn(PluginStubHelper.PLUGIN_IMAGE_SHA);
    }

    @AfterEach
    public void cleanup() {
        WireMock.reset();
        databaseCleaner.cleanup();
        downloaderFactory.setDefaultSupplier(defaultBundleDownloaderSupplier);
    }

    @Test
    public void shouldReturnNotFoundWhenBundleDoesntExists() throws Exception {
        TestInstallUtils.stubPermissionRequestReturningSuperuser();

        mockMvc.perform(post(INSTALL_COMPONENT_ENDPOINT.build())
                        .header(HttpHeaders.AUTHORIZATION, "jwt"))
                .andDo(print()).andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnNotFoundWhenBundleDoesntExistsAndInstallWithPlanIsRequested() throws Exception {

        TestInstallUtils.stubPermissionRequestReturningSuperuser();

        mockMvc.perform(put(INSTALL_PLANS_ENDPOINT.build())
                        .header(HttpHeaders.AUTHORIZATION, "jwt"))
                .andDo(print()).andExpect(status().isNotFound());
    }

    @Test
    void shouldCallCoreToInstallComponents() throws Exception {
        simulateSuccessfullyCompletedInstall();
        installFlowAssertionHelper.verifyCoreCalls();
    }

    @Test
    void shouldCallCoreToInstallComponentsWithInstallPlan() throws Exception {
        simulateSuccessfullyCompletedInstallWithInstallPlan();
        installFlowAssertionHelper.verifyCoreCalls();
    }


    @Test
    void shouldCallCoreToInstallComponentsWithInstallPlanRequest() throws JsonProcessingException {
        simulateSuccessfullyCompletedInstallWithInstallPlanAndInstallPlanRequest();

        installFlowAssertionHelper.verifyPluginInstallRequestsWithInstallPlanRequest(k8SServiceClient);
        installFlowAssertionHelper.verifyWidgetsInstallRequestsWithInstallPlanRequest(coreClient);
        installFlowAssertionHelper.verifyCategoryInstallRequestsWithInstallPlanRequest(coreClient);
        installFlowAssertionHelper.verifyGroupInstallRequestsWithInstallPlanRequest(coreClient);
        installFlowAssertionHelper.verifyPageModelsInstallRequestsWithInstallPlanRequest(coreClient);
        installFlowAssertionHelper.verifyLanguagesInstallRequestsWithInstallPlanRequest(coreClient);
        installFlowAssertionHelper.verifyLabelsInstallRequestsWithInstallPlanRequest(coreClient);
        installFlowAssertionHelper.verifyDirectoryInstallRequestsWithInstallPlanRequest(coreClient);
        installFlowAssertionHelper.verifyFileInstallRequestsWithInstallPlanRequestV1(coreClient);
        installFlowAssertionHelper.verifyFragmentInstallRequestsWithInstallPlanRequest(coreClient);
        installFlowAssertionHelper.verifyPageInstallRequestsWithInstallPlanRequest(coreClient);
        installFlowAssertionHelper.verifyPageConfigurationInstallRequestsWithInstallPlanRequest(coreClient);
        // 2 times for PageInstallable and 2 times for PageConfigurationInstallable
        verify(coreClient, times(4)).setPageStatus(anyString(), anyString());
        installFlowAssertionHelper.verifyContentTypesInstallRequestsWithInstallPlanRequest(coreClient);
        installFlowAssertionHelper.verifyContentTemplatesInstallRequestsWithInstallPlanRequest(coreClient);
        installFlowAssertionHelper.verifyContentsInstallRequestsWithInstallPlanRequest(coreClient);
        installFlowAssertionHelper.verifyAssetsInstallRequestsWithInstallPlanRequest(coreClient);

        // check that db install_plan column is correctly populated
        List<EntandoBundleJobEntity> bundleJobEntityList = jobRepository.findAll();
        assertThat(bundleJobEntityList).hasSize(1);
        String stringInstallPlan = new ObjectMapper().writeValueAsString(
                TestInstallUtils.mockInstallWithPlansRequestWithActionsV1());
        EntandoBundleJobEntity entandoBundleJobEntity = bundleJobEntityList.get(0);
        assertThat(entandoBundleJobEntity.getInstallPlan()).isEqualTo(stringInstallPlan);

        // check that db custom_install column is correctly populated
        assertThat(entandoBundleJobEntity.getCustomInstallation()).isTrue();
    }


    @Test
    void shouldRecordJobStatusAndComponentsForAuditingWhenInstallComponents() {
        simulateSuccessfullyCompletedInstall();
        installFlowAssertionHelper.verifyAfterShouldRecordJobStatusAndComponentsForAuditingWhenInstallComponentsV1();
    }

    @Test
    void shouldRecordJobStatusAndComponentsForAuditingWhenInstallComponentsWithInstallPlan() {
        simulateSuccessfullyCompletedInstallWithInstallPlan();
        installFlowAssertionHelper.verifyAfterShouldRecordJobStatusAndComponentsForAuditingWhenInstallComponentsV1();
    }


    @Test
    void shouldRecordInstallJobsInOrderWithInstall() {
        simulateSuccessfullyCompletedInstall();
        verifyAfterShouldRecordInstallJobsInOrder();
    }

    @Test
    void shouldRecordInstallJobsInOrderWithInstallPlan() {
        simulateSuccessfullyCompletedInstallWithInstallPlan();
        verifyAfterShouldRecordInstallJobsInOrder();
    }

    private void verifyAfterShouldRecordInstallJobsInOrder() {

        List<EntandoBundleComponentJobEntity> jobs = componentJobRepository
                .findAll(Sort.by(Sort.Order.asc("startedAt")));

        for (int i = 1; i < jobs.size(); i++) {
            Installable thisInstallable = processorMap.get(jobs.get(i).getComponentType()).process(jobs.get(i));
            Installable prevInstallable = processorMap.get(jobs.get(i - 1).getComponentType()).process(jobs.get(i - 1));

            assertThat(thisInstallable.getPriority()).isGreaterThanOrEqualTo(prevInstallable.getPriority());
            assertThat(jobs.get(i).getStartedAt()).isAfterOrEqualTo(jobs.get(i - 1).getStartedAt());
        }

        String jobId = simulateSuccessfullyCompletedUninstall();
        EntandoBundleJobEntity uninstallJob = jobRepository.getOne(UUID.fromString(jobId));
        List<EntandoBundleComponentJobEntity> uninstallJobs = componentJobRepository.findAllByParentJob(uninstallJob)
                .stream()
                .sorted(Comparator.comparingLong(j -> j.getStartedAt().toInstant(ZoneOffset.UTC).toEpochMilli()))
                .collect(Collectors.toList());

        for (int i = 1; i < uninstallJobs.size(); i++) {
            Installable thisInstallable = processorMap.get(uninstallJobs.get(i).getComponentType())
                    .process(uninstallJobs.get(i));
            Installable prevInstallable = processorMap.get(uninstallJobs.get(i - 1).getComponentType())
                    .process(uninstallJobs.get(i - 1));

            assertThat(thisInstallable.getPriority()).isLessThanOrEqualTo(prevInstallable.getPriority());
            assertThat(uninstallJobs.get(i).getStartedAt()).isAfterOrEqualTo(uninstallJobs.get(i - 1).getStartedAt());
        }
    }

    @Test
    public void shouldCallCoreToUninstallComponents() throws Exception {
        String jobId = simulateSuccessfullyCompletedInstall();

        verifyJobHasComponentAndStatus(mockMvc, jobId, JobStatus.INSTALL_COMPLETED);

        final String uninstallJobId = simulateSuccessfullyCompletedUninstall();

        Class<ArrayList<EntandoCoreComponentDeleteRequest>> listClass =
                (Class<ArrayList<EntandoCoreComponentDeleteRequest>>) (Class) ArrayList.class;
        ArgumentCaptor<List<EntandoCoreComponentDeleteRequest>> ac = ArgumentCaptor.forClass(listClass);
        verify(coreClient, times(1)).deleteComponents(ac.capture());
        List<EntandoCoreComponentDeleteRequest> expectedInput = Arrays.asList(
                new EntandoCoreComponentDeleteRequest(ComponentType.WIDGET, "todomvc_widget"),
                new EntandoCoreComponentDeleteRequest(ComponentType.WIDGET, "another_todomvc_widget"),
                new EntandoCoreComponentDeleteRequest(ComponentType.PAGE_TEMPLATE,
                        "todomvc_page_model"),
                new EntandoCoreComponentDeleteRequest(ComponentType.PAGE_TEMPLATE,
                        "todomvc_another_page_model"),
                new EntandoCoreComponentDeleteRequest(ComponentType.CATEGORY, "my-category"),
                new EntandoCoreComponentDeleteRequest(ComponentType.CATEGORY, "another_category"),
                new EntandoCoreComponentDeleteRequest(ComponentType.GROUP, "ecr"),
                new EntandoCoreComponentDeleteRequest(ComponentType.GROUP, "ps"),
                new EntandoCoreComponentDeleteRequest(ComponentType.LANGUAGE, "it"),
                new EntandoCoreComponentDeleteRequest(ComponentType.LANGUAGE, "en"),
                new EntandoCoreComponentDeleteRequest(ComponentType.LABEL, "HELLO"),
                new EntandoCoreComponentDeleteRequest(ComponentType.LABEL, "WORLD"),
                new EntandoCoreComponentDeleteRequest(ComponentType.DIRECTORY, "/something"),
                new EntandoCoreComponentDeleteRequest(ComponentType.FRAGMENT, "title_fragment"),
                new EntandoCoreComponentDeleteRequest(ComponentType.FRAGMENT, "another_fragment"),
                new EntandoCoreComponentDeleteRequest(ComponentType.CONTENT_TYPE, "CNG"),
                new EntandoCoreComponentDeleteRequest(ComponentType.CONTENT_TYPE, "CNT"),
                new EntandoCoreComponentDeleteRequest(ComponentType.CONTENT, "CNG102"),
                new EntandoCoreComponentDeleteRequest(ComponentType.CONTENT, "CNT103"),
                new EntandoCoreComponentDeleteRequest(ComponentType.PAGE, "my-page"),
                new EntandoCoreComponentDeleteRequest(ComponentType.PAGE, "another-page"),
                new EntandoCoreComponentDeleteRequest(ComponentType.CONTENT_TEMPLATE, "8880002"),
                new EntandoCoreComponentDeleteRequest(ComponentType.CONTENT_TEMPLATE, "8880003"),
                new EntandoCoreComponentDeleteRequest(ComponentType.ASSET, "cc=my_asset"),
                new EntandoCoreComponentDeleteRequest(ComponentType.ASSET, "cc=anotherAsset")
        );

        assertThat(ac.getValue()).containsAll(expectedInput);
        assertEquals(ac.getValue().size(), expectedInput.size());
        verify(k8SServiceClient, times(6)).unlinkAndScaleDown(any());

        verifyJobHasComponentAndStatus(mockMvc, uninstallJobId, JobStatus.UNINSTALL_COMPLETED);
    }

    @Test
    public void shouldRecordJobStatusAndComponentsForAuditingWhenUninstallComponents() throws Exception {
        assertThat(jobRepository.findAll()).isEmpty();
        String jobId = simulateSuccessfullyCompletedInstall();

        verifyJobHasComponentAndStatus(mockMvc, jobId, JobStatus.INSTALL_COMPLETED);

        simulateSuccessfullyCompletedUninstall();
        List<EntandoBundleJobEntity> jobs = jobRepository.findAll();
        assertThat(jobs).hasSize(2);
        assertThat(jobs.get(0).getStatus()).isEqualByComparingTo(JobStatus.INSTALL_COMPLETED);
        assertThat(jobs.get(1).getStatus()).isEqualByComparingTo(JobStatus.UNINSTALL_COMPLETED);

        List<EntandoBundleComponentJobEntity> installedComponentList = componentJobRepository
                .findAllByParentJob(jobs.get(0));
        List<EntandoBundleComponentJobEntity> uninstalledComponentList = componentJobRepository
                .findAllByParentJob(jobs.get(1));
        assertThat(uninstalledComponentList).hasSize(installedComponentList.size());
        List<JobStatus> jobComponentStatus = uninstalledComponentList.stream()
                .map(EntandoBundleComponentJobEntity::getStatus)
                .collect(Collectors.toList());
        assertThat(jobComponentStatus).allMatch((jcs) -> jcs.equals(JobStatus.UNINSTALL_COMPLETED));

        boolean matchFound = false;
        for (EntandoBundleComponentJobEntity ic : installedComponentList) {
            matchFound = uninstalledComponentList.stream().anyMatch(uc -> {
                return uc.getParentJob().getId().equals(jobs.get(1).getId())
                        && uc.getComponentId().equals(ic.getComponentId())
                        && uc.getComponentType().equals(ic.getComponentType());
                //                        uc.getChecksum().equals(ic.getChecksum()); // FIXME when building descriptor for uninstall we
                //                         use only code, this changes the checksum
            });
            if (!matchFound) {
                break;
            }
        }
        assertThat(matchFound).isTrue();
    }

    @Test
    public void installedComponentShouldReturnInstalledFieldTrueAndEntryInTheInstalledComponentDatabase()
            throws Exception {

        simulateSuccessfullyCompletedInstall();

        mockMvc.perform(get(ALL_COMPONENTS_ENDPOINT.build()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").isArray())
                .andExpect(jsonPath("$.payload", hasSize(1)))
                .andExpect(jsonPath("$.payload[0].code").value(MOCK_BUNDLE_NAME))
                .andExpect(jsonPath("$.payload[0].installed").value("true"));

        List<EntandoBundleEntity> installedComponents = installedCompRepo.findAll();
        assertThat(installedComponents).hasSize(1);
        assertThat(installedComponents.get(0).getBundleCode()).isEqualTo(MOCK_BUNDLE_NAME);
        assertThat(installedComponents.get(0).getBundleType()).isEqualTo("STANDARD_BUNDLE");
        assertThat(installedComponents.get(0).isInstalled()).isEqualTo(true);
    }

    @Test
    void erroneousInstallationShouldRollback() throws Exception {
        // Given a failed install happened
        String failingJobId = simulateFailingInstall();
        verifyAfterErroneousInstallationShouldRollback(failingJobId);
    }

    @Test
    void erroneousInstallationShouldRollbackWithInstallPlan() throws Exception {
        // Given a failed install happened
        String failingJobId = simulateFailingInstallWithInstallPlan();
        verifyAfterErroneousInstallationShouldRollback(failingJobId);
    }

    private void verifyAfterErroneousInstallationShouldRollback(String failingJobId) throws Exception {

        // Install Job should have been rollback
        mockMvc.perform(get(INSTALL_PLANS_ENDPOINT.build()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.id").value(failingJobId))
                .andExpect(jsonPath("$.payload.componentId").value(MOCK_BUNDLE_NAME))
                .andExpect(jsonPath("$.payload.status").value(JobStatus.INSTALL_ROLLBACK.toString()));

        Optional<EntandoBundleJobEntity> job = jobRepository.findById(UUID.fromString(failingJobId));
        assertThat(job.isPresent()).isTrue();
        assertThat(job.get().getInstallErrorCode()).isEqualTo(100);
        assertThat(job.get().getInstallErrorMessage())
                .isEqualTo(
                        "ComponentType: page - Code: my-page --- Rest client exception (status code 500) - Internal Server Error");
        assertNull(job.get().getRollbackErrorCode());
        assertNull(job.get().getRollbackErrorMessage());

        // And for each installed component job there should be a component job that rollbacked the install
        List<EntandoBundleComponentJobEntity> jobRelatedComponents = componentJobRepository
                .findAllByParentJob(job.get());
        List<EntandoBundleComponentJobEntity> installedComponents = jobRelatedComponents.stream()
                .filter(j -> j.getStatus().equals(JobStatus.INSTALL_COMPLETED))
                .collect(Collectors.toList());

        for (EntandoBundleComponentJobEntity c : installedComponents) {
            List<EntandoBundleComponentJobEntity> jobs = jobRelatedComponents.stream()
                    .filter(j -> j.getComponentType().equals(c.getComponentType()) && j.getComponentId()
                            .equals(c.getComponentId()))
                    .collect(Collectors.toList());

            assertThat(jobs).hasSize(2);
            assertThat(jobs.stream().anyMatch(j -> j.getStatus().equals(JobStatus.INSTALL_ROLLBACK))).isTrue();
        }

        // And component should not be part of the installed components
        assertThat(installedCompRepo.findAll()).isEmpty();
    }

    @Test
    void shouldReturnAppropriateErrorCodeWhenFailingInstallDueToBigFile() throws Exception {

        // Given a failed install happened
        String failingJobId = simulateHugeAssetFailingInstall();
        verifyAfterShouldReturnAppropriateErrorCodeWhenFailingInstallDueToBigFile(failingJobId,
                INSTALL_COMPONENT_ENDPOINT);
    }

    @Test
    void shouldReturnAppropriateErrorCodeWhenFailingInstallDueToBigFileWithInstallPlan() throws Exception {

        // Given a failed install happened
        String failingJobId = simulateHugeAssetFailingInstallWithPlan();
        verifyAfterShouldReturnAppropriateErrorCodeWhenFailingInstallDueToBigFile(failingJobId, INSTALL_PLANS_ENDPOINT);
    }

    private void verifyAfterShouldReturnAppropriateErrorCodeWhenFailingInstallDueToBigFile(String failingJobId,
            UriBuilder endpointBuilder) throws Exception {
        // Install Job should have been rollback
        mockMvc.perform(get(INSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.id").value(failingJobId));

        Optional<EntandoBundleJobEntity> job = jobRepository.findById(UUID.fromString(failingJobId));
        assertThat(job.isPresent()).isTrue();

        // And for each installed component job there should be a component job that rollbacked the install
        List<EntandoBundleComponentJobEntity> jobRelatedComponents = componentJobRepository
                .findAllByParentJob(job.get());
        Optional<EntandoBundleComponentJobEntity> optErrComponent = jobRelatedComponents.stream()
                .filter(j -> j.getStatus().equals(JobStatus.INSTALL_ERROR))
                .findFirst();

        assertThat(optErrComponent.isPresent()).isTrue();
        EntandoBundleComponentJobEntity ec = optErrComponent.get();
        assertThat(ec.getInstallErrorMessage()).contains("status code 413", "Payload Too Large");

    }

    @Test
    void erroneousInstallationOfComponentShouldReturnComponentIsNotInstalled() throws Exception {
        // Given a failed install happened
        String failingJobId = simulateFailingInstall();
        verifyAfterErroneousInstallationOfComponentShouldReturnComponentIsNotInstalled(failingJobId,
                INSTALL_COMPONENT_ENDPOINT);
    }

    @Test
    void erroneousInstallationOfComponentShouldReturnComponentIsNotInstalledWithInstallPlan() throws Exception {
        // Given a failed install happened
        String failingJobId = simulateFailingInstallWithInstallPlan();
        verifyAfterErroneousInstallationOfComponentShouldReturnComponentIsNotInstalled(failingJobId,
                INSTALL_PLANS_ENDPOINT);
    }

    private void verifyAfterErroneousInstallationOfComponentShouldReturnComponentIsNotInstalled(String failingJobId,
            UriBuilder endpointUribuilder) throws Exception {

        // Components endpoints should still return the component is not installed
        mockMvc.perform(get(ALL_COMPONENTS_ENDPOINT.build()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.payload").isArray())
                .andExpect(jsonPath("$.payload", hasSize(1)))
                .andExpect(jsonPath("$.payload[0].code").value(MOCK_BUNDLE_NAME))
                .andExpect(jsonPath("$.payload[0].installed").value("false"));

        // Component install status should be rollback
        mockMvc.perform(get(INSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.id").value(failingJobId))
                .andExpect(jsonPath("$.payload.componentId").value(MOCK_BUNDLE_NAME))
                .andExpect(jsonPath("$.payload.status").value(JobStatus.INSTALL_ROLLBACK.toString()));

        // And component should not be installed
        assertThat(installedCompRepo.findAll()).isEmpty();
    }

    @Test
    public void shouldReportAllInstallationAttemptsOrderedByStartTimeDescending() throws Exception {
        // Given I installed, uninstalled and failed to reinstall a component
        String successfulInstallId = simulateSuccessfullyCompletedInstall();
        String successfulUninstallId = simulateSuccessfullyCompletedUninstall();
        String failingInstallId = simulateFailingInstall();

        // Jobs should have different ids
        assertThat(successfulInstallId).isNotEqualTo(successfulUninstallId);
        assertThat(successfulInstallId).isNotEqualTo(failingInstallId);
        assertThat(successfulUninstallId).isNotEqualTo(failingInstallId);

        // All jobs should be available via the API
        mockMvc.perform(get("/jobs?filters[0].attribute=componentId&filters[0].operator=eq&filters[0].value="
                        + MOCK_BUNDLE_NAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").isArray())
                .andExpect(jsonPath("$.payload.*.id", hasSize(3)))
                .andExpect(jsonPath("$.payload.*.id").value(containsInAnyOrder(
                        failingInstallId, successfulUninstallId, successfulInstallId
                )));
    }

    @Test
    void shouldReturnDifferentJobIdWhenAttemptingToInstallTheSameComponentTwice() throws Exception {
        // Given I try to update a component which is already installed
        String firstSuccessfulJobId = simulateSuccessfullyCompletedInstall();
        String secondSuccessfulJobId = simulateSuccessfullyCompletedUpdate();

        // two successfull jobs are created
        assertThat(firstSuccessfulJobId).isNotEqualTo(secondSuccessfulJobId);
        assertThat(jobRepository.findAll()).hasSize(2);
    }

    @Test
    void shouldReturnDifferentJobIdWhenAttemptingToInstallTheSameComponentTwiceWithInstallPlan() {
        // Given I try to update a component which is already installed
        String firstSuccessfulJobId = simulateSuccessfullyCompletedInstallWithInstallPlan();
        String secondSuccessfulJobId = simulateSuccessfullyCompletedInstallWithInstallPlan();

        // two successfull jobs are created
        assertThat(firstSuccessfulJobId).isNotEqualTo(secondSuccessfulJobId);
        assertThat(jobRepository.findAll()).hasSize(2);
    }

    @Test
    public void shouldThrowConflictWhenActingDuringInstallJobInProgress() throws Exception {
        // Given I have an in progress installatioon
        String jobId = simulateInProgressInstall();

        // I should get a conflict when trying to install or uninstall the same component
        mockMvc.perform(post(INSTALL_COMPONENT_ENDPOINT.build())
                        .header(HttpHeaders.AUTHORIZATION, "jwt"))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("JOB ID: " + jobId)));
        waitForInstallStatus(mockMvc, MOCK_BUNDLE_NAME, JobStatus.INSTALL_COMPLETED);
    }

    @Test
    public void shouldRollbackFailingPluginLinking() throws Exception {
        Mockito.reset(coreClient);
        WireMock.reset();
        WireMock.setGlobalFixedDelay(0);

        mockBundle(k8SServiceClient);
        mockPlugins(k8SServiceClient, FAILED);

        stubFor(WireMock.get("/repository/npm-internal/test_bundle/-/test_bundle-0.0.1.tgz")
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/octet-stream")
                        .withBody(readFromDEPackage(TestInstallUtils.MOCK_BUNDLE_NAME_TGZ))));

        stubFor(WireMock.post(urlEqualTo("/auth/protocol/openid-connect/auth"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"access_token\": \"iddqd\" }")));

        TestInstallUtils.stubPermissionRequestReturningSuperuser();

        MvcResult result = mockMvc.perform(post(INSTALL_COMPONENT_ENDPOINT.build())
                        .header(HttpHeaders.AUTHORIZATION, "jwt"))
                .andExpect(status().isCreated())
                .andReturn();

        waitForInstallStatus(mockMvc, MOCK_BUNDLE_NAME, JobStatus.INSTALL_ROLLBACK, JobStatus.INSTALL_ERROR);

        String jobId = JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");

        Optional<EntandoBundleJobEntity> job = jobRepository.findById(UUID.fromString(jobId));

        assertThat(job.isPresent()).isTrue();

        List<EntandoBundleComponentJobEntity> jobComponentList = componentJobRepository.findAllByParentJob(job.get());

        List<EntandoBundleComponentJobEntity> pluginJobs = jobComponentList.stream()
                .filter(jc -> jc.getComponentType().equals(ComponentType.PLUGIN))
                .collect(Collectors.toList());
        assertThat(pluginJobs).hasSize(2);
        assertThat(pluginJobs.stream().map(EntandoBundleComponentJobEntity::getStatus).collect(Collectors.toList()))
                .containsOnly(
                        JobStatus.INSTALL_ERROR, JobStatus.INSTALL_ROLLBACK
                );
    }

    /**
     * this test ensures that the plugin uninstallation can be done using the right data.
     */
    @Test
    void ensureEntandoBundleComponentJobEntityComponentIdCorrectness() throws Exception {

        Mockito.reset(coreClient);
        WireMock.reset();
        WireMock.setGlobalFixedDelay(0);

        mockBundle(k8SServiceClient);
        mockPlugins(k8SServiceClient);

        TestInstallUtils.stubPermissionRequestReturningSuperuser();

        MvcResult result = mockMvc.perform(post(INSTALL_COMPONENT_ENDPOINT.build())
                        .header(HttpHeaders.AUTHORIZATION, "jwt"))
                .andExpect(status().isCreated())
                .andReturn();
        waitForInstallStatus(mockMvc, MOCK_BUNDLE_NAME, JobStatus.INSTALL_COMPLETED);
        String jobId = JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");

        Optional<EntandoBundleJobEntity> job = jobRepository.findById(UUID.fromString(jobId));
        assertThat(job).isPresent();

        List<EntandoBundleComponentJobEntity> pluginJobs = componentJobRepository.findAllByParentJob(job.get())
                .stream()
                .filter(jc -> jc.getComponentType().equals(ComponentType.PLUGIN))
                .collect(Collectors.toList());
        ;

        assertThat(pluginJobs).hasSize(6);

        // when deploymentBaseName is not present => component id should be the image organization, name and version
        assertThat(pluginJobs.get(0).getComponentId()).isEqualTo(TestInstallUtils.PLUGIN_TODOMVC_TODOMVC_1);
        assertThat(pluginJobs.get(1).getComponentId()).isEqualTo(TestInstallUtils.PLUGIN_TODOMVC_TODOMVC_2);
        // when deploymentBaseName is not present => component id should be the deploymentBaseName itself
        assertThat(pluginJobs.get(2).getComponentId()).isEqualTo(TestInstallUtils.PLUGIN_TODOMVC_CUSTOMBASE);
    }


    @Test
    public void shouldUpdateDatabaseOnlyWhenOperationIsCompleted() throws Exception {
        simulateInProgressInstall();
        assertThat(installedCompRepo.findAll()).isEmpty();

        waitForInstallStatus(mockMvc, MOCK_BUNDLE_NAME, JobStatus.INSTALL_COMPLETED);
        assertThat(installedCompRepo.findAll()).isNotEmpty();

        simulateInProgressUninstall();
        assertThat(installedCompRepo.findAll()).isNotEmpty();

        waitForUninstallStatus(mockMvc, MOCK_BUNDLE_NAME, JobStatus.UNINSTALL_COMPLETED);
        assertThat(installedCompRepo.findAll()).isEmpty();
    }

    @Test
    public void shouldNotUpdateDatabaseWhenOperationError() throws Exception {
        simulateFailingInstall();
        assertThat(installedCompRepo.findAll()).isEmpty();

        databaseCleaner.cleanup();

        simulateSuccessfullyCompletedInstall();
        assertThat(installedCompRepo.findAll()).isNotEmpty();

        simulateFailingUninstall();
        assertThat(installedCompRepo.findAll()).isNotEmpty();
    }

    @Test
    void shouldThrowConflictWhenActingDuringUninstallJobInProgress() throws Exception {
        // Given I'm uninstalling an installed component and the job is in progress
        simulateSuccessfullyCompletedInstall();
        simulateInProgressUninstall();

        // I should get a conflict error when trying to install/uninstall the same component
        mockMvc.perform(post(INSTALL_COMPONENT_ENDPOINT.build())
                        .header(HttpHeaders.AUTHORIZATION, "jwt"))
                .andDo(print())
                .andExpect(status().isConflict());
        mockMvc.perform(post(UNINSTALL_COMPONENT_ENDPOINT.build())
                        .header(HttpHeaders.AUTHORIZATION, "jwt"))
                .andExpect(status().isConflict());
        waitForUninstallStatus(mockMvc, MOCK_BUNDLE_NAME, JobStatus.UNINSTALL_COMPLETED);
    }

    @Test
    void shouldThrowConflictWhenActingDuringUninstallJobInProgressWithInstallPlan() throws Exception {
        // Given I'm uninstalling an installed component and the job is in progress
        simulateSuccessfullyCompletedInstallWithInstallPlan();
        simulateInProgressUninstall();

        // I should get a conflict error when trying to install/uninstall the same component
        mockMvc.perform(post(INSTALL_COMPONENT_ENDPOINT.build())
                        .header(HttpHeaders.AUTHORIZATION, "jwt"))
                .andExpect(status().isConflict());
        mockMvc.perform(post(UNINSTALL_COMPONENT_ENDPOINT.build())
                        .header(HttpHeaders.AUTHORIZATION, "jwt"))
                .andExpect(status().isConflict());
        waitForUninstallStatus(mockMvc, MOCK_BUNDLE_NAME, JobStatus.UNINSTALL_COMPLETED);
    }

    @Test
    void shouldCreateJobAndReturn201StatusAndLocationHeader() throws Exception {
        Mockito.reset(coreClient);
        WireMock.reset();
        WireMock.setGlobalFixedDelay(0);

        mockBundle(k8SServiceClient);

        stubFor(WireMock.get("/repository/npm-internal/test_bundle/-/test_bundle-0.0.1.tgz")
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/octet-stream")
                        .withBody(readFromDEPackage(TestInstallUtils.MOCK_BUNDLE_NAME_TGZ))));

        stubFor(WireMock.post(urlEqualTo("/auth/protocol/openid-connect/auth"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"access_token\": \"iddqd\" }")));
        stubFor(WireMock.get(urlMatching("/k8s/.*")).willReturn(aResponse().withStatus(200)));
        //        stubFor(WireMock.post(urlMatching("/entando-app/api/.*")).willReturn(aResponse().withStatus(200)));

        TestInstallUtils.stubPermissionRequestReturningSuperuser();

        MvcResult result = mockMvc.perform(post(INSTALL_COMPONENT_ENDPOINT.build())
                        .header(HttpHeaders.AUTHORIZATION, "jwt"))
                .andExpect(status().isCreated())
                .andReturn();

        String jobId = JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");
        assertThat(result.getResponse().containsHeader("Location")).isTrue();
        assertThat(result.getResponse().getHeader("Location")).endsWith("/jobs/" + jobId);
        waitForJobStatus(() -> getJobStatus(mockMvc, jobId), JobType.FINISHED.getStatuses());
    }

    @Test
    void shouldCreateJobAndReturn201StatusAndLocationHeaderWithInstallPlan() throws Exception {
        Mockito.reset(coreClient);
        WireMock.reset();
        WireMock.setGlobalFixedDelay(0);

        mockBundle(k8SServiceClient);

        stubFor(WireMock.get("/repository/npm-internal/test_bundle/-/test_bundle-0.0.1.tgz")
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/octet-stream")
                        .withBody(readFromDEPackage(TestInstallUtils.MOCK_BUNDLE_NAME_TGZ))));

        stubFor(WireMock.post(urlEqualTo("/auth/protocol/openid-connect/auth"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"access_token\": \"iddqd\" }")));
        stubFor(WireMock.get(urlMatching("/k8s/.*")).willReturn(aResponse().withStatus(200)));

        TestInstallUtils.stubPermissionRequestReturningSuperuser();

        MvcResult result = mockMvc.perform(put(INSTALL_PLANS_ENDPOINT.build())
                        .header(HttpHeaders.AUTHORIZATION, "jwt"))
                .andExpect(status().isCreated())
                .andReturn();

        String jobId = JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");
        assertThat(result.getResponse().containsHeader("Location")).isTrue();
        assertThat(result.getResponse().getHeader("Location")).endsWith("/jobs/" + jobId);
        waitForJobStatus(() -> getJobStatus(mockMvc, jobId), JobType.FINISHED.getStatuses());
    }

    @Test
    public void shouldFailInstallAndHandleExceptionDuringBundleDownloadError() {
        String jobId = simulateBundleDownloadError();
        Optional<EntandoBundleJobEntity> optJob = jobRepository.findById(UUID.fromString(jobId));

        assertThat(optJob.isPresent()).isTrue();
        EntandoBundleJobEntity job = optJob.get();
        assertThat(job.getStatus()).isEqualByComparingTo(JobStatus.INSTALL_ERROR);
        assertThat(job.getFinishedAt() != null);
        assertThat(job.getStartedAt()).isBeforeOrEqualTo(job.getFinishedAt());

        List<EntandoBundleComponentJobEntity> componentJobs = componentJobRepository.findAllByParentJob(job);
        assertThat(componentJobs).isEmpty();
    }


    @Test
    void shouldReturnAValidInstallPlanV1() throws Exception {
        mockAnalysisReportV1(coreClient, k8SServiceClient);
        mockBundle(k8SServiceClient);

        TestInstallUtils.stubPermissionRequestReturningSuperuser();

        InstallPlan expected = TestInstallUtils.mockInstallPlanV1();
        MvcResult response = mockMvc.perform(post(INSTALL_PLANS_ENDPOINT.build())
                        .header(HttpHeaders.AUTHORIZATION, "jwt"))
                .andExpect(status().isOk())
                .andReturn();

        Configuration conf = Configuration.builder()
                .jsonProvider(new JacksonJsonProvider(new ObjectMapper()))
                .build();

        InstallPlan result = JsonPath.using(conf).parse(response.getResponse().getContentAsString())
                .read("$.payload", InstallPlan.class);

        assertOnInstallPlanComponents(result.getFragments(), expected.getFragments());
        assertOnInstallPlanComponents(result.getAssets(), expected.getAssets());
        assertOnInstallPlanComponents(result.getCategories(), expected.getCategories());
        assertOnInstallPlanComponents(result.getContents(), expected.getContents());
        assertOnInstallPlanComponents(result.getContentTemplates(), expected.getContentTemplates());
        assertOnInstallPlanComponents(result.getContentTypes(), expected.getContentTypes());
        assertOnInstallPlanComponents(result.getDirectories(), expected.getDirectories());
        assertOnInstallPlanComponents(result.getGroups(), expected.getGroups());
        assertOnInstallPlanComponents(result.getLabels(), expected.getLabels());
        assertOnInstallPlanComponents(result.getLanguages(), expected.getLanguages());
        assertOnInstallPlanComponents(result.getPages(), expected.getPages());
        assertOnInstallPlanComponents(result.getPageTemplates(), expected.getPageTemplates());
        assertOnInstallPlanComponents(result.getPlugins(), expected.getPlugins());
        assertOnInstallPlanComponents(result.getResources(), expected.getResources());
        assertOnInstallPlanComponents(result.getWidgets(), expected.getWidgets());
    }


    private void assertOnInstallPlanComponents(Map<String, ComponentInstallPlan> current,
            Map<String, ComponentInstallPlan> expected) {

        assertThat(current).hasSameSizeAs(expected);
        assertThat(current).containsAllEntriesOf(expected);
    }


    @Test
    @Disabled("Ignore until rollback is implemented and #fails is tracked")
    public void shouldThrowInternalServerErrorWhenActingOnPreviousInstallErrorState() throws Exception {
        simulateFailingInstall();

        mockMvc.perform(post(INSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isInternalServerError());
        mockMvc.perform(post(UNINSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isInternalServerError());

    }

    @Test
    @Disabled("Ignore untill rollback is implemented and #fails is tracked")
    public void shouldThrowInternalServerErrorWhenActingOnPreviousUninstallErrorState() throws Exception {
        simulateSuccessfullyCompletedInstall();
        simulateFailingUninstall();

        mockMvc.perform(post(INSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isInternalServerError());
        mockMvc.perform(post(UNINSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @Disabled("Just to test install/uninstall and debug")
    public void testErroneousUninstall() throws Exception {
        simulateSuccessfullyCompletedInstall();

        WireMock.reset();
        WireMock.setGlobalFixedDelay(0);

        stubFor(WireMock.post(urlEqualTo("/auth/protocol/openid-connect/auth"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"access_token\": \"iddqd\" }")));
        stubFor(WireMock.delete(urlMatching("/entando-app/api/.*")).willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.delete(urlMatching("/entando-app/api/widgets/.*")).willReturn(aResponse().withStatus(500)));

        mockMvc.perform(post(UNINSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isOk())
                .andReturn();
        waitForUninstallStatus(mockMvc, MOCK_BUNDLE_NAME, JobStatus.UNINSTALL_ERROR);
    }

    @Test
    public void testProgressIsExposedViaApi() throws Exception {
        String jobId = simulateInProgressInstall();
        verifyJobProgressesFromStatusToStatus(jobId, JobStatus.INSTALL_IN_PROGRESS, JobStatus.INSTALL_COMPLETED);

        jobId = simulateInProgressUninstall();
        verifyJobProgressesFromStatusToStatus(jobId, JobStatus.UNINSTALL_IN_PROGRESS, JobStatus.UNINSTALL_COMPLETED);
    }

    @Test
    void shouldReturn409OnInstallIfAnotherBundleOperationIsRunning() throws Exception {

        mockServicesForaSuccessfullyInstallation();

        doCallRealMethod().when(bundleOperationsConcurrencyManager).throwIfAnotherOperationIsRunningOrStartOperation();
        when(bundleOperationsConcurrencyManager.manageStartOperation()).thenReturn(false);

        mockMvc.perform(post(INSTALL_COMPONENT_ENDPOINT.build())
                        .header(HttpHeaders.AUTHORIZATION, "jwt"))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn409OnInstallPlanIfAnotherBundleOperationIsRunning() throws Exception {

        mockAnalysisReportV1(coreClient, k8SServiceClient);
        mockBundle(k8SServiceClient);

        doCallRealMethod().when(bundleOperationsConcurrencyManager).throwIfAnotherOperationIsRunningOrStartOperation();
        when(bundleOperationsConcurrencyManager.manageStartOperation()).thenReturn(false);

        TestInstallUtils.stubPermissionRequestReturningSuperuser();

        mockMvc.perform(post(INSTALL_PLANS_ENDPOINT.build())
                        .header(HttpHeaders.AUTHORIZATION, "jwt"))
                .andExpect(status().isConflict());
    }


    @Test
    void shouldPreventTheInstallationOfABundleWithAPluginWithInvalidSecretNames() throws Exception {

        String compId = "wrong-secret-name";

        final EntandoDeBundle testBundle = TestInstallUtils.getTestBundle();
        testBundle.getMetadata().setName(compId);

        mockBundle(k8SServiceClient, testBundle);

        stubFor(WireMock.get("/repository/npm-internal/test_bundle/-/test_bundle-0.0.1.tgz")
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/octet-stream")
                        .withBody(readFromDEPackage("bundle-invalid-secret.tgz"))));

        TestInstallUtils.stubPermissionRequestReturningSuperuser();

        final UriBuilder uriBuilder = UriComponentsBuilder.newInstance()
                .pathSegment("components", compId, "install");
        mockMvc.perform(post(uriBuilder.build())
                .header(HttpHeaders.AUTHORIZATION, "jwt"))
                .andExpect(status().isCreated());

        waitForInstallStatus(mockMvc, compId, JobStatus.INSTALL_ERROR);
    }

    private void verifyJobProgressesFromStatusToStatus(String jobId, JobStatus startStatus, JobStatus endStatus)
            throws Exception {
        LocalDateTime start = LocalDateTime.now();
        Duration maxDuration = Duration.ofMinutes(1);
        double lastProgress = 0.0;

        EntandoBundleJobEntity job = TestInstallUtils.getJob(mockMvc, jobId);
        double newProgress = job.getProgress();
        JobStatus lastStatus = job.getStatus();
        assertThat(newProgress).isGreaterThanOrEqualTo(lastProgress);
        assertThat(lastStatus).isEqualByComparingTo(startStatus);
        while (!lastStatus.equals(endStatus)) {
            lastProgress = newProgress;
            job = TestInstallUtils.getJob(mockMvc, jobId);
            newProgress = job.getProgress();
            lastStatus = job.getStatus();
            assertThat(newProgress).isGreaterThanOrEqualTo(lastProgress);
            assertThat(Duration.between(start, LocalDateTime.now())).isLessThan(maxDuration);
            Thread.sleep(1000);
        }
        job = TestInstallUtils.getJob(mockMvc, jobId);
        newProgress = job.getProgress();
        assertThat(newProgress).isEqualTo(1.0);
    }

    private void mockServicesForaSuccessfullyInstallation() {
        TestInstallUtils
                .mockSuccessfullyCompletedInstallV5(coreClient, k8SServiceClient,
                        TestInstallUtils.MOCK_BUNDLE_NAME_TGZ);
    }

    private String simulateSuccessfullyCompletedInstall() {
        return TestInstallUtils
                .simulateSuccessfullyCompletedInstall(mockMvc, coreClient, k8SServiceClient,
                        TestInstallUtils.MOCK_BUNDLE_NAME_TGZ);
    }

    private String simulateSuccessfullyCompletedUpdate() {
        return TestInstallUtils
                .simulateSuccessfullyCompletedUpdate(mockMvc, coreClient, k8SServiceClient,
                        TestInstallUtils.MOCK_BUNDLE_NAME_TGZ);
    }

    private String simulateSuccessfullyCompletedInstallWithInstallPlan() {
        return TestInstallUtils
                .simulateSuccessfullyCompletedInstallWithInstallPlan(mockMvc, coreClient, k8SServiceClient,
                        TestInstallUtils.MOCK_BUNDLE_NAME_TGZ);
    }

    private String simulateSuccessfullyCompletedInstallWithInstallPlanAndInstallPlanRequest() {
        return TestInstallUtils
                .simulateSuccessfullyCompletedInstallWithInstallPlanAndInstallPlanRequest(mockMvc, coreClient,
                        k8SServiceClient,
                        TestInstallUtils.MOCK_BUNDLE_NAME_TGZ);
    }

    private String simulateSuccessfullyCompletedUninstall() {
        return TestInstallUtils.simulateSuccessfullyCompletedUninstall(mockMvc, coreClient);
    }

    private String simulateInProgressInstall() {
        return TestInstallUtils.simulateInProgressInstall(mockMvc, coreClient, k8SServiceClient,
                TestInstallUtils.MOCK_BUNDLE_NAME_TGZ);
    }

    private String simulateInProgressUninstall() {
        return TestInstallUtils.simulateInProgressUninstall(mockMvc, coreClient);
    }

    private String simulateFailingInstall() {
        return TestInstallUtils.simulateFailingInstall(mockMvc, coreClient, k8SServiceClient,
                TestInstallUtils.MOCK_BUNDLE_NAME_TGZ);
    }

    private String simulateFailingInstallWithInstallPlan() {
        return TestInstallUtils
                .simulateFailingInstallWithInstallPlan(mockMvc, coreClient, k8SServiceClient,
                        TestInstallUtils.MOCK_BUNDLE_NAME_TGZ);
    }

    private String simulateHugeAssetFailingInstall() {
        return TestInstallUtils
                .simulateHugeAssetFailingInstall(mockMvc, coreClient, k8SServiceClient,
                        TestInstallUtils.MOCK_BUNDLE_NAME_TGZ);
    }

    private String simulateHugeAssetFailingInstallWithPlan() {
        return TestInstallUtils
                .simulateHugeAssetFailingInstallWithInstallPlan(mockMvc, coreClient, k8SServiceClient,
                        TestInstallUtils.MOCK_BUNDLE_NAME_TGZ);
    }

    private String simulateFailingUninstall() {
        return TestInstallUtils.simulateFailingUninstall(mockMvc, coreClient);
    }

    private String simulateBundleDownloadError() {
        return TestInstallUtils.simulateBundleDownloadError(mockMvc, coreClient, k8SServiceClient, downloaderFactory);
    }
}
