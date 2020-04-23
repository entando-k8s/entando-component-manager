package org.entando.kubernetes.controller.mockmvc;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.entando.kubernetes.DigitalExchangeTestUtils.readFile;
import static org.entando.kubernetes.DigitalExchangeTestUtils.readFileAsBase64;
import static org.entando.kubernetes.utils.SleepStubber.doSleep;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.entando.kubernetes.DatabaseCleaner;
import org.entando.kubernetes.EntandoKubernetesJavaApplication;
import org.entando.kubernetes.client.K8SServiceClientTestDouble;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.config.TestAppConfiguration;
import org.entando.kubernetes.config.TestKubernetesConfig;
import org.entando.kubernetes.config.TestSecurityConfiguration;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FragmentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageModelDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.downloader.GitBundleDownloader;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleBuilder;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpec;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpecBuilder;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.EntandoBundle;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleComponentJob;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleJob;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleJobDto;
import org.entando.kubernetes.model.digitalexchange.JobStatus;
import org.entando.kubernetes.model.digitalexchange.JobType;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage.NoUsageComponent;
import org.entando.kubernetes.model.link.EntandoAppPluginLink;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClientResponseException;
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
                TestAppConfiguration.class
        })
@ActiveProfiles({"test"})
@Tag("component")
@WithMockUser
public class InstallFlowTest {

    private final UriBuilder ALL_COMPONENTS_ENDPOINT = UriComponentsBuilder.newInstance().pathSegment("components");
    private final UriBuilder SINGLE_COMPONENT_ENDPOINT = UriComponentsBuilder.newInstance()
            .pathSegment("components", "todomvc");
    private final UriBuilder UNINSTALL_COMPONENT_ENDPOINT = UriComponentsBuilder.newInstance()
            .pathSegment("components", "todomvc", "uninstall");
    private final UriBuilder INSTALL_COMPONENT_ENDPOINT = UriComponentsBuilder.newInstance()
            .pathSegment("components", "todomvc", "install");

    private static final String JOBS_ENDPOINT = "/jobs";
    private static final String MOCK_BUNDLE_NAME = "bundle.tgz";
    private static final Duration MAX_WAITING_TIME_FOR_JOB_STATUS = Duration.ofMinutes(45);
    private static final Duration AWAITILY_DEFAULT_POLL_INTERVAL = Duration.ofSeconds(1);

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private DatabaseCleaner databaseCleaner;

    @Autowired
    private K8SServiceClient k8SServiceClient;

    @Autowired
    private EntandoBundleComponentJobRepository jobComponentRepository;

    @Autowired
    private EntandoBundleJobRepository jobRepository;

    @Autowired
    private InstalledEntandoBundleRepository installedCompRepo;

    @MockBean
    private GitBundleDownloader gitBundleDownloader;

    @MockBean
    private EntandoCoreClient coreClient;

    @BeforeEach
    public void setup() {
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
    }

    @Test
    public void shouldReturnNotFoundWhenBundleDoesntExists() throws Exception {
        mockMvc.perform(post(INSTALL_COMPONENT_ENDPOINT.build()))
                .andDo(print()).andExpect(status().isNotFound());
    }

    @Test
    public void shouldCallCoreToInstallComponents() throws Exception {
        simulateSuccessfullyCompletedInstall();

        K8SServiceClientTestDouble k8SServiceClientTestDouble = (K8SServiceClientTestDouble) k8SServiceClient;
        // Verify interaction with mocks
        List<EntandoAppPluginLink> createdLinks = k8SServiceClientTestDouble.getInMemoryLinkCopy();
        Optional<EntandoAppPluginLink> appPluginLinkForTodoMvc = createdLinks.stream()
                .filter(link -> link.getSpec().getEntandoPluginName().equals("todomvc")).findAny();

        assertTrue(appPluginLinkForTodoMvc.isPresent());

        verifyWidgetsRequests(coreClient);
        verifyPageModelsRequests(coreClient);
        verifyDirectoryRequests(coreClient);
        verifyFileRequests(coreClient);
        verifyFragmentRequests(coreClient);
        verifyPageRequests(coreClient);

        verify(coreClient, times(1)).registerContentType(any());
        verify(coreClient, times(2)).registerContentModel(any());
        verify(coreClient, times(1)).registerLabel(any());


    }

