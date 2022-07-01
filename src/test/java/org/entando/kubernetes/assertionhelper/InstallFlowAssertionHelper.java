package org.entando.kubernetes.assertionhelper;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.entando.kubernetes.DigitalExchangeTestUtils.readFile;
import static org.entando.kubernetes.DigitalExchangeTestUtils.readFileAsBase64;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
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
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor.ApiClaim;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.utils.TestInstallUtils;
import org.mockito.ArgumentCaptor;

public class InstallFlowAssertionHelper {

    private K8SServiceClient k8SServiceClient;
    private EntandoCoreClient coreClient;
    private EntandoBundleJobRepository jobRepository;
    private EntandoBundleComponentJobRepository componentJobRepository;

    public InstallFlowAssertionHelper(K8SServiceClient k8SServiceClient, EntandoCoreClient coreClient,
            EntandoBundleJobRepository jobRepository, EntandoBundleComponentJobRepository componentJobRepository) {
        this.k8SServiceClient = k8SServiceClient;
        this.coreClient = coreClient;
        this.jobRepository = jobRepository;
        this.componentJobRepository = componentJobRepository;
    }

    public void verifyCoreCalls() {
        verifyPluginInstallRequests(k8SServiceClient);
        verifyWidgetsInstallRequests(coreClient);
        verifyCategoryInstallRequests(coreClient);
        verifyGroupInstallRequests(coreClient);
        verifyPageModelsInstallRequests(coreClient);
        verifyLanguagesInstallRequests(coreClient);
        verifyLabelsInstallRequests(coreClient);
        verifyDirectoryInstallRequests(coreClient);
        verifyFileInstallRequestsV1(coreClient);
        verifyFragmentInstallRequests(coreClient);
        verifyPageInstallRequests(coreClient);
        verifyPageConfigurationInstallRequests(coreClient);
        verifyContentTypesInstallRequests(coreClient);
        verifyContentTemplatesInstallRequests(coreClient);
        verifyContentsInstallRequests(coreClient);
        verifyAssetsInstallRequests(coreClient);
    }

