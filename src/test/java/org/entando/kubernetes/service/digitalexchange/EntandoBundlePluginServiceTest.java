package org.entando.kubernetes.service.digitalexchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.entando.kubernetes.TestEntitiesGenerator;
import org.entando.kubernetes.exception.digitalexchange.BundleNotInstalledException;
import org.entando.kubernetes.model.bundle.EntandoBundle;
import org.entando.kubernetes.model.bundle.EntandoBundleData;
import org.entando.kubernetes.model.job.EntandoBundleEntity;
import org.entando.kubernetes.model.job.PluginData;
import org.entando.kubernetes.model.job.PluginDataEntity;
import org.entando.kubernetes.model.web.request.Filter;
import org.entando.kubernetes.model.web.request.FilterOperator;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
import org.entando.kubernetes.repository.PluginDataRepository;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleDataListProcessor;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundlePluginService;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundlePluginServiceImpl;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleService;
import org.entando.kubernetes.validator.ValidationFunctions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.zalando.problem.DefaultProblem;

@Tag("unit")
class EntandoBundlePluginServiceTest {

    private EntandoBundlePluginService targetService;
    private EntandoBundleService bundleService;
    private PluginDataRepository pluginDataRepository;

    private InstalledEntandoBundleRepository installedEntandoBundleRepository;
    private static final String PREFIX_BUNDLE_NAME = "my-app-code";
    private static final String PREFIX_BUNDLE_REPO_URL = "http://test.com/gitrepo/reponame";
    private static final int INSTALLED_BUNDLE_SIZE = 6;
    private static final int DEPLOYED_BUNDLE_CR_SIZE = 4;

    @BeforeEach
    public void setup() {
        pluginDataRepository = Mockito.mock(PluginDataRepository.class);
        installedEntandoBundleRepository = Mockito.mock(InstalledEntandoBundleRepository.class);
        bundleService = Mockito.mock(EntandoBundleService.class);
        targetService = new EntandoBundlePluginServiceImpl(pluginDataRepository, installedEntandoBundleRepository,
                bundleService);
    }

    @AfterEach
    public void teardown() {
    }

    @Test
    void getBundles_shouldReturnBundleList() {
        PagedListRequest req = new PagedListRequest();
        // test all installed
        List<EntandoBundleEntity> list = generateBundleListOf_SIZE();
        when(bundleService.listBundlesFromEcr()).thenReturn(new ArrayList<>());
        when(installedEntandoBundleRepository.findAll()).thenReturn(list);
        assertThat(targetService.listBundles(req).getBody()).hasSize(INSTALLED_BUNDLE_SIZE);

        // test installed and CR with no overlap
        final int FULL_SIZE = INSTALLED_BUNDLE_SIZE + DEPLOYED_BUNDLE_CR_SIZE;
        List<EntandoBundle> listCdr = generateBundleCrListOf_CR_SIZE();
        when(bundleService.listBundlesFromEcr()).thenReturn(listCdr);
        assertThat(targetService.listBundles(req).getBody()).hasSize(FULL_SIZE);

        // test installed and CR with one overlap
        String repoUrl = PREFIX_BUNDLE_REPO_URL + 1;
        String bundleId = BundleUtilities.removeProtocolAndGetBundleId(repoUrl);
        String bundleName = PREFIX_BUNDLE_NAME + 1;
        String bundleCode = bundleId + "-" + bundleName;
        listCdr.add(EntandoBundle.builder()
                .code(bundleName)
                .repoUrl(repoUrl)
                .title(bundleName)
                .installedJob(null)
                .lastJob(null)
                .build());
        when(bundleService.listBundlesFromEcr()).thenReturn(listCdr);
        assertThat(targetService.listBundles(req).getBody()).hasSize(FULL_SIZE);

    }

