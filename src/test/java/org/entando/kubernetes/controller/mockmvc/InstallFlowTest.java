package org.entando.kubernetes.controller.mockmvc;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.entando.kubernetes.DigitalExchangeTestUtils.readFile;
import static org.entando.kubernetes.DigitalExchangeTestUtils.readFileAsBase64;
import static org.entando.kubernetes.model.EntandoDeploymentPhase.FAILED;
import static org.entando.kubernetes.utils.TestInstallUtils.ALL_COMPONENTS_ENDPOINT;
import static org.entando.kubernetes.utils.TestInstallUtils.INSTALL_COMPONENT_ENDPOINT;
import static org.entando.kubernetes.utils.TestInstallUtils.UNINSTALL_COMPONENT_ENDPOINT;
import static org.entando.kubernetes.utils.TestInstallUtils.getAnalysisReport;
import static org.entando.kubernetes.utils.TestInstallUtils.getJobStatus;
import static org.entando.kubernetes.utils.TestInstallUtils.mockAnalysisReport;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.entando.kubernetes.DatabaseCleaner;
import org.entando.kubernetes.EntandoKubernetesJavaApplication;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.config.TestAppConfiguration;
import org.entando.kubernetes.config.TestKubernetesConfig;
import org.entando.kubernetes.config.TestSecurityConfiguration;
import org.entando.kubernetes.controller.digitalexchange.job.model.AnalysisReport;
import org.entando.kubernetes.exception.digitalexchange.BundleOperationConcurrencyException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.AssetDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.CategoryDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FragmentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.GroupDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.LabelDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.LanguageDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageTemplateDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.content.ContentDescriptor;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloader;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderFactory;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.bundle.reportable.AnalysisReportFunction;
import org.entando.kubernetes.model.bundle.reportable.ReportableComponentProcessor;
import org.entando.kubernetes.model.bundle.reportable.ReportableRemoteHandler;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.model.job.EntandoBundleEntity;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.model.job.JobType;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
import org.entando.kubernetes.service.digitalexchange.concurrency.BundleOperationsConcurrencyManager;
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
        })