    private void verifyPageRequests(EntandoCoreClient coreClient) {
        ArgumentCaptor<PageDescriptor> pag = ArgumentCaptor.forClass(PageDescriptor.class);
        verify(coreClient, times(1)).registerPage(pag.capture());

        List<PageDescriptor> allPageRequests = pag.getAllValues()
                .stream().sorted(Comparator.comparing(PageDescriptor::getCode))
                .collect(Collectors.toList());

        assertThat(allPageRequests.get(0)).matches(pd ->  pd.getCode().equals("my-page") &&
                pd.getTitles().get("it").equals("La mia pagina") &&
                pd.getTitles().get("en").equals("My page") &&
                pd.getPageModel().equals("service") &&
                pd.getOwnerGroup().equals("administrators") &&
                pd.getJoinGroups().containsAll(Arrays.asList("free", "customers", "developers")) &&
                pd.isDisplayedInMenu() &&
                !pd.isSeo() &&
                pd.getStatus().equals("published")
        );
    }

    private void verifyFragmentRequests(EntandoCoreClient coreClient) {
        ArgumentCaptor<FragmentDescriptor> fragmentDescArgCapt = ArgumentCaptor.forClass(FragmentDescriptor.class);
        verify(coreClient, times(2)).registerFragment(fragmentDescArgCapt.capture());
        List<FragmentDescriptor> allFragmentsRequests = fragmentDescArgCapt.getAllValues()
                .stream().sorted(Comparator.comparing(FragmentDescriptor::getCode))
                .collect(Collectors.toList());

        assertThat(allFragmentsRequests.get(0).getCode()).isEqualTo("another_fragment");
        assertThat(allFragmentsRequests.get(0).getGuiCode()).isEqualTo(readFile("/bundle/fragments/fragment.ftl"));
        assertThat(allFragmentsRequests.get(1).getCode()).isEqualTo("title_fragment");
        assertThat(allFragmentsRequests.get(1).getGuiCode()).isEqualTo("<h2>Bundle 1 Fragment</h2>");

    }

    private void verifyFileRequests(EntandoCoreClient coreClient) throws Exception{
        ArgumentCaptor<FileDescriptor> fileArgCaptor = ArgumentCaptor.forClass(FileDescriptor.class);
        verify(coreClient, times(5)).uploadFile(fileArgCaptor.capture());

        List<FileDescriptor> allPassedFiles = fileArgCaptor.getAllValues()
                .stream().sorted(Comparator.comparing(fd -> (fd.getFolder() + fd.getFilename())))
                .collect(Collectors.toList());

        assertThat(allPassedFiles.get(0)).matches(fd -> fd.getFilename().equals("custom.css") &&
                fd.getFolder().equals("/todomvc/css") &&
                fd.getBase64().equals(readFileAsBase64("/bundle/resources/css/custom.css")));
        assertThat(allPassedFiles.get(1)).matches(fd -> fd.getFilename().equals("style.css") &&
                fd.getFolder().equals("/todomvc/css") &&
                fd.getBase64().equals(readFileAsBase64("/bundle/resources/css/style.css")));
        assertThat(allPassedFiles.get(2)).matches(fd -> fd.getFilename().equals("configUiScript.js") &&
                fd.getFolder().equals("/todomvc/js") &&
                fd.getBase64().equals(readFileAsBase64("/bundle/resources/js/configUiScript.js")));
        assertThat(allPassedFiles.get(3)).matches(fd -> fd.getFilename().equals("script.js") &&
                fd.getFolder().equals("/todomvc/js") &&
                fd.getBase64().equals(readFileAsBase64("/bundle/resources/js/script.js")));
    }

    private void verifyDirectoryRequests(EntandoCoreClient coreClient) {
        ArgumentCaptor<String> folderArgCaptor = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(5)).createFolder(folderArgCaptor.capture());

        List<String> allPassedFolders = folderArgCaptor.getAllValues()
                .stream().sorted(Comparator.comparing(String::toLowerCase))
                .collect(Collectors.toList());

