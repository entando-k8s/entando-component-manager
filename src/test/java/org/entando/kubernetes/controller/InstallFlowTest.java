package org.entando.kubernetes.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.entando.kubernetes.DigitalExchangeTestUtils.checkRequest;
import static org.entando.kubernetes.DigitalExchangeTestUtils.readFile;
import static org.entando.kubernetes.DigitalExchangeTestUtils.readFileAsBase64;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.jayway.jsonpath.JsonPath;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.entando.kubernetes.DatabaseCleaner;
import org.entando.kubernetes.EntandoKubernetesJavaApplication;
import org.entando.kubernetes.client.K8SServiceClientTestDouble;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.config.TestAppConfiguration;
import org.entando.kubernetes.config.TestKubernetesConfig;
import org.entando.kubernetes.config.TestSecurityConfiguration;
import org.entando.kubernetes.model.bundle.downloader.GitBundleDownloader;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleBuilder;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpec;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpecBuilder;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeComponent;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJobComponent;
import org.entando.kubernetes.model.digitalexchange.JobStatus;
import org.entando.kubernetes.model.link.EntandoAppPluginLink;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.entando.kubernetes.repository.DigitalExchangeInstalledComponentRepository;
import org.entando.kubernetes.repository.DigitalExchangeJobComponentRepository;
import org.entando.kubernetes.repository.DigitalExchangeJobRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
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
                TestAppConfiguration.class
        })
@ActiveProfiles({"test"})
@Tag("component")
public class InstallFlowTest {

    private final UriBuilder ALL_COMPONENTS_ENDPOINT = UriComponentsBuilder.newInstance().pathSegment("components");
    private final UriBuilder SINGLE_COMPONENT_ENDPOINT = UriComponentsBuilder.newInstance().pathSegment("components","todomvc");
    private final UriBuilder UNINSTALL_COMPONENT_ENDPOINT = UriComponentsBuilder.newInstance().pathSegment("components", "todomvc", "uninstall");
    private final UriBuilder INSTALL_COMPONENT_ENDPOINT = UriComponentsBuilder.newInstance().pathSegment("components", "todomvc", "install");

    private static final String JOBS_ENDPOINT = "/jobs";
    private static final String MOCK_BUNDLE_NAME = "bundle.tgz";
    private static final Duration MAX_WAITING_TIME_FOR_JOB_STATUS = Duration.ofMinutes(45);
    private static final Duration AWAITILY_DEFAULT_POLL_INTERVAL = Duration.ofSeconds(1);

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @Autowired
    private K8SServiceClient k8SServiceClient;

    @Autowired
    private DigitalExchangeJobComponentRepository jobComponentRepository;

    @Autowired
    private DigitalExchangeJobRepository jobRepository;

    @Autowired
    private DigitalExchangeInstalledComponentRepository installedCompRepo;

    @MockBean
    private GitBundleDownloader gitBundleDownloader;

//    @TestConfiguration
//    static class TestConfig {
//
//        @Bean
//        @Primary
//        public DigitalExchangeJobRepository jobRepository() {
//            return Mockito.spy(DigitalExchangeJobRepository.class);
//        }
//
//    }


    @AfterEach
    public void cleanup() {
        WireMock.reset();
        databaseCleaner.cleanup();
        ((K8SServiceClientTestDouble) k8SServiceClient).cleanInMemoryDatabases();
    }

    @Test
    public void shouldReturnNotFoundWhenBundleDoesntExists() throws Exception {
        mockMvc.perform(post(INSTALL_COMPONENT_ENDPOINT.build()))
                .andDo(print()).andExpect(status().isNotFound());
    }