    public void verifyCoreCallsV5() {
        verifyPluginInstallRequestsV5(k8SServiceClient);
        verifyWidgetsInstallRequestsV5(coreClient);
        verifyDirectoryInstallRequests(coreClient);
        verifyFileInstallRequestsV5(coreClient);
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
        assertThat(allPassedContents.get(0).getCategories()).containsOnly("cat1", "cat2");

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

    private void verifyFileInstallRequestsV1(EntandoCoreClient coreClient) {
        ArgumentCaptor<FileDescriptor> fileArgCaptor = ArgumentCaptor.forClass(FileDescriptor.class);
        verify(coreClient, times(5)).createFile(fileArgCaptor.capture());

        List<FileDescriptor> allPassedFiles = fileArgCaptor.getAllValues()
                .stream().sorted(Comparator.comparing(fd -> (fd.getFolder() + fd.getFilename())))
                .collect(Collectors.toList());

        validateResourceFile(allPassedFiles, 0, "custom.css", "/something/css",
                "/bundle/resources/css/custom.css");
        validateResourceFile(allPassedFiles, 1, "style.css", "/something/css",
                "/bundle/resources/css/style.css");
        validateResourceFile(allPassedFiles, 2, "configUiScript.js", "/something/js",
                "/bundle/resources/js/configUiScript.js");
        validateResourceFile(allPassedFiles, 3, "script.js", "/something/js",
                "/bundle/resources/js/script.js");
    }

    private void verifyFileInstallRequestsV5(EntandoCoreClient coreClient) {
        ArgumentCaptor<FileDescriptor> fileArgCaptor = ArgumentCaptor.forClass(FileDescriptor.class);
        verify(coreClient, times(11)).createFile(fileArgCaptor.capture());

        List<FileDescriptor> allPassedFiles = fileArgCaptor.getAllValues()
                .stream().sorted(Comparator.comparing(fd -> (fd.getFolder() + fd.getFilename())))
                .collect(Collectors.toList());

        int i = 0;
        validateResourceFile(allPassedFiles, i++, "css-res.css",
                "bundles/something-ece8f6f0/widgets/my_widget_app_builder_descriptor_v5-ece8f6f0/assets",
                "/bundle-v5/widgets/my_widget_app_builder_descriptor_v5/assets/css-res.css");
        validateResourceFile(allPassedFiles, i++, "generic-file.txt",
                "bundles/something-ece8f6f0/widgets/my_widget_app_builder_descriptor_v5-ece8f6f0/media",
                "/bundle-v5/widgets/my_widget_app_builder_descriptor_v5/media/generic-file.txt");
        validateResourceFile(allPassedFiles, i++, "js-res-2.js",
                "bundles/something-ece8f6f0/widgets/my_widget_app_builder_descriptor_v5-ece8f6f0/static/js",
                "/bundle-v5/widgets/my_widget_app_builder_descriptor_v5/static/js/js-res-2.js");
        validateResourceFile(allPassedFiles, i++, "js-res-1.js",
                "bundles/something-ece8f6f0/widgets/my_widget_app_builder_descriptor_v5-ece8f6f0",
                "/bundle-v5/widgets/my_widget_app_builder_descriptor_v5/js-res-1.js");
        validateResourceFile(allPassedFiles, i++, "css-res.css",
                "bundles/something-ece8f6f0/widgets/my_widget_config_descriptor_v5-ece8f6f0/assets",
                "/bundle-v5/widgets/my_widget_config_descriptor_v5/assets/css-res.css");
        validateResourceFile(allPassedFiles, i++, "js-res-2.js",
                "bundles/something-ece8f6f0/widgets/my_widget_config_descriptor_v5-ece8f6f0/static/js",
                "/bundle-v5/widgets/my_widget_config_descriptor_v5/static/js/js-res-2.js");
        validateResourceFile(allPassedFiles, i++, "js-res-1.js",
                "bundles/something-ece8f6f0/widgets/my_widget_config_descriptor_v5-ece8f6f0",
                "/bundle-v5/widgets/my_widget_config_descriptor_v5/js-res-1.js");
        validateResourceFile(allPassedFiles, i++, "css-res.css",
                "bundles/something-ece8f6f0/widgets/my_widget_descriptor_v5-ece8f6f0/assets",
                "/bundle-v5/widgets/my_widget_descriptor_v5/assets/css-res.css");
        validateResourceFile(allPassedFiles, i++, "generic-file.txt",
                "bundles/something-ece8f6f0/widgets/my_widget_descriptor_v5-ece8f6f0/media",
                "/bundle-v5/widgets/my_widget_descriptor_v5/media/generic-file.txt");
        validateResourceFile(allPassedFiles, i++, "js-res-2.js",
                "bundles/something-ece8f6f0/widgets/my_widget_descriptor_v5-ece8f6f0/static/js",
                "/bundle-v5/widgets/my_widget_descriptor_v5/static/js/js-res-2.js");
        validateResourceFile(allPassedFiles, i++, "js-res-1.js",
                "bundles/something-ece8f6f0/widgets/my_widget_descriptor_v5-ece8f6f0",
                "/bundle-v5/widgets/my_widget_descriptor_v5/js-res-1.js");
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
        verify(k8SServiceClient, times(6)).linkAppWithPlugin(any(), any(), any());
    }

    private void verifyPluginInstallRequestsV5(K8SServiceClient k8SServiceClient) {
        verify(k8SServiceClient, times(3)).linkAppWithPlugin(any(), any(), any());
    }

    private void verifyPageModelsInstallRequests(EntandoCoreClient coreClient) {
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

    private void verifyWidgetsInstallRequests(EntandoCoreClient coreClient) {
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

    public void verifyWidgetsInstallRequestsV5(EntandoCoreClient coreClient) {
        ArgumentCaptor<WidgetDescriptor> widgetDescArgCaptor = ArgumentCaptor.forClass(WidgetDescriptor.class);
        verify(coreClient, times(1)).createWidget(widgetDescArgCaptor.capture());
        List<WidgetDescriptor> allPassedWidgets = widgetDescArgCaptor.getAllValues()
                .stream().sorted(Comparator.comparing(WidgetDescriptor::getCode))
                .collect(Collectors.toList());

        assertThat(allPassedWidgets.get(0).getCode()).isEqualTo("todomvc_widget-ece8f6f0");
        assertThat(allPassedWidgets.get(0).getGroup()).isEqualTo("free");
        assertThat(allPassedWidgets.get(0).getCustomUi()).isEqualTo(readFile("/my_widget_descriptor_v5.ftl").trim());
        assertThat(allPassedWidgets.get(0).getCustomElement()).isEqualTo("my-widget");

        ApiClaim apiClaim1 = new ApiClaim("ext-api", "external", "ms1", "abcdefgh");
        ApiClaim apiClaim2 = new ApiClaim("int-api", "internal", "customBaseNameV5", null);
        assertThat(allPassedWidgets.get(0).getApiClaims().get(0)).isEqualToComparingFieldByField(apiClaim1);
        assertThat(allPassedWidgets.get(0).getApiClaims().get(1)).isEqualToComparingFieldByField(apiClaim2);
    }


    public void verifyAfterShouldRecordJobStatusAndComponentsForAuditingWhenInstallComponentsV1() {

        List<EntandoBundleJobEntity> jobs = jobRepository.findAll();
        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).getStatus()).isEqualByComparingTo(JobStatus.INSTALL_COMPLETED);

        List<String> expected = Arrays.asList(
                // Plugins
                TestInstallUtils.PLUGIN_TODOMVC_TODOMVC_1,
                TestInstallUtils.PLUGIN_TODOMVC_TODOMVC_2,
                TestInstallUtils.PLUGIN_TODOMVC_CUSTOMBASE,
                TestInstallUtils.PLUGIN_TODOMVC_CUSTOMBASE_V3,
                TestInstallUtils.PLUGIN_TODOMVC_CUSTOMBASE_V3C,
                TestInstallUtils.PLUGIN_TODOMVC_CUSTOMBASE_V4,
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
        expectedComponents.put(ComponentType.PLUGIN, 6);

        assertThat(jobComponentTypes).containsAllEntriesOf(expectedComponents);
    }

    //##############################################################################
    // INSTALL WITH INSTALL PLAN
    //##############################################################################

    public void verifyPluginInstallRequestsWithInstallPlanRequest(K8SServiceClient k8SServiceClient) {
        verify(k8SServiceClient, times(6)).linkAppWithPlugin(any(), any(), any());
    }

    public void verifyPluginInstallRequestsWithInstallPlanRequestV5(K8SServiceClient k8SServiceClient) {
        verify(k8SServiceClient, times(3)).linkAppWithPlugin(any(), any(), any());
    }

    public void verifyWidgetsInstallRequestsWithInstallPlanRequest(EntandoCoreClient coreClient) {
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


    public void verifyCategoryInstallRequestsWithInstallPlanRequest(EntandoCoreClient coreClient) {
        ArgumentCaptor<CategoryDescriptor> categoryDescriptor = ArgumentCaptor.forClass(CategoryDescriptor.class);
        verify(coreClient, times(1)).createCategory(categoryDescriptor.capture());

        List<CategoryDescriptor> allRequests = categoryDescriptor.getAllValues()
                .stream().sorted(Comparator.comparing(CategoryDescriptor::getCode))
                .collect(Collectors.toList());

        assertThat(allRequests.get(0)).matches(d ->
                d.getCode().equals("my-category")
                        && d.getParentCode().equals("home")
                        && d.getTitles().containsKey("it")
                        && d.getTitles().get("it").equals("La mia categoria")
                        && d.getTitles().containsKey("en")
                        && d.getTitles().get("en").equals("My own category"));
    }

    public void verifyGroupInstallRequestsWithInstallPlanRequest(EntandoCoreClient coreClient) {
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

    public void verifyPageInstallRequestsWithInstallPlanRequest(EntandoCoreClient coreClient) {
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

    public void verifyPageConfigurationInstallRequestsWithInstallPlanRequest(EntandoCoreClient coreClient) {
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

    public void verifyContentsInstallRequestsWithInstallPlanRequest(EntandoCoreClient coreClient) {
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
        assertThat(allPassedContents.get(0).getCategories()).containsOnly("cat1", "cat2");

        assertThat(allPassedContents.get(1).getId()).isEqualTo("CNT103");
        assertThat(allPassedContents.get(1).getTypeCode()).isEqualTo("CNT");
        assertThat(allPassedContents.get(1).getDescription()).isEqualTo("Interest 3 card title - 2nd banner");
        assertThat(allPassedContents.get(1).getMainGroup()).isEqualTo("free");

        verify(coreClient, times(2)).publishContent(any());
    }

    public void verifyFragmentInstallRequestsWithInstallPlanRequest(EntandoCoreClient coreClient) {
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

    public void verifyFileInstallRequestsWithInstallPlanRequestV1(EntandoCoreClient coreClient) {
        ArgumentCaptor<FileDescriptor> fileArgCaptor = ArgumentCaptor.forClass(FileDescriptor.class);
        verify(coreClient, times(2)).createFile(fileArgCaptor.capture());
        verify(coreClient, times(2)).updateFile(fileArgCaptor.capture());

        List<FileDescriptor> allPassedFiles = fileArgCaptor.getAllValues()
                .stream().sorted(Comparator.comparing(fd -> (fd.getFolder() + fd.getFilename())))
                .collect(Collectors.toList());

        validateResourceFile(allPassedFiles, 0, "style.css", "/something/css",
                "/bundle/resources/css/style.css");
        validateResourceFile(allPassedFiles, 1, "configUiScript.js", "/something/js",
                "/bundle/resources/js/configUiScript.js");
        validateResourceFile(allPassedFiles, 2, "script.js", "/something/js",
                "/bundle/resources/js/script.js");
    }

    public void verifyFileInstallRequestsWithInstallPlanRequestV5(EntandoCoreClient coreClient) {
        ArgumentCaptor<FileDescriptor> fileArgCaptor = ArgumentCaptor.forClass(FileDescriptor.class);
        verify(coreClient, times(10)).createFile(fileArgCaptor.capture());
        verify(coreClient, times(0)).updateFile(fileArgCaptor.capture());

        List<FileDescriptor> allPassedFiles = fileArgCaptor.getAllValues()
                .stream().sorted(Comparator.comparing(fd -> (fd.getFolder() + fd.getFilename())))
                .collect(Collectors.toList());

        int i = 0;
        validateResourceFile(allPassedFiles, i++, "css-res.css",
                "bundles/something-ece8f6f0/widgets/my_widget_app_builder_descriptor_v5-ece8f6f0/assets",
                "/bundle-v5/widgets/my_widget_app_builder_descriptor_v5/assets/css-res.css");
        validateResourceFile(allPassedFiles, i++, "generic-file.txt",
                "bundles/something-ece8f6f0/widgets/my_widget_app_builder_descriptor_v5-ece8f6f0/media",
                "/bundle-v5/widgets/my_widget_app_builder_descriptor_v5/media/generic-file.txt");
        validateResourceFile(allPassedFiles, i++, "js-res-2.js",
                "bundles/something-ece8f6f0/widgets/my_widget_app_builder_descriptor_v5-ece8f6f0/static/js",
                "/bundle-v5/widgets/my_widget_app_builder_descriptor_v5/static/js/js-res-2.js");
        validateResourceFile(allPassedFiles, i++, "js-res-1.js",
                "bundles/something-ece8f6f0/widgets/my_widget_app_builder_descriptor_v5-ece8f6f0",
                "/bundle-v5/widgets/my_widget_app_builder_descriptor_v5/js-res-1.js");
        validateResourceFile(allPassedFiles, i++, "css-res.css",
                "bundles/something-ece8f6f0/widgets/my_widget_config_descriptor_v5-ece8f6f0/assets",
                "/bundle-v5/widgets/my_widget_config_descriptor_v5/assets/css-res.css");
        validateResourceFile(allPassedFiles, i++, "js-res-2.js",
                "bundles/something-ece8f6f0/widgets/my_widget_config_descriptor_v5-ece8f6f0/static/js",
                "/bundle-v5/widgets/my_widget_config_descriptor_v5/static/js/js-res-2.js");
        validateResourceFile(allPassedFiles, i++, "js-res-1.js",
                "bundles/something-ece8f6f0/widgets/my_widget_config_descriptor_v5-ece8f6f0",
                "/bundle-v5/widgets/my_widget_config_descriptor_v5/js-res-1.js");
        validateResourceFile(allPassedFiles, i++, "generic-file.txt",
                "bundles/something-ece8f6f0/widgets/my_widget_descriptor_v5-ece8f6f0/media",
                "/bundle-v5/widgets/my_widget_descriptor_v5/media/generic-file.txt");
        validateResourceFile(allPassedFiles, i++, "js-res-2.js",
                "bundles/something-ece8f6f0/widgets/my_widget_descriptor_v5-ece8f6f0/static/js",
                "/bundle-v5/widgets/my_widget_descriptor_v5/static/js/js-res-2.js");
        validateResourceFile(allPassedFiles, i++, "js-res-1.js",
                "bundles/something-ece8f6f0/widgets/my_widget_descriptor_v5-ece8f6f0",
                "/bundle-v5/widgets/my_widget_descriptor_v5/js-res-1.js");
    }

    private void validateResourceFile(List<FileDescriptor> allPassedFiles, int i,
            String shouldBeNamed, String shouldBeInFolder, String shouldMatchTheContentsOfFile) {
        assertThat(allPassedFiles.get(i)).matches(fd -> fd.getFilename().equals(shouldBeNamed)
                && fd.getFolder().equals(shouldBeInFolder)
                && fd.getBase64().equals(readFileAsBase64(shouldMatchTheContentsOfFile)));
    }

    public void verifyLanguagesInstallRequestsWithInstallPlanRequest(EntandoCoreClient coreClient) {
        ArgumentCaptor<LanguageDescriptor> languageArgCaptor = ArgumentCaptor.forClass(LanguageDescriptor.class);
        verify(coreClient, times(1)).enableLanguage(languageArgCaptor.capture());

        List<LanguageDescriptor> languageDescriptorList = languageArgCaptor.getAllValues().stream()
                .sorted(Comparator.comparing(langDescriptor -> langDescriptor.getCode().toLowerCase()))
                .collect(Collectors.toList());

        assertThat(languageDescriptorList.get(0).getCode()).isEqualTo("en");
        assertThat(languageDescriptorList.get(0).getDescription()).isEqualTo("English");
    }


    public void verifyLabelsInstallRequestsWithInstallPlanRequest(EntandoCoreClient coreClient) {
        ArgumentCaptor<LabelDescriptor> labelArgCaptor = ArgumentCaptor.forClass(LabelDescriptor.class);
        verify(coreClient, times(1)).createLabel(labelArgCaptor.capture());

        List<LabelDescriptor> allPassedLabels = labelArgCaptor.getAllValues()
                .stream().sorted(Comparator.comparing(LabelDescriptor::getKey))
                .collect(Collectors.toList());

        assertThat(allPassedLabels.get(0).getKey()).isEqualTo("WORLD");
        assertThat(allPassedLabels.get(0).getTitles()).hasSize(2);
        assertThat(allPassedLabels.get(0).getTitles()).containsEntry("it", "Mundo");
        assertThat(allPassedLabels.get(0).getTitles()).containsEntry("en", "World");
    }

    public void verifyContentTypesInstallRequestsWithInstallPlanRequest(EntandoCoreClient coreClient) {
        verify(coreClient, times(1)).createContentType(any());
        verify(coreClient, times(1)).updateContentType(any());
    }

    public void verifyContentTemplatesInstallRequestsWithInstallPlanRequest(EntandoCoreClient coreClient) {
        verify(coreClient, times(1)).createContentTemplate(any());
    }

    public void verifyAssetsInstallRequestsWithInstallPlanRequest(EntandoCoreClient coreClient) {
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

    public void verifyDirectoryInstallRequestsWithInstallPlanRequest(EntandoCoreClient coreClient) {
        verify(coreClient, times(0)).createFolder(any());
    }

    public void verifyPageModelsInstallRequestsWithInstallPlanRequest(EntandoCoreClient coreClient) {
        ArgumentCaptor<PageTemplateDescriptor> pageModelDescrArgCaptor = ArgumentCaptor
                .forClass(PageTemplateDescriptor.class);
        verify(coreClient, times(1)).createPageTemplate(pageModelDescrArgCaptor.capture());

        List<PageTemplateDescriptor> allPassedPageModels = pageModelDescrArgCaptor.getAllValues()
                .stream().sorted(Comparator.comparing(PageTemplateDescriptor::getCode))
                .collect(Collectors.toList());

        assertThat(allPassedPageModels.get(0).getCode()).isEqualTo("todomvc_page_model");
        assertThat(allPassedPageModels.get(0).getDescription()).isEqualTo("TODO MVC basic page model");
        assertThat(allPassedPageModels.get(0).getConfiguration().getFrames().get(0))
                .matches(f -> f.getPos().equals("0")
                        && f.getDescription().equals("Header")
                        && f.getSketch().getX1() == 0
                        && f.getSketch().getY1() == 0
                        && f.getSketch().getX2() == 11
                        && f.getSketch().getY2() == 0);
        assertThat(allPassedPageModels.get(0).getConfiguration().getFrames().get(1))
                .matches(f -> f.getPos().equals("1")
                        && f.getDescription().equals("Breadcrumb")
                        && f.getSketch().getX1() == 0
                        && f.getSketch().getY1() == 1
                        && f.getSketch().getX2() == 11
                        && f.getSketch().getY2() == 1);
    }
}