    @Test
    void getBundles_shouldReturnBundleListFiltered() {
        List<EntandoBundleEntity> list = generateBundleListOf_SIZE();
        when(bundleService.listBundlesFromEcr()).thenReturn(new ArrayList<>());
        when(installedEntandoBundleRepository.findAll()).thenReturn(list);

        // test filter by installed
        PagedListRequest req = new PagedListRequest();
        Filter filter = new Filter(EntandoBundleDataListProcessor.INSTALLED, "true");
        filter.setOperator(FilterOperator.EQUAL.getValue());
        req.setFilters(new Filter[]{filter});
        assertThat(targetService.listBundles(req).getBody()).hasSize(INSTALLED_BUNDLE_SIZE);

        // test filter by publicationUrl
        req = new PagedListRequest();
        filter = new Filter(EntandoBundleDataListProcessor.PUBLICATION_URL, PREFIX_BUNDLE_REPO_URL + "1");
        filter.setOperator(FilterOperator.EQUAL.getValue());
        req.setFilters(new Filter[]{filter});
        assertThat(targetService.listBundles(req).getBody()).hasSize(1);

        // test filter by componentType
        req = new PagedListRequest();
        filter = new Filter(EntandoBundleDataListProcessor.COMPONENT_TYPE, "plugin");
        filter.setOperator(FilterOperator.EQUAL.getValue());
        req.setFilters(new Filter[]{filter});
        assertThat(targetService.listBundles(req).getBody()).hasSize(1);

        // test filter by bundleId
        req = new PagedListRequest();
        String repoUrl = PREFIX_BUNDLE_REPO_URL + 2;
        String bundleId = BundleUtilities.removeProtocolAndGetBundleId(repoUrl);
        filter = new Filter(EntandoBundleDataListProcessor.BUNDLE_ID, bundleId);
        filter.setOperator(FilterOperator.EQUAL.getValue());
        req.setFilters(new Filter[]{filter});
        assertThat(targetService.listBundles(req).getBody()).hasSize(1);

        // test filter by wrong name
        req = new PagedListRequest();
        filter = new Filter("XXX", "XXX");
        filter.setOperator(FilterOperator.EQUAL.getValue());
        req.setFilters(new Filter[]{filter});
        assertThat(targetService.listBundles(req).getBody()).hasSize(INSTALLED_BUNDLE_SIZE);

        // test filter by id (no filtering)
        req = new PagedListRequest();
        String uuid = targetService.listBundles(req).getBody().get(1).getId();
        filter = new Filter(EntandoBundleDataListProcessor.ID, uuid);
        filter.setOperator(FilterOperator.EQUAL.getValue());
        req.setFilters(new Filter[]{filter});
        assertThat(targetService.listBundles(req).getBody()).hasSize(INSTALLED_BUNDLE_SIZE);

    }

    @Test
    void getBundles_shouldReturnBundleListSorted() {
        List<EntandoBundleEntity> list = generateBundleListOf_SIZE();
        when(bundleService.listBundlesFromEcr()).thenReturn(new ArrayList<>());
        when(installedEntandoBundleRepository.findAll()).thenReturn(list);

        // test sort by bundle id desc
        PagedListRequest req = new PagedListRequest();
        req.setSort(EntandoBundleDataListProcessor.BUNDLE_ID);
        req.setDirection(Filter.ASC_ORDER);

        PagedMetadata<EntandoBundleData> result = targetService.listBundles(req);

        assertThat(result.getBody()).hasSize(INSTALLED_BUNDLE_SIZE);

        // test sort by pub url desc
        req = new PagedListRequest();
        req.setSort(EntandoBundleDataListProcessor.PUBLICATION_URL);
        req.setDirection(Filter.DESC_ORDER);

        result = targetService.listBundles(req);

        assertThat(result.getBody()).hasSize(INSTALLED_BUNDLE_SIZE);
        String publicationUrlToTest = result.getBody().get(INSTALLED_BUNDLE_SIZE - 1).getPublicationUrl();
        assertThat(publicationUrlToTest).isEqualTo(PREFIX_BUNDLE_REPO_URL + "1");

    }

    private List<EntandoBundleEntity> generateBundleListOf_SIZE() {
        List<EntandoBundleEntity> list = new ArrayList<>();
        IntStream.range(1, INSTALLED_BUNDLE_SIZE + 1).forEach(idx -> {
            String repoUrl = PREFIX_BUNDLE_REPO_URL + idx;
            String bundleId = BundleUtilities.removeProtocolAndGetBundleId(repoUrl);
            String bundleName = PREFIX_BUNDLE_NAME + idx;
            String bundleCode = bundleId + "-" + bundleName;
            EntandoBundleEntity entity = TestEntitiesGenerator.getTestComponent(bundleName, bundleName);
            entity.setRepoUrl(repoUrl);
            if (idx % INSTALLED_BUNDLE_SIZE == 0) {
                entity.setType(Collections.singleton("plugin"));
            } else {
                entity.setType(Collections.singleton("widget"));
            }
            list.add(entity);
        });
        return list;
    }

    private List<EntandoBundle> generateBundleCrListOf_CR_SIZE() {
        final int FULL_SIZE = INSTALLED_BUNDLE_SIZE + DEPLOYED_BUNDLE_CR_SIZE;
        List<EntandoBundle> listCdr = new ArrayList<>();
        IntStream.range(INSTALLED_BUNDLE_SIZE + 1, FULL_SIZE + 1).forEach(idx -> {
            String repoUrl = PREFIX_BUNDLE_REPO_URL + idx;
            String bundleId = BundleUtilities.removeProtocolAndGetBundleId(repoUrl);
            String bundleName = PREFIX_BUNDLE_NAME + idx;
            String bundleCode = bundleId + "-" + bundleName;

            listCdr.add(EntandoBundle.builder()
                    .code(bundleName)
                    .repoUrl(repoUrl)
                    .title(bundleName)
                    .installedJob(null)
                    .lastJob(null)
                    .build());
        });
        return listCdr;
    }