    @Test
    public void shouldCallCoreToInstallComponents() throws Exception {
        simulateSuccessfullyCompletedInstall();


        K8SServiceClientTestDouble k8SServiceClientTestDouble = (K8SServiceClientTestDouble)  k8SServiceClient;
        // Verify interaction with mocks
        List<EntandoAppPluginLink> createdLinks = k8SServiceClientTestDouble.getInMemoryLinkCopy();
        Optional<EntandoAppPluginLink> appPluginLinkForTodoMvc = createdLinks.stream()
                .filter(link -> link.getSpec().getEntandoPluginName().equals("todomvc")).findAny();

        assertTrue(appPluginLinkForTodoMvc.isPresent());

        WireMock.verify(2, postRequestedFor(urlEqualTo("/entando-app/api/widgets")));
        WireMock.verify(2, postRequestedFor(urlEqualTo("/entando-app/api/pageModels")));
        WireMock.verify(5, postRequestedFor(urlEqualTo("/entando-app/api/fileBrowser/directory")));
        WireMock.verify(5, postRequestedFor(urlEqualTo("/entando-app/api/fileBrowser/file")));
        WireMock.verify(1, postRequestedFor(urlEqualTo("/entando-app/api/plugins/cms/contentTypes")));
        WireMock.verify(2, postRequestedFor(urlEqualTo("/entando-app/api/plugins/cms/contentmodels")));
        WireMock.verify(1, postRequestedFor(urlEqualTo("/entando-app/api/labels")));
        WireMock.verify(2, postRequestedFor(urlEqualTo("/entando-app/api/fragments")));
        WireMock.verify(1, postRequestedFor(urlEqualTo("/entando-app/api/pages")));

        List<LoggedRequest> widgetRequests = findAll(postRequestedFor(urlEqualTo("/entando-app/api/widgets")));
        List<LoggedRequest> pageModelRequests = findAll(
          postRequestedFor(urlEqualTo("/entando-app/api/pageModels")));
        List<LoggedRequest> directoryRequests = findAll(
          postRequestedFor(urlEqualTo("/entando-app/api/fileBrowser/directory")));
        List<LoggedRequest> fileRequests = findAll(
          postRequestedFor(urlEqualTo("/entando-app/api/fileBrowser/file")));
        List<LoggedRequest> contentTypeRequests = findAll(
          postRequestedFor(urlEqualTo("/entando-app/api/plugins/cms/contentTypes")));
        List<LoggedRequest> contentModelRequests = findAll(
          postRequestedFor(urlEqualTo("/entando-app/api/plugins/cms/contentmodels")));
        List<LoggedRequest> labelRequests = findAll(postRequestedFor(urlEqualTo("/entando-app/api/labels")));
        List<LoggedRequest> fragmentRequests = findAll(postRequestedFor(urlEqualTo("/entando-app/api/fragments")));
        List<LoggedRequest> pageRequests = findAll(postRequestedFor(urlEqualTo("/entando-app/api/pages")));

//        checkRequests(widgetRequests, pageModelRequests, directoryRequests, fileRequests, contentTypeRequests,
//                contentModelRequests, labelRequests, fragmentRequests, pageRequests);

        widgetRequests.sort(Comparator.comparing(InstallFlowTest::requestCode));
        pageModelRequests.sort(Comparator.comparing(InstallFlowTest::requestCode));
        directoryRequests.sort(Comparator.comparing(InstallFlowTest::requestPath));
        fileRequests.sort(Comparator.comparing(InstallFlowTest::requestPath));
        fragmentRequests.sort(Comparator.comparing(InstallFlowTest::requestCode));
        pageRequests.sort(Comparator.comparing(InstallFlowTest::requestCode));

        checkRequest(widgetRequests.get(0))
                .expectEqual("code", "another_todomvc_widget")
                .expectEqual("group", "free")
                .expectEqual("customUi", readFile("/bundle/widgets/widget.ftl"));

        checkRequest(widgetRequests.get(1))
                .expectEqual("code", "todomvc_widget")
                .expectEqual("group", "free")
                .expectEqual("customUi", "<h2>Bundle 1 Widget</h2>");

        checkRequest(fragmentRequests.get(0))
                .expectEqual("code", "another_fragment")
                .expectEqual("guiCode", readFile("/bundle/fragments/fragment.ftl"));

        checkRequest(fragmentRequests.get(1))
                .expectEqual("code", "title_fragment")
                .expectEqual("guiCode", "<h2>Bundle 1 Fragment</h2>");

        checkRequest(pageModelRequests.get(0))
                .expectEqual("code", "todomvc_another_page_model")
                .expectEqual("descr", "TODO MVC another page model")
                .expectEqual("configuration.frames[0].pos", "0")
                .expectEqual("configuration.frames[0].descr", "Simple Frame")
                .expectEqual("template", readFile("/bundle/pagemodels/page.ftl"));

        checkRequest(pageModelRequests.get(1))
                .expectEqual("code", "todomvc_page_model")
                .expectEqual("descr", "TODO MVC basic page model")
                .expectEqual("configuration.frames[0].pos", "0")
                .expectEqual("configuration.frames[0].descr", "Header")
                .expectEqual("configuration.frames[0].sketch.x1", "0")
                .expectEqual("configuration.frames[0].sketch.y1", "0")
                .expectEqual("configuration.frames[0].sketch.x2", "11")
                .expectEqual("configuration.frames[0].sketch.y2", "0")
                .expectEqual("configuration.frames[1].pos", "1")
                .expectEqual("configuration.frames[1].descr", "Breadcrumb")
                .expectEqual("configuration.frames[1].sketch.x1", "0")
                .expectEqual("configuration.frames[1].sketch.y1", "1")
                .expectEqual("configuration.frames[1].sketch.x2", "11")
                .expectEqual("configuration.frames[1].sketch.y2", "1");

        checkRequest(directoryRequests.get(0))
                .expectEqual("path", "/todomvc")
                .expectEqual("protectedFolder", false);

        checkRequest(directoryRequests.get(1))
                .expectEqual("path", "/todomvc/css")
                .expectEqual("protectedFolder", false);

        checkRequest(directoryRequests.get(2))
                .expectEqual("path", "/todomvc/js")
                .expectEqual("protectedFolder", false);

        checkRequest(fileRequests.get(0))
                .expectEqual("filename", "custom.css")
                .expectEqual("path", "/todomvc/css/custom.css")
                .expectEqual("base64", readFileAsBase64("/bundle/resources/css/custom.css"));

        checkRequest(fileRequests.get(1))
                .expectEqual("filename", "style.css")
                .expectEqual("path", "/todomvc/css/style.css")
                .expectEqual("base64", readFileAsBase64("/bundle/resources/css/style.css"));

        checkRequest(fileRequests.get(2))
                .expectEqual("filename", "configUiScript.js")
                .expectEqual("path", "/todomvc/js/configUiScript.js")
                .expectEqual("base64", readFileAsBase64("/bundle/resources/js/configUiScript.js"));

        checkRequest(fileRequests.get(3))
                .expectEqual("filename", "script.js")
                .expectEqual("path", "/todomvc/js/script.js")
                .expectEqual("base64", readFileAsBase64("/bundle/resources/js/script.js"));

        checkRequest(pageRequests.get(0))
                .expectEqual("code", "my-page")
                .expectEqual("titles.it", "La mia pagina")
                .expectEqual("titles.en", "My page")
                .expectEqual("parentCode", "homepage")
                .expectEqual("pageModel", "service")
                .expectEqual("ownerGroup", "administrators")
                .expectEqual("joinGroups[0]", "free")
                .expectEqual("joinGroups[1]", "customers")
                .expectEqual("joinGroups[2]", "developers")
                .expectEqual("displayedInMenu", "true")
                .expectEqual("seo", "false")
                .expectEqual("status", "published");

        // Finish first test
    }

