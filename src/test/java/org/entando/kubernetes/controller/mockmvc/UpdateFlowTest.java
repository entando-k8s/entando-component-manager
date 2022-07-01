package org.entando.kubernetes.controller.mockmvc;

import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.entando.kubernetes.DigitalExchangeTestUtils.readFile;
import static org.entando.kubernetes.DigitalExchangeTestUtils.readFileAsBase64;
import static org.entando.kubernetes.utils.TestInstallUtils.verifyJobHasComponentAndStatus;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.entando.kubernetes.DatabaseCleaner;
import org.entando.kubernetes.EntandoKubernetesJavaApplication;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.config.TestAppConfiguration;
import org.entando.kubernetes.config.TestKubernetesConfig;
import org.entando.kubernetes.config.TestSecurityConfiguration;
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
import org.entando.kubernetes.model.bundle.descriptor.content.ContentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloader;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderFactory;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.bundle.reportable.AnalysisReportFunction;
import org.entando.kubernetes.model.bundle.reportable.ReportableComponentProcessor;
import org.entando.kubernetes.model.bundle.reportable.ReportableRemoteHandler;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
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
        })
@ActiveProfiles({"test"})
@Tag("component")
@WithMockUser
public class UpdateFlowTest {

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
        downloaderFactory.setDefaultSupplier(defaultBundleDownloaderSupplier);
    }

    @Test
    void shouldCreateOrUpdateComponentsDuringInstall() throws Exception {
        simulateSuccessfullyCompletedUpdate();

        verifyPluginInstallRequests(k8SServiceClient);
        verifyCategoryInstallRequests(coreClient);
        verifyGroupInstallRequests(coreClient);
        verifyLanguagesInstallRequests(coreClient);
        verifyLabelsInstallRequests(coreClient);
        verifyDirectoryInstallRequests(coreClient);
        verifyFileInstallRequests(coreClient);
        verifyWidgetsInstallRequests(coreClient);
        verifyFragmentInstallRequests(coreClient);
        verifyPageInstallRequests(coreClient);
        verifyPageModelsInstallRequests(coreClient);
        verifyPageConfigurationInstallRequests(coreClient);
        verifyContentTypesInstallRequests(coreClient);
        verifyContentTemplatesInstallRequests(coreClient);
        verifyContentsInstallRequests(coreClient);
        verifyAssetsInstallRequests(coreClient);
    }

    @Test
    void shouldRemoveOnlyCreatedComponentsDuringUninstall() throws Exception {
        String jobId = simulateSuccessfullyCompletedUpdate();

        verifyJobHasComponentAndStatus(mockMvc, jobId, JobStatus.INSTALL_COMPLETED);

        final String uninstallJobId = simulateSuccessfullyCompletedUninstall();

        verifyPluginsUninstallRequests();
        verifyWidgetsUninstallRequests();
        verifyPageTemplatesUninstallRequests();
        verifyLanguagesUninstallRequests();
        verifyLabelsUninstallRequests();
        verifyDirectoriesUninstallRequests();
        verifyFragmentsUninstallRequests();
        verifyContentTypesUninstallRequests();
        verifyContentsUninstallRequests();
        verifyAssetsUninstallRequests();
        verifyContentTypesUninstallRequests();
        verifyPageSetDraftStatus();
        verifyPagesUninstallRequests();

        verifyJobHasComponentAndStatus(mockMvc, uninstallJobId, JobStatus.UNINSTALL_COMPLETED);
    }

    private void verifyCategoryInstallRequests(EntandoCoreClient coreClient) {
        ArgumentCaptor<CategoryDescriptor> categoryDescriptor = ArgumentCaptor.forClass(CategoryDescriptor.class);
        verify(coreClient, times(1)).createCategory(categoryDescriptor.capture());
        verify(coreClient, times(1)).updateCategory(categoryDescriptor.capture());

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
        verify(coreClient, times(1)).createGroup(groupDescriptor.capture());
        verify(coreClient, times(1)).updateGroup(groupDescriptor.capture());

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
        verify(coreClient, times(1)).createPage(pag.capture());

        List<PageDescriptor> allPageRequests = pag.getAllValues()
                .stream().sorted(Comparator.comparing(PageDescriptor::getCode))
                .collect(Collectors.toList());

        assertThat(allPageRequests.get(0)).matches(pd -> pd.getCode().equals("my-page")
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
        verify(coreClient, times(1)).createContent(contentDescriptorArgCaptor.capture());
        verify(coreClient, times(1)).updateContent(contentDescriptorArgCaptor.capture());

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
        verify(coreClient, times(1)).createFragment(fragmentDescArgCapt.capture());
        verify(coreClient, times(1)).updateFragment(fragmentDescArgCapt.capture());
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
        verify(coreClient, times(4)).createFile(fileArgCaptor.capture());
        verify(coreClient, times(1)).updateFile(fileArgCaptor.capture());

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
        assertThat(allPassedFiles.get(4)).matches(fd -> fd.getFilename().equals("jquery.js")
                && fd.getFolder().equals("/something/vendor/jquery")
                && fd.getBase64().equals(readFileAsBase64("/bundle/resources/vendor/jquery/jquery.js")));
    }


    private void verifyLanguagesInstallRequests(EntandoCoreClient coreClient) {
        ArgumentCaptor<LanguageDescriptor> languageArgCaptor = ArgumentCaptor.forClass(LanguageDescriptor.class);
        verify(coreClient, times(1)).enableLanguage(languageArgCaptor.capture());

        List<LanguageDescriptor> languageDescriptorList = languageArgCaptor.getAllValues().stream()
                .sorted(Comparator.comparing(langDescriptor -> langDescriptor.getCode().toLowerCase()))
                .collect(Collectors.toList());

        assertThat(languageDescriptorList.get(0).getCode()).isEqualTo("en");
        assertThat(languageDescriptorList.get(0).getDescription()).isEqualTo("English");
    }


    private void verifyLabelsInstallRequests(EntandoCoreClient coreClient) {
        ArgumentCaptor<LabelDescriptor> labelArgCaptor = ArgumentCaptor.forClass(LabelDescriptor.class);
        verify(coreClient, times(1)).createLabel(labelArgCaptor.capture());
        verify(coreClient, times(1)).updateLabel(labelArgCaptor.capture());

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
        verify(coreClient, times(1)).createContentType(any());
        verify(coreClient, times(1)).updateContentType(any());
    }

    private void verifyContentTemplatesInstallRequests(EntandoCoreClient coreClient) {
        verify(coreClient, times(1)).createContentTemplate(any());
        verify(coreClient, times(1)).updateContentTemplate(any());
    }

    private void verifyAssetsInstallRequests(EntandoCoreClient coreClient) {
        ArgumentCaptor<AssetDescriptor> codesArgCaptor = ArgumentCaptor.forClass(AssetDescriptor.class);
        verify(coreClient, times(1)).createAsset(codesArgCaptor.capture(), isA(File.class));
        verify(coreClient, times(1)).updateAsset(codesArgCaptor.capture(), isA(File.class));

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
        verify(k8SServiceClient, times(6)).linkAppWithPlugin(any(), any(), any());
        verify(k8SServiceClient, times(4)).unlink(any());
    }

    private void verifyPageModelsInstallRequests(EntandoCoreClient coreClient) throws Exception {
        ArgumentCaptor<PageTemplateDescriptor> pageModelDescrArgCaptor = ArgumentCaptor
                .forClass(PageTemplateDescriptor.class);
        verify(coreClient, times(1)).createPageTemplate(pageModelDescrArgCaptor.capture());
        verify(coreClient, times(1)).updatePageTemplate(pageModelDescrArgCaptor.capture());

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
        verify(coreClient, times(1)).createWidget(widgetDescArgCaptor.capture());
        verify(coreClient, times(1)).updateWidget(widgetDescArgCaptor.capture());
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

    private void verifyPluginsUninstallRequests() {
        verify(k8SServiceClient, times(6)).unlinkAndScaleDown(any());
    }

    public void verifyPageSetDraftStatus() {
        ArgumentCaptor<String> ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(1)).setPageStatus(ac.capture(), ac.capture());
        assertThat(ac.getAllValues()).contains("my-page");
        assertThat(ac.getAllValues()).contains("draft");
    }

    private void verifyPagesUninstallRequests() {
        ArgumentCaptor<String> ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(1)).deletePage(ac.capture());
        assertThat(ac.getAllValues()).contains("my-page");
    }

    private void verifyContentTypesUninstallRequests() {
        ArgumentCaptor<String> ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(1)).deleteContentType(ac.capture());
        assertThat(ac.getAllValues()).contains("CNT");
    }

    private void verifyAssetsUninstallRequests() {
        ArgumentCaptor<String> ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(1)).deleteAsset(ac.capture());
        assertThat(ac.getAllValues()).contains("cc=my_asset");
    }

    private void verifyContentsUninstallRequests() {
        ArgumentCaptor<String> ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(1)).deleteContent(ac.capture());
        assertThat(ac.getAllValues()).contains("CNT103");
    }

    private void verifyFragmentsUninstallRequests() {
        ArgumentCaptor<String> ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(1)).deleteFragment(ac.capture());
        assertThat(ac.getAllValues()).contains("title_fragment");
    }

    private void verifyDirectoriesUninstallRequests() {
        ArgumentCaptor<String> ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(1)).deleteFolder(ac.capture());
        assertEquals("/something", ac.getValue());
    }

    private void verifyLabelsUninstallRequests() {
        ArgumentCaptor<String> ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(1)).deleteLabel(ac.capture());
        assertThat(ac.getAllValues()).contains("WORLD");
    }

    private void verifyLanguagesUninstallRequests() {
        ArgumentCaptor<String> ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(1)).disableLanguage(ac.capture());
        assertThat(ac.getAllValues()).contains("en");
    }

    private void verifyPageTemplatesUninstallRequests() {
        ArgumentCaptor<String> ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(1)).deletePageModel(ac.capture());
        assertThat(ac.getAllValues()).contains("todomvc_page_model");
    }

    private void verifyWidgetsUninstallRequests() {
        ArgumentCaptor<String> ac = ArgumentCaptor.forClass(String.class);
        verify(coreClient, times(1)).deleteWidget(ac.capture());
        assertThat(ac.getAllValues()).contains("todomvc_widget");
    }

    private String simulateSuccessfullyCompletedUpdate() {
        return TestInstallUtils.simulateSuccessfullyCompletedUpdate(mockMvc, coreClient,
                k8SServiceClient, TestInstallUtils.MOCK_BUNDLE_NAME_TGZ);
    }

    private String simulateSuccessfullyCompletedUninstall() {
        return TestInstallUtils.simulateSuccessfullyCompletedUninstall(mockMvc, coreClient);
    }
}