    @Test
    void getInstalledComponentsByBundleId_withValidBundleId_shouldReturnPluginComponentList() {

        final PagedListRequest req = new PagedListRequest();
        final String repoUrl = "https://github.com/entando/example-qe-bundle-01";
        final String bundleId = BundleUtilities.removeProtocolAndGetBundleId(repoUrl);
        final Optional<EntandoBundle> installedBundle = Optional.of(
                TestEntitiesGenerator.getTestEntandoBundle(TestEntitiesGenerator.getTestJob()).setRepoUrl(repoUrl));
        when(bundleService.getInstalledBundleByBundleId(any())).thenReturn(installedBundle);

        List<PluginDataEntity> allComponents =
                Collections.singletonList(new PluginDataEntity());
        when(pluginDataRepository.findAllByBundleId(bundleId)).thenReturn(allComponents);

        PagedMetadata<PluginData> components = targetService.getInstalledPluginsByBundleId(req, bundleId);

        assertThat(components.getBody()).hasSize(1);

        when(bundleService.getInstalledBundleByBundleId(any())).thenReturn(Optional.empty());

        assertThrows(BundleNotInstalledException.class,
                () -> targetService.getInstalledPluginsByBundleId(req, bundleId));

    }

    @Test
    void getInstalledComponentsByBundleCode_withInvalidBundleCode_shouldReturnError() {

        final PagedListRequest req = new PagedListRequest();
        when(bundleService.getInstalledBundle(any())).thenReturn(Optional.empty());

        assertThrows(BundleNotInstalledException.class, () -> targetService.getInstalledPluginsByBundleId(req, ""));
    }

    @Test
    void getInstalledComponentsByEncodedUrl_withValidEncoded_shouldReturnBundleComponentList() {

        final PagedListRequest req = new PagedListRequest();
        final String repoUrl = "http://github.com/entando/example-qe-bundle-01";
        final String bundleId = BundleUtilities.removeProtocolAndGetBundleId(repoUrl);
        final String encodedRepoUrl = Base64.getEncoder().encodeToString(repoUrl.getBytes(StandardCharsets.UTF_8));

        final Optional<EntandoBundle> installedBundle = Optional.of(
                TestEntitiesGenerator.getTestEntandoBundle(TestEntitiesGenerator.getTestJob()).setRepoUrl(repoUrl));

        when(bundleService.getInstalledBundleByEncodedUrl(encodedRepoUrl)).thenReturn(installedBundle);
        when(bundleService.getInstalledBundleByBundleId(bundleId)).thenReturn(installedBundle);

        List<PluginDataEntity> allComponents =
                Collections.singletonList(new PluginDataEntity());
        when(pluginDataRepository.findAllByBundleId(bundleId)).thenReturn(allComponents);

        PagedMetadata<PluginData> components = targetService.getInstalledPluginsByEncodedUrl(req, encodedRepoUrl);
        assertThat(components.getBody()).hasSize(1);
    }

    @Test
    void getInstalledComponentsByEncodedUrl_withInvalidEncoded_shouldReturnError() {

        final PagedListRequest req = new PagedListRequest();

        when(bundleService.getInstalledBundleByEncodedUrl(any())).thenReturn(Optional.empty());

        final String repoUrl = "http://github.com/entando/example-qe-bundle-01";
        final String encodedRepoUrl = Base64.getEncoder().encodeToString(repoUrl.getBytes(StandardCharsets.UTF_8));
        assertThrows(BundleNotInstalledException.class,
                () -> targetService.getInstalledPluginsByEncodedUrl(req, encodedRepoUrl));

        assertThrows(DefaultProblem.class,
                () -> targetService.getInstalledPluginsByEncodedUrl(req, "432AA)°"));

    }