@ActiveProfiles({"test"})
@Tag("component")
@WithMockUser
//Sonar doesn't pick up MockMVC assertions
@SuppressWarnings("java:S2699")
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
    private List<ReportableComponentProcessor> reportableComponentProcessorList;

    @Autowired
    private Map<ReportableRemoteHandler, AnalysisReportFunction> analysisReportStrategies;

    @Autowired
    private BundleDownloaderFactory downloaderFactory;

    @MockBean
    private K8SServiceClient k8SServiceClient;

    @MockBean
    private EntandoCoreClient coreClient;

    @MockBean
    private BundleOperationsConcurrencyManager bundleOperationsConcurrencyManager;


    private Supplier<BundleDownloader> defaultBundleDownloaderSupplier;

    private static final String MOCK_BUNDLE_NAME = "bundle.tgz";

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
        downloaderFactory.setDefaultSupplier(defaultBundleDownloaderSupplier);
    }

    @Test
    public void shouldReturnNotFoundWhenBundleDoesntExists() throws Exception {
        mockMvc.perform(post(INSTALL_COMPONENT_ENDPOINT.build()))
                .andDo(print()).andExpect(status().isNotFound());
    }

    @Test
    void shouldCallCoreToInstallComponents() throws Exception {
        simulateSuccessfullyCompletedInstall();

        verifyPluginInstallRequests(k8SServiceClient);
        verifyWidgetsInstallRequests(coreClient);
        verifyCategoryInstallRequests(coreClient);
        verifyGroupInstallRequests(coreClient);
        verifyPageModelsInstallRequests(coreClient);
        verifyLanguagesInstallRequests(coreClient);
        verifyLabelsInstallRequests(coreClient);
        verifyDirectoryInstallRequests(coreClient);
        verifyFileInstallRequests(coreClient);
        verifyFragmentInstallRequests(coreClient);
        verifyPageInstallRequests(coreClient);
        verifyPageConfigurationInstallRequests(coreClient);
        verifyContentTypesInstallRequests(coreClient);
        verifyContentTemplatesInstallRequests(coreClient);
        verifyContentsInstallRequests(coreClient);
        verifyAssetsInstallRequests(coreClient);
    }

    private void verifyCategoryInstallRequests(EntandoCoreClient coreClient) {
        ArgumentCaptor<CategoryDescriptor> categoryDescriptor = ArgumentCaptor.forClass(CategoryDescriptor.class);
        verify(coreClient, times(2)).createCategory(categoryDescriptor.capture());

        List<CategoryDescriptor> allRequests = categoryDescriptor.getAllValues()
                .stream().sorted(Comparator.comparing(CategoryDescriptor::getCode))
                .collect(Collectors.toList());

        assertThat(allRequests.get(0)).matches(d ->
                d.getCode().equals("another_category")
                        && d.getParentCode().equals("my-category")
                        && d.getTitles().containsKey("it")
                        && d.getTitles().get("it").equals("Altra categoria")
                        && d.getTitles().containsKey("en")
                        && d.getTitles().get("en").equals("Another category"));

        assertThat(allRequests.get(1)).matches(d ->
                d.getCode().equals("my-category")
                        && d.getParentCode().equals("home")
                        && d.getTitles().containsKey("it")
                        && d.getTitles().get("it").equals("La mia categoria")
                        && d.getTitles().containsKey("en")
                        && d.getTitles().get("en").equals("My own category"));
    }

    private void verifyGroupInstallRequests(EntandoCoreClient coreClient) {
        ArgumentCaptor<GroupDescriptor> groupDescriptor = ArgumentCaptor.forClass(GroupDescriptor.class);
        verify(coreClient, times(2)).createGroup(groupDescriptor.capture());

        List<GroupDescriptor> allPageRequests = groupDescriptor.getAllValues()
                .stream().sorted(Comparator.comparing(GroupDescriptor::getCode))
                .collect(Collectors.toList());

        assertThat(allPageRequests.get(0)).matches(pd -> pd.getCode().equals("ecr")
                && pd.getName().equals("Ecr"));

        assertThat(allPageRequests.get(1)).matches(pd -> pd.getCode().equals("ps")
                && pd.getName().equals("Professional Services"));
    }

    private void verifyPageInstallRequests(EntandoCoreClient coreClient) {
        ArgumentCaptor<PageDescriptor> pag = ArgumentCaptor.forClass(PageDescriptor.class);
        verify(coreClient, times(2)).createPage(pag.capture());

        List<PageDescriptor> allPageRequests = pag.getAllValues()
                .stream().sorted(Comparator.comparing(PageDescriptor::getCode))
                .collect(Collectors.toList());

        assertThat(allPageRequests.get(0)).matches(pd -> pd.getCode().equals("another-page")
                && pd.getParentCode().equals("homepage")
                && pd.getTitles().get("it").equals("La mia altra pagina")
                && pd.getTitles().get("en").equals("My other page")
                && pd.getPageModel().equals("todomvc_another_page_model")
                && pd.getOwnerGroup().equals("administrators"));

        assertThat(allPageRequests.get(1)).matches(pd -> pd.getCode().equals("my-page")
                && pd.getParentCode().equals("homepage")
                && pd.getTitles().get("it").equals("La mia pagina")
                && pd.getTitles().get("en").equals("My page")
                && pd.getPageModel().equals("service")
                && pd.getOwnerGroup().equals("administrators"));

        //+1 for each page, +1 after updating each page configuration
        verify(coreClient, times(4)).setPageStatus(anyString(), eq("published"));
    }

    private void verifyPageConfigurationInstallRequests(EntandoCoreClient coreClient) {
        ArgumentCaptor<PageDescriptor> pag = ArgumentCaptor.forClass(PageDescriptor.class);
        verify(coreClient, times(2)).updatePageConfiguration(pag.capture());

        List<PageDescriptor> allPageRequests = pag.getAllValues()
                .stream().sorted(Comparator.comparing(PageDescriptor::getCode))
                .collect(Collectors.toList());

        assertThat(allPageRequests.get(0)).matches(pd -> pd.getCode().equals("another-page")
                && pd.getParentCode().equals("homepage")
                && pd.getTitles().get("it").equals("La mia altra pagina")
                && pd.getTitles().get("en").equals("My other page")
                && pd.getPageModel().equals("todomvc_another_page_model")
                && pd.getOwnerGroup().equals("administrators")
                && pd.getJoinGroups().containsAll(Arrays.asList("free", "customers", "developers", "ps"))
                && pd.isDisplayedInMenu()
                && !pd.isSeo()
                && pd.getStatus().equals("published"));

        assertThat(allPageRequests.get(1)).matches(pd -> pd.getCode().equals("my-page")
                && pd.getParentCode().equals("homepage")
                && pd.getTitles().get("it").equals("La mia pagina")
                && pd.getTitles().get("en").equals("My page")
                && pd.getPageModel().equals("service")
                && pd.getOwnerGroup().equals("administrators")
                && pd.getJoinGroups().containsAll(Arrays.asList("free", "customers", "developers"))
                && pd.isDisplayedInMenu()
                && !pd.isSeo()
                && pd.getStatus().equals("published"));

        assertThat(allPageRequests.get(1).getWidgets().get(0)).matches(wd -> wd.getCode().equals("my-code"));
    }

    private void verifyContentsInstallRequests(EntandoCoreClient coreClient) {
        ArgumentCaptor<ContentDescriptor> contentDescriptorArgCaptor = ArgumentCaptor.forClass(ContentDescriptor.class);
        verify(coreClient, times(2)).createContent(contentDescriptorArgCaptor.capture());

        List<ContentDescriptor> allPassedContents = contentDescriptorArgCaptor.getAllValues()
                .stream().sorted(Comparator.comparing(ContentDescriptor::getId))
                .collect(Collectors.toList());

        assertThat(allPassedContents.get(0).getId()).isEqualTo("CNG102");
        assertThat(allPassedContents.get(0).getTypeCode()).isEqualTo("CNG");
        assertThat(allPassedContents.get(0).getDescription()).isEqualTo("Interest 3 card title - 2nd banner");
        assertThat(allPassedContents.get(0).getMainGroup()).isEqualTo("free");
        assertThat(allPassedContents.get(0).getViewPage()).isEqualTo("news");
        assertThat(allPassedContents.get(0).getListModel()).isEqualTo("10022");
        assertThat(allPassedContents.get(0).getDefaultModel()).isEqualTo("10003");

        assertThat(allPassedContents.get(1).getId()).isEqualTo("CNT103");
        assertThat(allPassedContents.get(1).getTypeCode()).isEqualTo("CNT");
        assertThat(allPassedContents.get(1).getDescription()).isEqualTo("Interest 3 card title - 2nd banner");
        assertThat(allPassedContents.get(1).getMainGroup()).isEqualTo("free");

        verify(coreClient, times(2)).publishContent(any());
    }

    private void verifyFragmentInstallRequests(EntandoCoreClient coreClient) {
        ArgumentCaptor<FragmentDescriptor> fragmentDescArgCapt = ArgumentCaptor.forClass(FragmentDescriptor.class);
        verify(coreClient, times(2)).createFragment(fragmentDescArgCapt.capture());
        List<FragmentDescriptor> allFragmentsRequests = fragmentDescArgCapt.getAllValues()
                .stream().sorted(Comparator.comparing(FragmentDescriptor::getCode))
                .collect(Collectors.toList());

        assertThat(allFragmentsRequests.get(0).getCode()).isEqualTo("another_fragment");
        assertThat(allFragmentsRequests.get(0).getGuiCode()).isEqualTo(readFile("/bundle/fragments/fragment.ftl"));
        assertThat(allFragmentsRequests.get(1).getCode()).isEqualTo("title_fragment");
        assertThat(allFragmentsRequests.get(1).getGuiCode()).isEqualTo("<h2>Bundle 1 Fragment</h2>");

    }

    private void verifyFileInstallRequests(EntandoCoreClient coreClient) throws Exception {
        ArgumentCaptor<FileDescriptor> fileArgCaptor = ArgumentCaptor.forClass(FileDescriptor.class);
        verify(coreClient, times(5)).createFile(fileArgCaptor.capture());

        List<FileDescriptor> allPassedFiles = fileArgCaptor.getAllValues()
                .stream().sorted(Comparator.comparing(fd -> (fd.getFolder() + fd.getFilename())))
                .collect(Collectors.toList());

        assertThat(allPassedFiles.get(0)).matches(fd -> fd.getFilename().equals("custom.css")
                && fd.getFolder().equals("/something/css")
                && fd.getBase64().equals(readFileAsBase64("/bundle/resources/css/custom.css")));
        assertThat(allPassedFiles.get(1)).matches(fd -> fd.getFilename().equals("style.css")
                && fd.getFolder().equals("/something/css")
                && fd.getBase64().equals(readFileAsBase64("/bundle/resources/css/style.css")));
        assertThat(allPassedFiles.get(2)).matches(fd -> fd.getFilename().equals("configUiScript.js")
                && fd.getFolder().equals("/something/js")
                && fd.getBase64().equals(readFileAsBase64("/bundle/resources/js/configUiScript.js")));
        assertThat(allPassedFiles.get(3)).matches(fd -> fd.getFilename().equals("script.js")
                && fd.getFolder().equals("/something/js")
                && fd.getBase64().equals(readFileAsBase64("/bundle/resources/js/script.js")));
    }


    private void verifyLanguagesInstallRequests(EntandoCoreClient coreClient) {
        ArgumentCaptor<LanguageDescriptor> languageArgCaptor = ArgumentCaptor.forClass(LanguageDescriptor.class);
        verify(coreClient, times(2)).enableLanguage(languageArgCaptor.capture());

        List<LanguageDescriptor> languageDescriptorList = languageArgCaptor.getAllValues().stream()
                .sorted(Comparator.comparing(langDescriptor -> langDescriptor.getCode().toLowerCase()))
                .collect(Collectors.toList());

        assertThat(languageDescriptorList.get(0).getCode()).isEqualTo("en");
        assertThat(languageDescriptorList.get(0).getDescription()).isEqualTo("English");
        assertThat(languageDescriptorList.get(1).getCode()).isEqualTo("it");
        assertThat(languageDescriptorList.get(1).getDescription()).isEqualTo("Italiano");
    }


    private void verifyLabelsInstallRequests(EntandoCoreClient coreClient) {
        ArgumentCaptor<LabelDescriptor> labelArgCaptor = ArgumentCaptor.forClass(LabelDescriptor.class);
        verify(coreClient, times(2)).createLabel(labelArgCaptor.capture());

        List<LabelDescriptor> allPassedLabels = labelArgCaptor.getAllValues()
                .stream().sorted(Comparator.comparing(LabelDescriptor::getKey))
                .collect(Collectors.toList());

        assertThat(allPassedLabels.get(0).getKey()).isEqualTo("HELLO");
        assertThat(allPassedLabels.get(0).getTitles()).hasSize(2);
        assertThat(allPassedLabels.get(0).getTitles()).containsEntry("it", "Ciao");
        assertThat(allPassedLabels.get(0).getTitles()).containsEntry("en", "Hello");

        assertThat(allPassedLabels.get(1).getKey()).isEqualTo("WORLD");
        assertThat(allPassedLabels.get(1).getTitles()).hasSize(2);
        assertThat(allPassedLabels.get(1).getTitles()).containsEntry("it", "Mundo");
        assertThat(allPassedLabels.get(1).getTitles()).containsEntry("en", "World");
    }

    private void verifyContentTypesInstallRequests(EntandoCoreClient coreClient) {
        verify(coreClient, times(2)).createContentType(any());
    }

    private void verifyContentTemplatesInstallRequests(EntandoCoreClient coreClient) {
        verify(coreClient, times(2)).createContentTemplate(any());
    }

    private void verifyAssetsInstallRequests(EntandoCoreClient coreClient) {
        ArgumentCaptor<AssetDescriptor> codesArgCaptor = ArgumentCaptor.forClass(AssetDescriptor.class);
        verify(coreClient, times(2)).createAsset(codesArgCaptor.capture(), isA(File.class));

        List<AssetDescriptor> allPassedAssets = codesArgCaptor.getAllValues()
                .stream().sorted(Comparator.comparing(AssetDescriptor::getCorrelationCode))
                .collect(Collectors.toList());

        assertThat(allPassedAssets.get(0).getCorrelationCode()).isEqualTo("anotherAsset");
        assertThat(allPassedAssets.get(0).getName()).isEqualTo("another_image.jpg");

        assertThat(allPassedAssets.get(1).getCorrelationCode()).isEqualTo("my_asset");
        assertThat(allPassedAssets.get(1).getName()).isEqualTo("my_image.jpg");
    }

    private void verifyDirectoryInstallRequests(EntandoCoreClient coreClient) {
        verify(coreClient, times(0)).createFolder(any());
    }

    private void verifyPluginInstallRequests(K8SServiceClient k8SServiceClient) {
        verify(k8SServiceClient, times(3)).linkAppWithPlugin(any(), any(), any());
    }

    private void verifyPageModelsInstallRequests(EntandoCoreClient coreClient) throws Exception {
        ArgumentCaptor<PageTemplateDescriptor> pageModelDescrArgCaptor = ArgumentCaptor
                .forClass(PageTemplateDescriptor.class);
        verify(coreClient, times(2)).createPageTemplate(pageModelDescrArgCaptor.capture());

        List<PageTemplateDescriptor> allPassedPageModels = pageModelDescrArgCaptor.getAllValues()
                .stream().sorted(Comparator.comparing(PageTemplateDescriptor::getCode))
                .collect(Collectors.toList());

        assertThat(allPassedPageModels.get(0).getCode()).isEqualTo("todomvc_another_page_model");
        assertThat(allPassedPageModels.get(0).getDescription()).isEqualTo("TODO MVC another page model");
        assertThat(allPassedPageModels.get(0).getConfiguration().getFrames().get(0))
                .matches(f -> f.getPos().equals("0") && f.getDescription().equals("Simple Frame"));
        assertThat(allPassedPageModels.get(0).getTemplate()).isEqualTo(readFile("/bundle/pagemodels/page.ftl"));

        assertThat(allPassedPageModels.get(1).getCode()).isEqualTo("todomvc_page_model");
        assertThat(allPassedPageModels.get(1).getDescription()).isEqualTo("TODO MVC basic page model");
        assertThat(allPassedPageModels.get(1).getConfiguration().getFrames().get(0))
                .matches(f -> f.getPos().equals("0")
                        && f.getDescription().equals("Header")
                        && f.getSketch().getX1() == 0
                        && f.getSketch().getY1() == 0
                        && f.getSketch().getX2() == 11
                        && f.getSketch().getY2() == 0);
        assertThat(allPassedPageModels.get(1).getConfiguration().getFrames().get(1))
                .matches(f -> f.getPos().equals("1")
                        && f.getDescription().equals("Breadcrumb")
                        && f.getSketch().getX1() == 0
                        && f.getSketch().getY1() == 1
                        && f.getSketch().getX2() == 11
                        && f.getSketch().getY2() == 1);
    }

    private void verifyWidgetsInstallRequests(EntandoCoreClient coreClient) throws Exception {
        ArgumentCaptor<WidgetDescriptor> widgetDescArgCaptor = ArgumentCaptor.forClass(WidgetDescriptor.class);
        verify(coreClient, times(2)).createWidget(widgetDescArgCaptor.capture());
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

        List<EntandoBundleJobEntity> jobs = jobRepository.findAll();
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).getStatus()).isEqualByComparingTo(JobStatus.INSTALL_COMPLETED);

        List<String> expected = Arrays.asList(
                // Plugins
                "entando-todomvcv1",
                "entando-todomvcv2",
                "customBaseName",
                // Directories
                "/something",
                // Categories
                "my-category",
                "another_category",
                // Groups
                "ecr",
                "ps",
                // Languages
                "it",
                "en",
                // Labels
                "HELLO",
                "WORLD",
                // Files
                "/something/css/custom.css",
                "/something/css/style.css",
                "/something/js/configUiScript.js",
                "/something/js/script.js",
                "/something/vendor/jquery/jquery.js",
                //Widgets
                "todomvc_widget",
                "another_todomvc_widget",
                // Content-Type
                "CNG",
                "CNT",
                // Content-Template
                "8880003",
                "8880002",
                // Assets
                "my_asset",
                "anotherAsset",
                // Content
                "CNG102",
                "CNT103",
                // Fragments
                "title_fragment",
                "another_fragment",
                // Page templates
                "todomvc_page_model",
                "todomvc_another_page_model",
                // Pages
                "my-page",
                "another-page",
                // Page Configurations
                "my-page",
                "another-page");

        List<EntandoBundleComponentJobEntity> jobComponentList = componentJobRepository
                .findAllByParentJob(jobs.get(0))
                .stream()
                .sorted(Comparator.comparingLong(cj -> cj.getStartedAt().toInstant(ZoneOffset.UTC).toEpochMilli()))
                .collect(Collectors.toList());
        assertThat(jobComponentList).hasSize(expected.size());
        List<String> jobComponentNames = jobComponentList.stream().map(EntandoBundleComponentJobEntity::getComponentId)
                .collect(Collectors.toList());
        assertThat(jobComponentNames).containsExactlyInAnyOrder(expected.toArray(String[]::new));

        Map<ComponentType, Integer> jobComponentTypes = new HashMap<>();
        for (EntandoBundleComponentJobEntity jcomp : jobComponentList) {
            Integer n = jobComponentTypes.getOrDefault(jcomp.getComponentType(), 0);
            jobComponentTypes.put(jcomp.getComponentType(), n + 1);
        }

        Map<ComponentType, Integer> expectedComponents = new HashMap<>();
        expectedComponents.put(ComponentType.WIDGET, 2);
        expectedComponents.put(ComponentType.RESOURCE, 5);
        expectedComponents.put(ComponentType.GROUP, 2);
        expectedComponents.put(ComponentType.CATEGORY, 2);
        expectedComponents.put(ComponentType.DIRECTORY, 1);
        expectedComponents.put(ComponentType.PAGE_TEMPLATE, 2);
        expectedComponents.put(ComponentType.CONTENT_TYPE, 2);
        expectedComponents.put(ComponentType.CONTENT_TEMPLATE, 2);
        expectedComponents.put(ComponentType.CONTENT, 2);
        expectedComponents.put(ComponentType.ASSET, 2);
        expectedComponents.put(ComponentType.LANGUAGE, 2);
        expectedComponents.put(ComponentType.LABEL, 2);
        expectedComponents.put(ComponentType.FRAGMENT, 2);
        expectedComponents.put(ComponentType.PAGE, 2);
        expectedComponents.put(ComponentType.PAGE_CONFIGURATION, 2);
        expectedComponents.put(ComponentType.PLUGIN, 3);

        assertThat(jobComponentTypes).containsAllEntriesOf(expectedComponents);
    }

    @Test
    public void shouldRecordInstallJobsInOrder() throws Exception {
        simulateSuccessfullyCompletedInstall();
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

        ArgumentCaptor<String> ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(2)).deleteWidget(ac.capture());
        assertThat(ac.getAllValues()).containsAll(Arrays.asList("todomvc_widget", "another_todomvc_widget"));

        ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(2)).deletePageModel(ac.capture());
        assertThat(ac.getAllValues()).containsAll(Arrays.asList("todomvc_page_model", "todomvc_another_page_model"));

        ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(2)).disableLanguage(ac.capture());
        assertThat(ac.getAllValues()).containsAll(Arrays.asList("it", "en"));

        ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(2)).deleteLabel(ac.capture());
        assertThat(ac.getAllValues()).containsAll(Arrays.asList("HELLO", "WORLD"));

        ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(1)).deleteFolder(ac.capture());
        assertEquals("/something", ac.getValue());

        ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(2)).deleteFragment(ac.capture());
        assertThat(ac.getAllValues()).containsAll(Arrays.asList("title_fragment", "another_fragment"));

        ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(2)).deleteContentType(ac.capture());
        assertThat(ac.getAllValues()).containsAll(Arrays.asList("CNG", "CNT"));

        ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(2)).deleteContent(ac.capture());
        assertThat(ac.getAllValues()).containsAll(Arrays.asList("CNG102", "CNT103"));

        ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(2)).deleteAsset(ac.capture());
        assertThat(ac.getAllValues()).containsAll(Arrays.asList("cc=my_asset", "cc=anotherAsset"));

        ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(2)).deleteContentType(ac.capture());
        assertThat(ac.getAllValues()).containsAll(Arrays.asList("CNG", "CNT"));

        ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(2)).setPageStatus(ac.capture(), ac.capture());
        assertThat(ac.getAllValues()).containsAll(Arrays.asList("my-page", "another-page"));
        assertThat(ac.getAllValues()).contains("draft");

        ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(2)).deletePage(ac.capture());
        assertThat(ac.getAllValues()).containsAll(Arrays.asList("my-page", "another-page"));

        verify(k8SServiceClient, times(3)).unlink(any());

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
                .andExpect(jsonPath("$.payload[0].code").value("todomvc"))
                .andExpect(jsonPath("$.payload[0].installed").value("true"));

        List<EntandoBundleEntity> installedComponents = installedCompRepo.findAll();
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

        Optional<EntandoBundleJobEntity> job = jobRepository.findById(UUID.fromString(failingJobId));
        assertThat(job.isPresent()).isTrue();

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
            assertThat(jobs.size()).isEqualTo(2);
            assertThat(jobs.stream().anyMatch(j -> j.getStatus().equals(JobStatus.INSTALL_ROLLBACK))).isTrue();
        }

        // And component should not be part of the installed components
        assertThat(installedCompRepo.findAll()).isEmpty();
    }

    @Test
    public void shouldReturnAppropriateErrorCodeWhenFailingInstallDueToBigFile() throws Exception {

        // Given a failed install happened
        String failingJobId = simulateHugeAssetFailingInstall();

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
        assertThat(ec.getErrorMessage()).contains("status code 413", "Payload Too Large");

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
                .andExpect(jsonPath("$.payload[0].code").value("todomvc"))
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
    void shouldReturnDifferentJobIdWhenAttemptingToInstallTheSameComponentTwice() throws Exception {
        // Given I try to update a component which is already installed
        String firstSuccessfulJobId = simulateSuccessfullyCompletedInstall();
        String secondSuccessfulJobId = simulateSuccessfullyCompletedInstall();

        // two successfull jobs are created
        assertThat(firstSuccessfulJobId).isNotEqualTo(secondSuccessfulJobId);
        assertThat(jobRepository.findAll().size()).isEqualTo(2);
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
        waitForInstallStatus(mockMvc, JobStatus.INSTALL_COMPLETED);
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
                        .withBody(readFromDEPackage(MOCK_BUNDLE_NAME))));

        stubFor(WireMock.post(urlEqualTo("/auth/protocol/openid-connect/auth"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"access_token\": \"iddqd\" }")));

        MvcResult result = mockMvc.perform(post(INSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isCreated())
                .andReturn();

        waitForInstallStatus(mockMvc, JobStatus.INSTALL_ROLLBACK, JobStatus.INSTALL_ERROR);

        String jobId = JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");

        Optional<EntandoBundleJobEntity> job = jobRepository.findById(UUID.fromString(jobId));

        assertThat(job.isPresent()).isTrue();

        List<EntandoBundleComponentJobEntity> jobComponentList = componentJobRepository.findAllByParentJob(job.get());

        List<EntandoBundleComponentJobEntity> pluginJobs = jobComponentList.stream()
                .filter(jc -> jc.getComponentType().equals(ComponentType.PLUGIN))
                .collect(Collectors.toList());
        assertThat(pluginJobs.size()).isEqualTo(2);
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

        MvcResult result = mockMvc.perform(post(INSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isCreated())
                .andReturn();
        waitForInstallStatus(mockMvc, JobStatus.INSTALL_COMPLETED);
        String jobId = JsonPath.read(result.getResponse().getContentAsString(), "$.payload.id");

        Optional<EntandoBundleJobEntity> job = jobRepository.findById(UUID.fromString(jobId));
        assertThat(job).isPresent();

        List<EntandoBundleComponentJobEntity> pluginJobs = componentJobRepository.findAllByParentJob(job.get())
                .stream()
                .filter(jc -> jc.getComponentType().equals(ComponentType.PLUGIN))
                .collect(Collectors.toList());;

        assertThat(pluginJobs.size()).isEqualTo(3);

        // when deploymentBaseName is not present => component id should be the image organization, name and version
        assertThat(pluginJobs.get(0).getComponentId()).isEqualTo("entando-todomvcv1");
        assertThat(pluginJobs.get(1).getComponentId()).isEqualTo("entando-todomvcv2");
        // when deploymentBaseName is not present => component id should be the deploymentBaseName itself
        assertThat(pluginJobs.get(2).getComponentId()).isEqualTo("customBaseName");
    }


    @Test
    public void shouldUpdateDatabaseOnlyWhenOperationIsCompleted() throws Exception {
        simulateInProgressInstall();
        assertThat(installedCompRepo.findAll()).isEmpty();

        waitForInstallStatus(mockMvc, JobStatus.INSTALL_COMPLETED);
        assertThat(installedCompRepo.findAll()).isNotEmpty();

        simulateInProgressUninstall();
        assertThat(installedCompRepo.findAll()).isNotEmpty();

        waitForUninstallStatus(mockMvc, JobStatus.UNINSTALL_COMPLETED);
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
        waitForUninstallStatus(mockMvc, JobStatus.UNINSTALL_COMPLETED);
    }

    @Test
    public void shouldCreateJobAndReturn201StatusAndLocationHeader() throws Exception {
        Mockito.reset(coreClient);
        WireMock.reset();
        WireMock.setGlobalFixedDelay(0);

        mockBundle(k8SServiceClient);

        stubFor(WireMock.get("/repository/npm-internal/test_bundle/-/test_bundle-0.0.1.tgz")
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/octet-stream")
                        .withBody(readFromDEPackage(MOCK_BUNDLE_NAME))));

        stubFor(WireMock.post(urlEqualTo("/auth/protocol/openid-connect/auth"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{ \"access_token\": \"iddqd\" }")));
        stubFor(WireMock.get(urlMatching("/k8s/.*")).willReturn(aResponse().withStatus(200)));
        //        stubFor(WireMock.post(urlMatching("/entando-app/api/.*")).willReturn(aResponse().withStatus(200)));

        MvcResult result = mockMvc.perform(post(INSTALL_COMPONENT_ENDPOINT.build()))
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
    void shouldReturnAValidAnalysisReport() throws Exception {
        mockAnalysisReport(coreClient, k8SServiceClient);
        mockBundle(k8SServiceClient);

        AnalysisReport expected = getAnalysisReport();
        MvcResult response = mockMvc.perform(post(TestInstallUtils.ANALYSIS_REPORT_ENDPOINT.build()))
                .andExpect(status().isOk())
                .andReturn();

        Configuration conf = Configuration.builder()
                .jsonProvider(new JacksonJsonProvider(new ObjectMapper()))
                .build();

        AnalysisReport result = JsonPath.using(conf).parse(response.getResponse().getContentAsString())
                .read("$.payload", AnalysisReport.class);

        assertThat(result).isEqualTo(expected);
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
        waitForUninstallStatus(mockMvc, JobStatus.UNINSTALL_ERROR);
        assertThat(true).isTrue();
    }

    @Test
    public void testProgressIsExposedViaApi() throws Exception {
        String jobId = simulateInProgressInstall();
        verifyJobProgressesFromStatusToStatus(jobId, JobStatus.INSTALL_IN_PROGRESS, JobStatus.INSTALL_COMPLETED);

        jobId = simulateInProgressUninstall();
        verifyJobProgressesFromStatusToStatus(jobId, JobStatus.UNINSTALL_IN_PROGRESS, JobStatus.UNINSTALL_COMPLETED);
    }

    @Test
    void shouldReturn503OnInstallIfAnotherBundleOperationIsRunning() throws Exception {

        simulateSuccessfullyCompletedInstall();

        doThrow(BundleOperationConcurrencyException.class).when(bundleOperationsConcurrencyManager)
                .throwIfAnotherOperationIsRunningOrStartOperation();

        mockMvc.perform(post(INSTALL_COMPONENT_ENDPOINT.build()))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void shouldReturn503OnAnalysisIfAnotherBundleOperationIsRunning() throws Exception {

        mockAnalysisReport(coreClient, k8SServiceClient);
        mockBundle(k8SServiceClient);

        doThrow(BundleOperationConcurrencyException.class).when(bundleOperationsConcurrencyManager)
                .throwIfAnotherOperationIsRunningOrStartOperation();

        mockMvc.perform(post(TestInstallUtils.ANALYSIS_REPORT_ENDPOINT.build()))
                .andExpect(status().isServiceUnavailable());
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

    private String simulateSuccessfullyCompletedInstall() {
        return TestInstallUtils
                .simulateSuccessfullyCompletedInstall(mockMvc, coreClient, k8SServiceClient, MOCK_BUNDLE_NAME);
    }

    private String simulateSuccessfullyCompletedUninstall() {
        return TestInstallUtils.simulateSuccessfullyCompletedUninstall(mockMvc, coreClient);
    }

    private String simulateInProgressInstall() {
        return TestInstallUtils.simulateInProgressInstall(mockMvc, coreClient, k8SServiceClient, MOCK_BUNDLE_NAME);
    }

    private String simulateInProgressUninstall() {
        return TestInstallUtils.simulateInProgressUninstall(mockMvc, coreClient);
    }

    private String simulateFailingInstall() {
        return TestInstallUtils.simulateFailingInstall(mockMvc, coreClient, k8SServiceClient, MOCK_BUNDLE_NAME);
    }

    private String simulateHugeAssetFailingInstall() {
        return TestInstallUtils
                .simulateHugeAssetFailingInstall(mockMvc, coreClient, k8SServiceClient, MOCK_BUNDLE_NAME);
    }

    private String simulateFailingUninstall() {
        return TestInstallUtils.simulateFailingUninstall(mockMvc, coreClient);
    }

    private String simulateBundleDownloadError() {
        return TestInstallUtils.simulateBundleDownloadError(mockMvc, coreClient, k8SServiceClient, downloaderFactory);
    }
}