    @Test
    public void shouldRecordJobStatusAndComponentsForAuditingWhenInstallComponents() throws Exception {
        simulateSuccessfullyCompletedInstall();

        List<DigitalExchangeJob> jobs = jobRepository.findAll();
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).getStatus()).isEqualByComparingTo(JobStatus.INSTALL_COMPLETED);

        List<DigitalExchangeJobComponent> jobComponentList = jobComponentRepository.findAllByJob(jobs.get(0));
        assertThat(jobComponentList).hasSize(22);
        List<String> jobComponentNames = jobComponentList.stream().map(DigitalExchangeJobComponent::getName).collect(Collectors.toList());
        assertThat(jobComponentNames).containsExactlyInAnyOrder(
                "/todomvc",
                "/todomvc/js",
                "/todomvc/css",
                "/todomvc/vendor",
                "/todomvc/vendor/jquery",
                "/todomvc/css/style.css",
                "/todomvc/js/script.js",
                "/todomvc/css/custom.css",
                "/todomvc/js/configUiScript.js",
                "/todomvc/vendor/jquery/jquery.js",
                "todomvc",
                "my-page",
                "todomvc_another_page_model",
                "todomvc_page_model",
                "another_fragment",
                "title_fragment",
                "CNG",
                "8880002",
                "8880003",
                "todomvc_widget",
                "another_todomvc_widget",
                "HELLO"
        );

        Map<ComponentType, Integer> jobComponentTypes = new HashMap<>();
        for(DigitalExchangeJobComponent jcomp: jobComponentList) {
            Integer n = jobComponentTypes.getOrDefault(jcomp.getComponentType(), 0);
            jobComponentTypes.put(jcomp.getComponentType(), n + 1);
        }

        Map<ComponentType, Integer> expectedComponents = new HashMap<>();
        expectedComponents.put(ComponentType.WIDGET, 2);
        expectedComponents.put(ComponentType.RESOURCE, 10);
        expectedComponents.put(ComponentType.PAGE_MODEL, 2);
        expectedComponents.put(ComponentType.CONTENT_TYPE, 1);
        expectedComponents.put(ComponentType.CONTENT_MODEL, 2);
        expectedComponents.put(ComponentType.LABEL, 1);
        expectedComponents.put(ComponentType.FRAGMENT, 2);
        expectedComponents.put(ComponentType.PAGE, 1);
        expectedComponents.put(ComponentType.PLUGIN, 1);

        assertThat(jobComponentTypes).containsAllEntriesOf(expectedComponents);
    }

    @Test
    public void shouldCallCoreToUninstallComponents() throws Exception {
        String jobId = simulateSuccessfullyCompletedInstall();

        verifyJobHasComponentAndStatus(jobId, JobStatus.INSTALL_COMPLETED);

        String uninstallJobId = simulateSuccessfullyCompletedUninstall();

        WireMock.verify(1, deleteRequestedFor(urlEqualTo("/entando-app/api/widgets/todomvc_widget")));
        WireMock.verify(1, deleteRequestedFor(urlEqualTo("/entando-app/api/widgets/another_todomvc_widget")));
        WireMock.verify(1, deleteRequestedFor(urlEqualTo("/entando-app/api/pageModels/todomvc_page_model")));
        WireMock.verify(1, deleteRequestedFor(urlEqualTo("/entando-app/api/pageModels/todomvc_another_page_model")));
        WireMock.verify(1, deleteRequestedFor(
                urlEqualTo("/entando-app/api/fileBrowser/directory?protectedFolder=false&currentPath=/todomvc")));
        WireMock.verify(1, deleteRequestedFor(urlEqualTo("/entando-app/api/fragments/title_fragment")));
        WireMock.verify(1, deleteRequestedFor(urlEqualTo("/entando-app/api/fragments/another_fragment")));
        WireMock.verify(1, deleteRequestedFor(urlEqualTo("/entando-app/api/pages/my-page")));

        verifyJobHasComponentAndStatus(uninstallJobId, JobStatus.UNINSTALL_COMPLETED);

    }

    @Test
    public void shouldRecordJobStatusAndComponentsForAuditingWhenUninstallComponents() throws Exception {
        String jobId = simulateSuccessfullyCompletedInstall();

        verifyJobHasComponentAndStatus(jobId, JobStatus.INSTALL_COMPLETED);

        simulateSuccessfullyCompletedUninstall();
        List<DigitalExchangeJob> jobs = jobRepository.findAll();
        assertThat(jobs).hasSize(2);
        assertThat(jobs.get(0).getStatus()).isEqualByComparingTo(JobStatus.INSTALL_COMPLETED);
        assertThat(jobs.get(1).getStatus()).isEqualByComparingTo(JobStatus.UNINSTALL_COMPLETED);

        List<DigitalExchangeJobComponent> installedComponentList = jobComponentRepository.findAllByJob(jobs.get(0));
        List<DigitalExchangeJobComponent> uninstalledComponentList = jobComponentRepository.findAllByJob(jobs.get(1));
        assertThat(uninstalledComponentList).hasSize(installedComponentList.size());
        List<JobStatus> jobComponentStatus = uninstalledComponentList.stream().map(DigitalExchangeJobComponent::getStatus).collect(Collectors.toList());
        assertThat(jobComponentStatus).allMatch((jcs) -> jcs.equals(JobStatus.UNINSTALL_COMPLETED));

        boolean matchFound = false;
        for(DigitalExchangeJobComponent ic: installedComponentList) {
            matchFound = uninstalledComponentList.stream().anyMatch(uc -> {
                return uc.getJob().getId().equals(jobs.get(1).getId()) &&
                        uc.getName().equals(ic.getName()) &&
                        uc.getComponentType().equals(ic.getComponentType()) &&
                        uc.getChecksum().equals(ic.getChecksum());
            });
            if (!matchFound) {
                break;
            }
        }
        assertThat(matchFound).isTrue();
    }

    @Test
    public void installedComponentShouldReturnInstalledFieldTrueAndEntryInTheInstalledComponentDatabase() throws Exception {

        simulateSuccessfullyCompletedInstall();

        mockMvc.perform(get(ALL_COMPONENTS_ENDPOINT.build()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").isArray())
                .andExpect(jsonPath("$.payload", hasSize(1)))
                .andExpect(jsonPath("$.payload[0].id").value("todomvc"))
                .andExpect(jsonPath("$.payload[0].installed").value("true"));

        List<DigitalExchangeComponent> installedComponents = installedCompRepo.findAll();
        assertThat(installedComponents).hasSize(1);
        assertThat(installedComponents.get(0).getId()).isEqualTo("todomvc");
        assertThat(installedComponents.get(0).isInstalled()).isEqualTo(true);
        assertThat(installedComponents.get(0).getDigitalExchangeId()).isEqualTo("entando-de-bundles");
        assertThat(installedComponents.get(0).getDigitalExchangeName()).isEqualTo("entando-de-bundles");

    }

    @Test
    public void erroneousInstallationShouldRollback() throws Exception {
        // Given a failed install happened
        String failingJobId = simulateFailingInstall();

        // Install Job should have been rollback
        mockMvc.perform(get(INSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.id").value(failingJobId))
                .andExpect(jsonPath("$.payload.componentId").value("todomvc"))
                .andExpect(jsonPath("$.payload.status").value(JobStatus.INSTALL_ROLLBACK.toString()));

        Optional<DigitalExchangeJob> job = jobRepository.findById(UUID.fromString(failingJobId));
        assertThat(job.isPresent()).isTrue();

        // And for each installed component job there should be a component job that rollbacked the install
        List<DigitalExchangeJobComponent> jobRelatedComponents = jobComponentRepository.findAllByJob(job.get());
        List<DigitalExchangeJobComponent> installedComponents = jobRelatedComponents.stream().filter(j ->
                j.getStatus().equals(JobStatus.INSTALL_COMPLETED)).collect(
                Collectors.toList());
        for(DigitalExchangeJobComponent c : installedComponents) {
            List<DigitalExchangeJobComponent> jobs = jobRelatedComponents.stream().filter(j ->
                    j.getComponentType().equals(c.getComponentType()) && j.getName().equals(c.getName()))
                    .collect(Collectors.toList());
            assertThat(jobs.size()).isEqualTo(2);
            assertThat(jobs.stream().anyMatch(j -> j.getStatus().equals(JobStatus.INSTALL_ROLLBACK))).isTrue();
        }

        // And component should not be part of the installed components
        assertThat(installedCompRepo.findAll()).isEmpty();
    }

    @Test
    public void erroneousInstallationOfComponentShouldReturnComponentIsNotInstalled() throws Exception {
        // Given a failed install happened
        String failingJobId = simulateFailingInstall();

        // Components endpoints should still return the component is not installed
        mockMvc.perform(get(ALL_COMPONENTS_ENDPOINT.build()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").isArray())
                .andExpect(jsonPath("$.payload", hasSize(1)))
                .andExpect(jsonPath("$.payload[0].id").value("todomvc"))
                .andExpect(jsonPath("$.payload[0].installed").value("false"));

        // Component install status should be rollback
        mockMvc.perform(get(INSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.id").value(failingJobId))
                .andExpect(jsonPath("$.payload.componentId").value("todomvc"))
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
        mockMvc.perform(get( JOBS_ENDPOINT + "?component={component}", "todomvc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").isArray())
                .andExpect(jsonPath("$.payload.*.id", hasSize(3)))
                .andExpect(jsonPath("$.payload.*.id").value(contains(
                        failingInstallId, successfulUninstallId, successfulInstallId
                )));
    }

    @Test
    public void shouldReturnTheSameJobIdWhenTemptingToInstallTheSameComponentTwice() throws Exception {

        // Given I try to reinstall a component which is already installed
        String firstSuccessfulJobId = simulateSuccessfullyCompletedInstall();
        String secondSuccessfulJobId = simulateSuccessfullyCompletedInstall();

        // Only one job is created
        assertThat(firstSuccessfulJobId).isEqualTo(secondSuccessfulJobId);
        assertThat(jobRepository.findAll().size()).isEqualTo(1);
    }

    @Test
    public void shouldThrowConflictWhenActingDuringInstallJobInProgress() throws Exception {
        // Given I have an in progress installatioon
        String jobId = simulateInProgressInstall();

        // I should get a conflict when trying to install or uninstall the same component
        mockMvc.perform(post(INSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("JOB ID: " + jobId)));
        mockMvc.perform(post(UNINSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("JOB ID: " + jobId)));
        waitForInstallStatus(JobStatus.INSTALL_COMPLETED);
    }

    @Test
    public void shouldUpdateDatabaseOnlyWhenOperationIsCompleted() throws Exception {
        simulateInProgressInstall();
        assertThat(installedCompRepo.findAll()).isEmpty();

        waitForInstallStatus(JobStatus.INSTALL_COMPLETED);
        assertThat(installedCompRepo.findAll()).isNotEmpty();

        simulateInProgressUninstall();
        assertThat(installedCompRepo.findAll()).isNotEmpty();

        waitForUninstallStatus(JobStatus.UNINSTALL_COMPLETED);
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
    public void shouldThrowConflictWhenActingDuringUninstallJobInProgress() throws Exception {
        // Given I'm uninstalling an installed component and the job is in progress
        simulateSuccessfullyCompletedInstall();
        simulateInProgressUninstall();

        // I should get a conflict error when trying to install/uninstall the same component
        mockMvc.perform(post(INSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isConflict());
        mockMvc.perform(post(UNINSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isConflict());
        waitForUninstallStatus(JobStatus.UNINSTALL_COMPLETED);
    }

    @Test
    @Disabled("Ignore untill rollback is implemented and #fails is tracked")
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
        waitForUninstallStatus(JobStatus.UNINSTALL_ERROR);
        assertThat(true).isTrue();
    }

    private void verifyJobHasComponentAndStatus(String jobId, JobStatus expectedStatus)
            throws Exception {
        mockMvc.perform(get(JOBS_ENDPOINT + "/{id}", jobId))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("payload.componentId").value("todomvc"))
                .andExpect(jsonPath("payload.status").value(expectedStatus.toString()));
    }

    private String simulateSuccessfullyCompletedInstall() throws Exception {
        WireMock.reset();
        WireMock.setGlobalFixedDelay(0);

        K8SServiceClientTestDouble k8SServiceClientTestDouble = (K8SServiceClientTestDouble) k8SServiceClient;
        k8SServiceClientTestDouble.addInMemoryBundle(getTestBundle());

        stubFor(WireMock.get("/repository/npm-internal/inail_bundle/-/inail_bundle-0.0.1.tgz")
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/octet-stream")
                        .withBody(readFromDEPackage())));

        stubFor(WireMock.post(urlEqualTo("/auth/protocol/openid-connect/auth"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"access_token\": \"iddqd\" }")));
        stubFor(WireMock.get(urlMatching("/k8s/.*")).willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.post(urlMatching("/entando-app/api/.*")).willReturn(aResponse().withStatus(200)));

        MvcResult result = mockMvc.perform(post(INSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isOk())
                .andReturn();

        waitForInstallStatus(JobStatus.INSTALL_COMPLETED);

        return JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");
    }


    private String simulateSuccessfullyCompletedUninstall() throws Exception {
        WireMock.reset();
        WireMock.setGlobalFixedDelay(0);

        stubFor(WireMock.post(urlEqualTo("/auth/protocol/openid-connect/auth"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"access_token\": \"iddqd\" }")));
        stubFor(WireMock.delete(urlMatching("/entando-app/api/.*")).willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.get(urlMatching("/k8s/.*")).willReturn(aResponse().withStatus(200)));

        MvcResult result = mockMvc.perform(post(UNINSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isOk())
                .andReturn();

        waitForUninstallStatus(JobStatus.UNINSTALL_COMPLETED);

        return JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");
    }

    private String simulateFailingInstall() throws Exception {
        WireMock.reset();
        WireMock.setGlobalFixedDelay(0);

        K8SServiceClientTestDouble k8SServiceClientTestDouble = (K8SServiceClientTestDouble) k8SServiceClient;

        k8SServiceClientTestDouble.addInMemoryBundle(getTestBundle());

        stubFor(WireMock.get("/repository/npm-internal/inail_bundle/-/inail_bundle-0.0.1.tgz")
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/octet-stream")
                        .withBody(readFromDEPackage())));

        stubFor(WireMock.post(urlEqualTo("/auth/protocol/openid-connect/auth"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"access_token\": \"iddqd\" }")));

        stubFor(WireMock.get(urlMatching("/k8s/.*")).willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.delete(urlMatching("/entando-app/api.*")).willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.post(urlMatching("/entando-app/api/.*")).willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.post(urlMatching("/entando-app/api/pages/?")).willReturn(aResponse().withStatus(500)));


        MvcResult result = mockMvc.perform(post(INSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isOk())
                .andReturn();

        waitForPossibleStatus(JobStatus.INSTALL_ROLLBACK, JobStatus.INSTALL_ERROR);

        return JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");
    }

    private String simulateFailingUninstall() throws Exception {

        WireMock.reset();
        WireMock.setGlobalFixedDelay(0);

        stubFor(WireMock.post(urlEqualTo("/auth/protocol/openid-connect/auth"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"access_token\": \"iddqd\" }")));
        stubFor(WireMock.delete(urlMatching("/entando-app/api/.*")).willReturn(aResponse().withStatus(500)));
        stubFor(WireMock.get(urlMatching("/k8s/.*")).willReturn(aResponse().withStatus(200)));

        MvcResult result = mockMvc.perform(post(UNINSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isOk())
                .andReturn();
        waitForUninstallStatus(JobStatus.UNINSTALL_ERROR);

        return JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");

    }

    private String simulateInProgressInstall() throws Exception {
        WireMock.reset();
        WireMock.setGlobalFixedDelay(1000);

        K8SServiceClientTestDouble k8SServiceClientTestDouble = (K8SServiceClientTestDouble) k8SServiceClient;
        k8SServiceClientTestDouble.addInMemoryBundle(getTestBundle());

        stubFor(WireMock.get("/repository/npm-internal/inail_bundle/-/inail_bundle-0.0.1.tgz")
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/octet-stream")
                        .withBody(readFromDEPackage())));

        stubFor(WireMock.post(urlEqualTo("/auth/protocol/openid-connect/auth"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"access_token\": \"iddqd\" }")));

        stubFor(WireMock.post(urlMatching("/entando-app/api/.*")).willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.get(urlMatching("/k8s/.*")).willReturn(aResponse().withStatus(200)));

        MvcResult result = mockMvc.perform(post(INSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isOk())
                .andReturn();

        waitForInstallStatus(JobStatus.INSTALL_IN_PROGRESS);

        return JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");
    }

    private String simulateInProgressUninstall() throws Exception {
        WireMock.reset();
        WireMock.setGlobalFixedDelay(1000);

        stubFor(WireMock.post(urlEqualTo("/auth/protocol/openid-connect/auth"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"access_token\": \"iddqd\" }")));
        stubFor(WireMock.delete(urlMatching("/entando-app/api/.*")).willReturn(aResponse().withStatus(200)));

        MvcResult result = mockMvc.perform(post(UNINSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isOk())
                .andReturn();
        waitForUninstallStatus(JobStatus.UNINSTALL_IN_PROGRESS);

        return JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");


    }

    private void waitForPossibleStatus(JobStatus... statuses) {
        await().atMost(MAX_WAITING_TIME_FOR_JOB_STATUS)
                .pollInterval(AWAITILY_DEFAULT_POLL_INTERVAL)
                .until(() -> Arrays.asList(statuses).contains(getInstallJob().getPayload().getStatus()));
    }

    private void waitForInstallStatus(JobStatus status) {
        await().atMost(MAX_WAITING_TIME_FOR_JOB_STATUS)
                .pollInterval(AWAITILY_DEFAULT_POLL_INTERVAL)
                .until(() -> getInstallJob().getPayload().getStatus().equals(status));
    }

    private void waitForUninstallStatus(JobStatus status) {
        await().atMost(MAX_WAITING_TIME_FOR_JOB_STATUS)
                .pollInterval(AWAITILY_DEFAULT_POLL_INTERVAL)
                .until(() -> getUninstallJob().getPayload().getStatus().equals(status));
    }

    private SimpleRestResponse<DigitalExchangeJob> getInstallJob() throws Exception{
        return mapper.readValue(mockMvc.perform(get(JOBS_ENDPOINT + "?component=todomvc&type=INSTALL"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), new TypeReference<SimpleRestResponse<DigitalExchangeJob>>(){});
    }

    private SimpleRestResponse<DigitalExchangeJob> getUninstallJob() throws Exception{
        return mapper.readValue(mockMvc.perform(get(JOBS_ENDPOINT + "?component=todomvc&type=UNINSTALL"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), new TypeReference<SimpleRestResponse<DigitalExchangeJob>>(){});
    }

    private byte[] readFromDEPackage() throws IOException {
        try (final InputStream inputStream = getClass().getClassLoader().getResourceAsStream(MOCK_BUNDLE_NAME)) {
            try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                Assertions.assertNotNull(inputStream);
                IOUtils.copy(inputStream, outputStream);
                return outputStream.toByteArray();
            }
        }
    }

    @SafeVarargs
    private final void checkRequests(final List<LoggedRequest>... requests) {
        final List<LoggedRequest> allRequests = new ArrayList<>();
        for (List<LoggedRequest> request : requests) {
            allRequests.addAll(request);
        }

//        for (final LoggedRequest req : allRequests) {
//            assertThat(req.getHeader("Authorization")).isEqualTo("Bearer iddqd");
//        }
    }

    private static String requestProperty(final LoggedRequest request, final String property) {
        return JsonPath.read(new String(request.getBody()), property);
    }

    private static String requestCode(final LoggedRequest request) {
        return requestProperty(request, "code");
    }

    private static String requestPath(final LoggedRequest request) {
        return requestProperty(request, "path");
    }

    private EntandoDeBundle getTestBundle() {
        return new EntandoDeBundleBuilder()
                .withNewMetadata()
                .withName("todomvc")
                .withNamespace("entando-de-bundles")
                .endMetadata()
                .withSpec(getTestEntandoDeBundleSpec()).build();

    }

    private EntandoDeBundleSpec getTestEntandoDeBundleSpec() {
        return new EntandoDeBundleSpecBuilder()
                .withNewDetails()
                .withDescription("A bundle containing some demo components for Entando6")
                .withName("todomvc")
                .addNewVersion("0.0.1")
                .addNewKeyword("entando6")
                .addNewKeyword("digital-exchange")
                .addNewDistTag("latest", "0.0.1")
                .and()
                .addNewTag()
                .withVersion("0.0.1")
                .withIntegrity("sha512-n4TEroSqg/sZlEGg2xj6RKNtl/t3ZROYdNd99/dl3UrzCUHvBrBxZ1rxQg/sl3kmIYgn3+ogbIFmUZYKWxG3Ag==")
                .withShasum("4d80130d7d651176953b5ce470c3a6f297a70815")
                .withTarball("http://localhost:8099/repository/npm-internal/inail_bundle/-/inail_bundle-0.0.1.tgz")
                .endTag()
                .build();
    }

}