    @Test
    void getInstalledPluginComponent_withValidBundleIdAndPluginCode_shouldReturnPlugin() {

        final PagedListRequest req = new PagedListRequest();
        final String rolesA = "rolesA";
        final String repoUrl = "github.com/entando/example-qe-bundle-01";
        final String bundleId = BundleUtilities.getBundleId(repoUrl);
        final Optional<EntandoBundle> installedBundle = Optional.of(
                TestEntitiesGenerator.getTestEntandoBundle(TestEntitiesGenerator.getTestJob()).setRepoUrl(repoUrl));
        final String pluginName = "entandodemo-sd-customer";

        when(bundleService.getInstalledBundleByBundleId(any())).thenReturn(installedBundle);

        PluginDataEntity pluginEntity = new PluginDataEntity().setPluginCode(pluginName).setBundleId(bundleId)
                .setPluginName(pluginName).setEndpoint("ingress/path")
                .setRoles(new HashSet<String>(Arrays.asList(rolesA, "rolesB")));
        when(pluginDataRepository.findByBundleIdAndPluginName(any(), any())).thenReturn(Optional.of(pluginEntity));
        PluginData plugin = targetService.getInstalledPlugin(bundleId, pluginName);
        assertThat(plugin.getPluginName()).isEqualTo(pluginName);
        assertThat(plugin.getRoles()).contains(rolesA);

        pluginEntity = new PluginDataEntity().setPluginCode(pluginName).setBundleId(bundleId)
                .setPluginName(pluginName).setEndpoint("ingress/path")
                .setRoles(null);
        when(pluginDataRepository.findByBundleIdAndPluginName(any(), any())).thenReturn(Optional.of(pluginEntity));
        plugin = targetService.getInstalledPlugin(bundleId, pluginName);
        assertThat(plugin.getPluginName()).isEqualTo(pluginName);
        assertThat(plugin.getRoles()).isEmpty();

        pluginEntity = new PluginDataEntity().setPluginCode(pluginName).setBundleId(bundleId)
                .setPluginName(pluginName).setEndpoint("ingress/path")
                .setRoles(new HashSet<>());
        when(pluginDataRepository.findByBundleIdAndPluginName(any(), any())).thenReturn(Optional.of(pluginEntity));
        plugin = targetService.getInstalledPlugin(bundleId, pluginName);
        assertThat(plugin.getPluginName()).isEqualTo(pluginName);
        assertThat(plugin.getRoles()).isEmpty();

        when(pluginDataRepository.findByBundleIdAndPluginName(any(), any())).thenReturn(Optional.empty());
        assertThrows(DefaultProblem.class,
                () -> targetService.getInstalledPlugin(bundleId, "pn-xxxx"));

        when(bundleService.getInstalledBundleByBundleId(any())).thenReturn(Optional.empty());

        assertThrows(BundleNotInstalledException.class,
                () -> targetService.getInstalledPlugin(bundleId, "pn-xxxx"));

    }

    @Test
    void getInstalledPluginComponentByEncodedUrl_withValidEncodedUrlAndPluginCode_shouldReturnPlugin() {

        final PagedListRequest req = new PagedListRequest();
        final String repoUrl = "http://github.com/entando/example-qe-bundle-01";
        final String encodedRepoUrl = Base64.getEncoder().encodeToString(repoUrl.getBytes(StandardCharsets.UTF_8));
        final String bundleId = BundleUtilities.getBundleId(
                ValidationFunctions.composeCommonUrlOrThrow(repoUrl, "", ""));
        final Optional<EntandoBundle> installedBundle = Optional.of(
                TestEntitiesGenerator.getTestEntandoBundle(TestEntitiesGenerator.getTestJob()).setRepoUrl(repoUrl));
        final String pluginName = "entandodemo-sd-customer";
        final String pluginCode = "pn-5e6c3ca4-251faff0-" + pluginName;

        when(bundleService.getInstalledBundleByBundleId(any())).thenReturn(installedBundle);

        PluginDataEntity pluginEntity = new PluginDataEntity().setPluginCode(pluginCode).setBundleId(bundleId)
                .setPluginName(pluginName);
        when(pluginDataRepository.findByBundleIdAndPluginName(any(), any())).thenReturn(Optional.of(pluginEntity));

        PluginData plugin = targetService.getInstalledPluginByEncodedUrl(encodedRepoUrl, pluginName);

        assertThat(plugin.getPluginName()).isEqualTo(pluginName);

        when(bundleService.getInstalledBundleByBundleId(any())).thenReturn(Optional.empty());

        assertThrows(BundleNotInstalledException.class,
                () -> targetService.getInstalledPlugin(bundleId, "pn-xxxx"));

    }

    @Test
    void getInstalledPluginComponentByEncodedUrl_withInvalidEncoded_shouldReturnError() {

        final String pluginName = "entandodemo-sd-customer";
        final String pluginCode = "pn-5e6c3ca4-251faff0-" + pluginName;

        when(bundleService.getInstalledBundleByEncodedUrl(any())).thenReturn(Optional.empty());

        final String repoUrl = "http://github.com/entando/example-qe-bundle-01";
        final String encodedRepoUrl = Base64.getEncoder().encodeToString(repoUrl.getBytes(StandardCharsets.UTF_8));
        assertThrows(BundleNotInstalledException.class,
                () -> targetService.getInstalledPluginByEncodedUrl(encodedRepoUrl, pluginCode));

        assertThrows(DefaultProblem.class,
                () -> targetService.getInstalledPluginByEncodedUrl("432AA)°", pluginCode));

    }

}
