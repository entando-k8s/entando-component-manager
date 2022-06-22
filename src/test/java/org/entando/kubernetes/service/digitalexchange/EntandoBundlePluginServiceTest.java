package org.entando.kubernetes.service.digitalexchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.entando.kubernetes.TestEntitiesGenerator;
import org.entando.kubernetes.exception.digitalexchange.BundleNotInstalledException;
import org.entando.kubernetes.model.bundle.EntandoBundle;
import org.entando.kubernetes.model.job.PluginData;
import org.entando.kubernetes.model.job.PluginDataEntity;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.repository.PluginDataRepository;
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

    @BeforeEach
    public void setup() {
        pluginDataRepository = Mockito.mock(PluginDataRepository.class);
        bundleService = Mockito.mock(EntandoBundleService.class);
        targetService = new EntandoBundlePluginServiceImpl(pluginDataRepository, bundleService);
    }

    @AfterEach
    public void teardown() {
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
        final String repoUrl = "github.com/entando/example-qe-bundle-01";
        final String bundleId = BundleUtilities.getBundleId(repoUrl);
        final Optional<EntandoBundle> installedBundle = Optional.of(
                TestEntitiesGenerator.getTestEntandoBundle(TestEntitiesGenerator.getTestJob()).setRepoUrl(repoUrl));
        final String pluginName = "entandodemo-sd-customer";

        when(bundleService.getInstalledBundleByBundleId(any())).thenReturn(installedBundle);

        PluginDataEntity pluginEntity = new PluginDataEntity().setPluginCode(pluginName).setBundleId(bundleId)
                .setPluginName(pluginName);
        when(pluginDataRepository.findByBundleIdAndPluginName(any(), any())).thenReturn(Optional.of(pluginEntity));

        PluginData plugin = targetService.getInstalledPlugin(bundleId, pluginName);

        assertThat(plugin.getPluginName()).isEqualTo(pluginName);

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