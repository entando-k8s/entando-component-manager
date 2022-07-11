package org.entando.kubernetes.service.digitalexchange.job;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.fabric8.kubernetes.client.KubernetesClientException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.entando.kubernetes.model.bundle.EntandoBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpecBuilder;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTagBuilder;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleService;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleServiceImpl;
import org.entando.kubernetes.service.digitalexchange.job.PostInitServiceImpl.PostInitData;
import org.entando.kubernetes.service.digitalexchange.job.PostInitServiceImpl.PostInitItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class PostInitServiceTest {

    private EntandoBundleService bundleService;
    private EntandoBundleInstallService installService;
    private KubernetesService kubernetesService;

    private PostInitServiceImpl serviceToTest;

    private static final String POST_INIT_BUNDLE_VERSION = "0.0.1";
    private static final String POST_INIT_BUNDLE_NAME = "test-bundle-entando-post-init-01";
    private static final String POST_INIT_BUNDLE_PUBLICATION_URL = "docker://docker.io/entando/post-init";
    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    public void setup() throws Exception {
        bundleService = Mockito.mock(EntandoBundleServiceImpl.class);
        installService = Mockito.mock(EntandoBundleInstallService.class);
        kubernetesService = Mockito.mock(KubernetesService.class);

    }

    @AfterEach
    public void teardown() {
    }

    @Test
    void kubernetesServiceAppStatusError_ShouldReturnStatusUnknow() throws Exception {
        initServiceToTest(convertConfigDataToString(convertConfigDataToString()));

        when(kubernetesService.getCurrentAppStatusPhase()).thenReturn("undefined");
        serviceToTest.install();
        assertThat(serviceToTest.getStatus()).isEqualTo(PostInitStatus.UNKNOWN);

        when(kubernetesService.getCurrentAppStatusPhase()).thenThrow(
                new KubernetesClientException("error 404 retrieve status"));

        serviceToTest.install();
        assertThat(serviceToTest.getStatus()).isEqualTo(PostInitStatus.UNKNOWN);
        assertThat(serviceToTest.isCompleted()).isTrue();
        assertThat(serviceToTest.shouldRetry()).isTrue();

    }

    @Test
    void kubernetesCRwithDataError_ShouldReturnStatusFailed() throws Exception {
        initServiceToTest(convertConfigDataToString(convertConfigDataToString()));

        when(kubernetesService.getCurrentAppStatusPhase()).thenReturn("successful");
        when(bundleService.listBundles()).thenReturn(new PagedMetadata(new PagedListRequest(), new ArrayList<>(), 0));
        when(bundleService.deployDeBundle(any())).thenReturn(new EntandoBundle());
        when(kubernetesService.fetchBundleByName(any())).thenReturn(Optional.empty());

        serviceToTest.install();
        assertThat(serviceToTest.getStatus()).isEqualTo(PostInitStatus.FAILED);
        assertThat(serviceToTest.isCompleted()).isTrue();
        assertThat(serviceToTest.shouldRetry()).isFalse();

        EntandoDeBundle deBundle = new EntandoDeBundle();
        EntandoDeBundleSpecBuilder deBundleBuilder = new EntandoDeBundleSpecBuilder();
        EntandoDeBundleTag tag = (new EntandoDeBundleTagBuilder()).withVersion("0.0.0")
                .withTarball("docker://docker.io/test/test").build();
        deBundle.setSpec(deBundleBuilder.addToTags(tag).build());
        when(kubernetesService.fetchBundleByName(any())).thenReturn(Optional.of(deBundle));
        serviceToTest.install();
        assertThat(serviceToTest.getStatus()).isEqualTo(PostInitStatus.FAILED);
        assertThat(serviceToTest.isCompleted()).isTrue();
        assertThat(serviceToTest.shouldRetry()).isFalse();
    }

    @Test
    void postInit_ShouldReturnConfigData() throws Exception {
        initServiceToTest(convertConfigDataToString(convertConfigDataToString()));

        PostInitItem item = serviceToTest.getConfigurationData().getItems().get(0);
        assertThat(item.getName()).isEqualTo(POST_INIT_BUNDLE_NAME);

        initServiceToTest("");
        assertThat(serviceToTest.getConfigurationData().getItems()).isNotEmpty();

        initServiceToTest("{test}");
        assertThat(serviceToTest.getConfigurationData().getItems()).isNotEmpty();

    }

    @Test
    void postInit_ShouldNotInstall() throws Exception {
        /*
        EntandoBundleInstallService installServiceSpy = Mockito.spy(installService);

        PostInitData data = convertConfigDataToString();
        data.getItems().get(0).setAction(new String[]{"deploy-only"});
        initServiceToTest(convertConfigDataToString(data), bundleService, installServiceSpy, kubernetesService);

        when(kubernetesService.getCurrentAppStatusPhase()).thenReturn("successful");
        when(bundleService.listBundles()).thenReturn(new PagedMetadata(new PagedListRequest(), new ArrayList<>(), 0));
        when(bundleService.deployDeBundle(any())).thenReturn(new EntandoBundle());

        EntandoDeBundle deBundle = new EntandoDeBundle();
        EntandoDeBundleSpecBuilder deBundleBuilder = new EntandoDeBundleSpecBuilder();
        EntandoDeBundleTag tag = (new EntandoDeBundleTagBuilder()).withVersion(POST_INIT_BUNDLE_VERSION)
                .withTarball("docker://docker.io/test/test").build();
        deBundle.setSpec(deBundleBuilder.addToTags(tag).build());
        when(kubernetesService.fetchBundleByName(any())).thenReturn(Optional.of(deBundle));

        serviceToTest.install();
        assertThat(serviceToTest.getStatus()).isEqualTo(PostInitStatus.SUCCESSFUL);
        assertThat(serviceToTest.isCompleted()).isTrue();
        assertThat(serviceToTest.shouldRetry()).isFalse();
    verify(installService, times(0)).install(any(), any(), any());
    */

    }

    @Test
    void isBundleOperationAllowed_shouldWork() throws Exception {
        initServiceToTest(convertConfigDataToString(convertConfigDataToString()));

        Optional<Boolean> resp = serviceToTest.isBundleOperationAllowed("ciccio", "test");
        assertThat(resp.isEmpty()).isTrue();

        String bundleCode = BundleUtilities.composeBundleCode(POST_INIT_BUNDLE_NAME,
                BundleUtilities.removeProtocolAndGetBundleId(POST_INIT_BUNDLE_PUBLICATION_URL));
        resp = serviceToTest.isBundleOperationAllowed(bundleCode, "test");
        assertThat(resp.get()).isFalse();

        initServiceToTest(convertConfigDataToString(convertConfigDataToString()));

    }

    @Test
    void postInit_ShouldReturnOk() throws Exception {
        initServiceToTest(convertConfigDataToString(convertConfigDataToString()));

        when(kubernetesService.getCurrentAppStatusPhase()).thenReturn("successful");
        when(bundleService.listBundles()).thenReturn(new PagedMetadata(new PagedListRequest(), new ArrayList<>(), 0));
        when(bundleService.deployDeBundle(any())).thenReturn(new EntandoBundle());

        EntandoDeBundle deBundle = new EntandoDeBundle();
        EntandoDeBundleSpecBuilder deBundleBuilder = new EntandoDeBundleSpecBuilder();
        EntandoDeBundleTag tag = (new EntandoDeBundleTagBuilder()).withVersion(POST_INIT_BUNDLE_VERSION)
                .withTarball("docker://docker.io/test/test").build();
        deBundle.setSpec(deBundleBuilder.addToTags(tag).build());
        when(kubernetesService.fetchBundleByName(any())).thenReturn(Optional.of(deBundle));

        serviceToTest.install();
        assertThat(serviceToTest.getStatus()).isEqualTo(PostInitStatus.SUCCESSFUL);
        assertThat(serviceToTest.isCompleted()).isTrue();
        assertThat(serviceToTest.shouldRetry()).isFalse();

    }

    private void initServiceToTest(String configData) throws Exception {
        serviceToTest = new PostInitServiceImpl(configData, bundleService, installService, kubernetesService);
        serviceToTest.afterPropertiesSet();
    }

    private void initServiceToTest(String configData, EntandoBundleService bundleService,
            EntandoBundleInstallService installService,
            KubernetesService kubernetesService) throws Exception {
        serviceToTest = new PostInitServiceImpl(configData, bundleService, installService, kubernetesService);
        serviceToTest.afterPropertiesSet();
    }

    private String convertConfigDataToString(PostInitData configData) throws JsonProcessingException {
        return mapper.writeValueAsString(configData);
    }

    private PostInitData convertConfigDataToString() {
        List<PostInitItem> items = new ArrayList<>();
        items.add(PostInitItem.builder()
                .name(POST_INIT_BUNDLE_NAME)
                .url(POST_INIT_BUNDLE_PUBLICATION_URL)
                .version(POST_INIT_BUNDLE_VERSION)
                .action("install-or-update")
                .priority(1)
                .build());
        return PostInitData.builder().frequency(3).items(items).build();

    }
}