        assertThat(allPassedFolders.get(0)).isEqualTo("/todomvc");
        assertThat(allPassedFolders.get(1)).isEqualTo("/todomvc/css");
        assertThat(allPassedFolders.get(2)).isEqualTo("/todomvc/js");
    }

    private void verifyPageModelsRequests(EntandoCoreClient coreClient) throws Exception {
        ArgumentCaptor<PageModelDescriptor> pageModelDescrArgCaptor = ArgumentCaptor.forClass(PageModelDescriptor.class);
        verify(coreClient, times(2)).registerPageModel(pageModelDescrArgCaptor.capture());

        List<PageModelDescriptor> allPassedPageModels = pageModelDescrArgCaptor.getAllValues()
                .stream().sorted(Comparator.comparing(PageModelDescriptor::getCode))
                .collect(Collectors.toList());

        assertThat(allPassedPageModels.get(0).getCode()).isEqualTo("todomvc_another_page_model");
        assertThat(allPassedPageModels.get(0).getDescription()).isEqualTo("TODO MVC another page model");
        assertThat(allPassedPageModels.get(0).getConfiguration().getFrames().get(0))
                .matches(f -> f.getPos().equals("0") && f.getDescription().equals("Simple Frame"));
        assertThat(allPassedPageModels.get(0).getTemplate()).isEqualTo(readFile("/bundle/pagemodels/page.ftl"));

        assertThat(allPassedPageModels.get(1).getCode()).isEqualTo("todomvc_page_model");
        assertThat(allPassedPageModels.get(1).getDescription()).isEqualTo("TODO MVC basic page model");
        assertThat(allPassedPageModels.get(1).getConfiguration().getFrames().get(0))
                .matches(f -> f.getPos().equals("0") &&
                       f.getDescription().equals("Header") &&
                        f.getSketch().getX1().equals("0") &&
                        f.getSketch().getY1().equals("0") &&
                        f.getSketch().getX2().equals("11") &&
                        f.getSketch().getY2().equals("0"));
        assertThat(allPassedPageModels.get(1).getConfiguration().getFrames().get(1))
                .matches(f -> f.getPos().equals("1") &&
                        f.getDescription().equals("Breadcrumb") &&
                        f.getSketch().getX1().equals("0") &&
                        f.getSketch().getY1().equals("1") &&
                        f.getSketch().getX2().equals("11") &&
                        f.getSketch().getY2().equals("1"));
    }

    private void verifyWidgetsRequests(EntandoCoreClient coreClient) throws Exception {
        ArgumentCaptor<WidgetDescriptor> widgetDescArgCaptor = ArgumentCaptor.forClass(WidgetDescriptor.class);
        verify(coreClient, times(2)).registerWidget(widgetDescArgCaptor.capture());
        List<WidgetDescriptor> allPassedWidgets = widgetDescArgCaptor.getAllValues()
                .stream().sorted(Comparator.comparing(WidgetDescriptor::getCode))
                .collect(Collectors.toList());

        assertThat(allPassedWidgets.get(0).getCode()).isEqualTo("another_todomvc_widget");
        assertThat(allPassedWidgets.get(0).getGroup()).isEqualTo("free");
        assertThat(allPassedWidgets.get(0).getCustomUi()).isEqualTo(readFile("/bundle/widgets/widget.ftl"));

        assertThat(allPassedWidgets.get(1).getCode()).isEqualTo("todomvc_widget");
        assertThat(allPassedWidgets.get(1).getGroup()).isEqualTo("free");
        assertThat(allPassedWidgets.get(1).getCustomUi()).isEqualTo("<h2>Bundle 1 Widget</h2>");
    }

    @Test
    public void shouldRecordJobStatusAndComponentsForAuditingWhenInstallComponents() throws Exception {
        simulateSuccessfullyCompletedInstall();

        List<EntandoBundleJob> jobs = jobRepository.findAll();
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).getStatus()).isEqualByComparingTo(JobStatus.INSTALL_COMPLETED);

        List<EntandoBundleComponentJob> jobComponentList = jobComponentRepository.findAllByJob(jobs.get(0));
        assertThat(jobComponentList).hasSize(22);
        List<String> jobComponentNames = jobComponentList.stream().map(EntandoBundleComponentJob::getName)
                .collect(Collectors.toList());
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
        for (EntandoBundleComponentJob jcomp : jobComponentList) {
            Integer n = jobComponentTypes.getOrDefault(jcomp.getComponentType(), 0);
            jobComponentTypes.put(jcomp.getComponentType(), n + 1);
        }

        Map<ComponentType, Integer> expectedComponents = new HashMap<>();
        expectedComponents.put(ComponentType.WIDGET, 2);
        expectedComponents.put(ComponentType.RESOURCE, 10);
        expectedComponents.put(ComponentType.PAGE_TEMPLATE, 2);
        expectedComponents.put(ComponentType.CONTENT_TYPE, 1);
        expectedComponents.put(ComponentType.CONTENT_TEMPLATE, 2);
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

        ArgumentCaptor<String> ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(2)).deleteWidget(ac.capture());
        assertTrue(ac.getAllValues().containsAll(Arrays.asList("todomvc_widget", "another_todomvc_widget")));

        ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(2)).deletePageModel(ac.capture());
        assertTrue(ac.getAllValues().containsAll(Arrays.asList("todomvc_page_model", "todomvc_another_page_model")));

        ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(1)).deleteFolder(ac.capture());
        assertEquals("/todomvc", ac.getValue());

        ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(2)).deleteFragment(ac.capture());
        assertTrue(ac.getAllValues().containsAll(Arrays.asList("title_fragment", "another_fragment")));

        ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(1)).deletePage(ac.capture());
        assertEquals("my-page", ac.getValue());

        verifyJobHasComponentAndStatus(uninstallJobId, JobStatus.UNINSTALL_COMPLETED);

    }

    @Test
    public void shouldRecordJobStatusAndComponentsForAuditingWhenUninstallComponents() throws Exception {
        String jobId = simulateSuccessfullyCompletedInstall();

        verifyJobHasComponentAndStatus(jobId, JobStatus.INSTALL_COMPLETED);

        simulateSuccessfullyCompletedUninstall();
        List<EntandoBundleJob> jobs = jobRepository.findAll();
        assertThat(jobs).hasSize(2);
        assertThat(jobs.get(0).getStatus()).isEqualByComparingTo(JobStatus.INSTALL_COMPLETED);
        assertThat(jobs.get(1).getStatus()).isEqualByComparingTo(JobStatus.UNINSTALL_COMPLETED);

        List<EntandoBundleComponentJob> installedComponentList = jobComponentRepository.findAllByJob(jobs.get(0));
        List<EntandoBundleComponentJob> uninstalledComponentList = jobComponentRepository.findAllByJob(jobs.get(1));
        assertThat(uninstalledComponentList).hasSize(installedComponentList.size());
        List<JobStatus> jobComponentStatus = uninstalledComponentList.stream().map(EntandoBundleComponentJob::getStatus)
                .collect(Collectors.toList());
        assertThat(jobComponentStatus).allMatch((jcs) -> jcs.equals(JobStatus.UNINSTALL_COMPLETED));

        boolean matchFound = false;
        for (EntandoBundleComponentJob ic : installedComponentList) {
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
    public void installedComponentShouldReturnInstalledFieldTrueAndEntryInTheInstalledComponentDatabase()
            throws Exception {

        simulateSuccessfullyCompletedInstall();

        mockMvc.perform(get(ALL_COMPONENTS_ENDPOINT.build()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").isArray())
                .andExpect(jsonPath("$.payload", hasSize(1)))
                .andExpect(jsonPath("$.payload[0].id").value("todomvc"))
                .andExpect(jsonPath("$.payload[0].installed").value("true"));

        List<EntandoBundle> installedComponents = installedCompRepo.findAll();
        assertThat(installedComponents).hasSize(1);
        assertThat(installedComponents.get(0).getId()).isEqualTo("todomvc");
        assertThat(installedComponents.get(0).isInstalled()).isEqualTo(true);
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

        Optional<EntandoBundleJob> job = jobRepository.findById(UUID.fromString(failingJobId));
        assertThat(job.isPresent()).isTrue();

        // And for each installed component job there should be a component job that rollbacked the install
        List<EntandoBundleComponentJob> jobRelatedComponents = jobComponentRepository.findAllByJob(job.get());
        List<EntandoBundleComponentJob> installedComponents = jobRelatedComponents.stream().filter(j ->
                j.getStatus().equals(JobStatus.INSTALL_COMPLETED)).collect(
                Collectors.toList());
        for (EntandoBundleComponentJob c : installedComponents) {
            List<EntandoBundleComponentJob> jobs = jobRelatedComponents.stream().filter(j ->
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
        mockMvc.perform(get("/jobs?filters[0].attribute=componentId&filters[0].operator=eq&filters[0].value=todomvc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").isArray())
                .andExpect(jsonPath("$.payload.*.id", hasSize(3)))
                .andExpect(jsonPath("$.payload.*.id").value(containsInAnyOrder(
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
//        mockMvc.perform(post(UNINSTALL_COMPONENT_ENDPOINT.build()))
//                .andExpect(status().isConflict())
//                .andExpect(content().string(containsString("JOB ID: " + jobId)));
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

        Mockito.reset(coreClient);
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
//        stubFor(WireMock.post(urlMatching("/entando-app/api/.*")).willReturn(aResponse().withStatus(200)));

        MvcResult result = mockMvc.perform(post(INSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isOk())
                .andReturn();

        waitForInstallStatus(JobStatus.INSTALL_COMPLETED);

        return JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");
    }


    private String simulateSuccessfullyCompletedUninstall() throws Exception {

        Mockito.reset(coreClient);
        WireMock.reset();
        WireMock.setGlobalFixedDelay(0);

        stubFor(WireMock.post(urlEqualTo("/auth/protocol/openid-connect/auth"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"access_token\": \"iddqd\" }")));
        when(coreClient.getWidgetUsage(anyString())).thenReturn(new NoUsageComponent(ComponentType.WIDGET));
        when(coreClient.getPageUsage(anyString())).thenReturn(new NoUsageComponent(ComponentType.PAGE));
        when(coreClient.getPageModelUsage(anyString())).thenReturn(new NoUsageComponent(ComponentType.PAGE_TEMPLATE));
        when(coreClient.getFragmentUsage(anyString())).thenReturn(new NoUsageComponent(ComponentType.FRAGMENT));
        when(coreClient.getContentTypeUsage(anyString())).thenReturn(new NoUsageComponent(ComponentType.CONTENT_TYPE));
//        stubFor(WireMock.delete(urlMatching("/entando-app/api/.*")).willReturn(aResponse().withStatus(200)));
        stubFor(WireMock.get(urlMatching("/k8s/.*")).willReturn(aResponse().withStatus(200)));

        MvcResult result = mockMvc.perform(post(UNINSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isOk())
                .andReturn();

        waitForUninstallStatus(JobStatus.UNINSTALL_COMPLETED);

        return JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");
    }

    private String simulateFailingInstall() throws Exception {

        Mockito.reset(coreClient);
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
        doThrow(new RestClientResponseException("error", 500, "Error", null, null, null))
                .when(coreClient).registerPage(any(PageDescriptor.class));
//        stubFor(WireMock.delete(urlMatching("/entando-app/api.*")).willReturn(aResponse().withStatus(200)));
//        stubFor(WireMock.post(urlMatching("/entando-app/api/.*")).willReturn(aResponse().withStatus(200)));
//        stubFor(WireMock.post(urlMatching("/entando-app/api/pages/?")).willReturn(aResponse().withStatus(500)));

        MvcResult result = mockMvc.perform(post(INSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isOk())
                .andReturn();

        waitForPossibleStatus(JobStatus.INSTALL_ROLLBACK, JobStatus.INSTALL_ERROR);

        return JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");
    }

    private String simulateFailingUninstall() throws Exception {

        Mockito.reset(coreClient);
        WireMock.reset();
        WireMock.setGlobalFixedDelay(0);

        stubFor(WireMock.post(urlEqualTo("/auth/protocol/openid-connect/auth"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"access_token\": \"iddqd\" }")));
        when(coreClient.getWidgetUsage(anyString())).thenReturn(new NoUsageComponent(ComponentType.WIDGET));
        when(coreClient.getPageUsage(anyString())).thenReturn(new NoUsageComponent(ComponentType.PAGE));
        when(coreClient.getPageModelUsage(anyString())).thenReturn(new NoUsageComponent(ComponentType.PAGE_TEMPLATE));
        when(coreClient.getFragmentUsage(anyString())).thenReturn(new NoUsageComponent(ComponentType.FRAGMENT));
        when(coreClient.getContentTypeUsage(anyString())).thenReturn(new NoUsageComponent(ComponentType.CONTENT_TYPE));
        doThrow(new RestClientResponseException("error", 500, "error", null, null, null)).when(coreClient).deleteFolder(any());
        doThrow(new RestClientResponseException("error", 500, "error", null, null, null)).when(coreClient).deleteContentModel(any());
        doThrow(new RestClientResponseException("error", 500, "error", null, null, null)).when(coreClient).deleteContentType(any());
        doThrow(new RestClientResponseException("error", 500, "error", null, null, null)).when(coreClient).deleteFragment(any());
        doThrow(new RestClientResponseException("error", 500, "error", null, null, null)).when(coreClient).deleteWidget(any());
        doThrow(new RestClientResponseException("error", 500, "error", null, null, null)).when(coreClient).deletePage(any());
        doThrow(new RestClientResponseException("error", 500, "error", null, null, null)).when(coreClient).deletePageModel(any());
        doThrow(new RestClientResponseException("error", 500, "error", null, null, null)).when(coreClient).deleteLabel(any());

        stubFor(WireMock.get(urlMatching("/k8s/.*")).willReturn(aResponse().withStatus(200)));

        MvcResult result = mockMvc.perform(post(UNINSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isOk())
                .andReturn();
        waitForUninstallStatus(JobStatus.UNINSTALL_ERROR);

        return JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");

    }

    private String simulateInProgressInstall() throws Exception {

        Mockito.reset(coreClient);
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

        doSleep(Duration.ofMillis(200)).when(coreClient).registerPage(any());
        doSleep(Duration.ofMillis(200)).when(coreClient).registerPageModel(any());
        doSleep(Duration.ofMillis(200)).when(coreClient).registerWidget(any());
        doSleep(Duration.ofMillis(200)).when(coreClient).registerFragment(any());
        doSleep(Duration.ofMillis(200)).when(coreClient).registerContentType(any());
        doSleep(Duration.ofMillis(200)).when(coreClient).registerContentModel(any());
        doSleep(Duration.ofMillis(200)).when(coreClient).registerLabel(any());
        doSleep(Duration.ofMillis(200)).when(coreClient).createFolder(any());
        doSleep(Duration.ofMillis(200)).when(coreClient).uploadFile(any());

        stubFor(WireMock.get(urlMatching("/k8s/.*")).willReturn(aResponse().withStatus(200)));

        MvcResult result = mockMvc.perform(post(INSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isOk())
                .andReturn();

        waitForInstallStatus(JobStatus.INSTALL_IN_PROGRESS);

        return JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");
    }

    private String simulateInProgressUninstall() throws Exception {

        Mockito.reset(coreClient);
        WireMock.reset();
        WireMock.setGlobalFixedDelay(1000);

        stubFor(WireMock.post(urlEqualTo("/auth/protocol/openid-connect/auth"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"access_token\": \"iddqd\" }")));
        when(coreClient.getWidgetUsage(anyString())).thenReturn(new NoUsageComponent(ComponentType.WIDGET));
        when(coreClient.getPageUsage(anyString())).thenReturn(new NoUsageComponent(ComponentType.PAGE));
        when(coreClient.getPageModelUsage(anyString())).thenReturn(new NoUsageComponent(ComponentType.PAGE_TEMPLATE));
        when(coreClient.getFragmentUsage(anyString())).thenReturn(new NoUsageComponent(ComponentType.FRAGMENT));
        when(coreClient.getContentTypeUsage(anyString())).thenReturn(new NoUsageComponent(ComponentType.CONTENT_TYPE));
        doSleep(Duration.ofSeconds(1)).when(coreClient).deletePage(any());
        doSleep(Duration.ofSeconds(1)).when(coreClient).deletePageModel(any());
        doSleep(Duration.ofSeconds(1)).when(coreClient).deleteWidget(any());
        doSleep(Duration.ofSeconds(1)).when(coreClient).deleteFragment(any());
        doSleep(Duration.ofSeconds(1)).when(coreClient).deleteContentType(any());
        doSleep(Duration.ofSeconds(1)).when(coreClient).deleteContentModel(any());
        doSleep(Duration.ofSeconds(1)).when(coreClient).deleteLabel(any());
        doSleep(Duration.ofSeconds(1)).when(coreClient).deleteFolder(any());


        MvcResult result = mockMvc.perform(post(UNINSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isOk())
                .andReturn();
        waitForUninstallStatus(JobStatus.UNINSTALL_IN_PROGRESS);

        return JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");


    }

    private void waitForPossibleStatus(JobStatus... statuses) {
        await().atMost(MAX_WAITING_TIME_FOR_JOB_STATUS)
                .pollInterval(AWAITILY_DEFAULT_POLL_INTERVAL)
                .until(() -> Arrays.asList(statuses).contains(getComponentLastJobStatusOfType("todomvc", JobType.INSTALL.getStatuses())));
    }

    private void waitForInstallStatus(JobStatus status) {
        await().atMost(MAX_WAITING_TIME_FOR_JOB_STATUS)
                .pollInterval(AWAITILY_DEFAULT_POLL_INTERVAL)
                .until(() -> getComponentLastJobStatusOfType("todomvc", JobType.INSTALL.getStatuses()).equals(status));
    }

    private void waitForUninstallStatus(JobStatus status) {
        await().atMost(MAX_WAITING_TIME_FOR_JOB_STATUS)
                .pollInterval(AWAITILY_DEFAULT_POLL_INTERVAL)
                .until(() -> getComponentLastJobStatusOfType("todomvc", JobType.UNINSTALL.getStatuses()).equals(status));
    }

    private PagedMetadata<EntandoBundleJob> getInstallJob() throws Exception {
        return mapper.readValue(mockMvc.perform(get(JOBS_ENDPOINT + "?component=todomvc&type=INSTALL"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<PagedMetadata<EntandoBundleJob>>() {
                });
    }

    private PagedMetadata<EntandoBundleJob> getUninstallJob() throws Exception {
        List<String> allowedValues = JobType.UNINSTALL.getStatuses().stream().map(JobStatus::name).collect(Collectors.toList());
        return mapper.readValue(mockMvc.perform(get("/jobs"
                        + "?sort=startedAt"
                        + "&direction=DESC"
                        + "&filters[0].attribute=status&filters[0].operator=eq&filters[0].allowedValues="+String.join(",", allowedValues)
                        + "&filters[1].attribute=componentId&filters[1].operator=eq&filters[1].value=todomvc"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<PagedMetadata<EntandoBundleJob>>() {
                });
    }

    private JobStatus getComponentLastJobStatusOfType(String component, Set<JobStatus> possibleStatues) throws Exception {
        List<String> allowedValues = possibleStatues.stream().map(JobStatus::name).collect(Collectors.toList());
        MockHttpServletResponse response = mockMvc.perform(get("/jobs"
                + "?sort=startedAt"
                + "&direction=DESC"
                + "&pageSize=1"
                + "&filters[0].attribute=status&filters[0].operator=eq&filters[0].allowedValues="+String.join(",", allowedValues)
                + "&filters[1].attribute=componentId&filters[1].operator=eq&filters[1].value="+component)
                .with(user("user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").value(hasSize(1)))
                .andExpect(jsonPath("$.payload.[0].componentId").value(component))
                .andReturn().getResponse();
        return JobStatus.valueOf(JsonPath.read(response.getContentAsString(), "$.payload.[0].status"));
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
                .withIntegrity(
                        "sha512-n4TEroSqg/sZlEGg2xj6RKNtl/t3ZROYdNd99/dl3UrzCUHvBrBxZ1rxQg/sl3kmIYgn3+ogbIFmUZYKWxG3Ag==")
                .withShasum("4d80130d7d651176953b5ce470c3a6f297a70815")
                .withTarball("http://localhost:8099/repository/npm-internal/inail_bundle/-/inail_bundle-0.0.1.tgz")
                .endTag()
                .build();
    }

}
