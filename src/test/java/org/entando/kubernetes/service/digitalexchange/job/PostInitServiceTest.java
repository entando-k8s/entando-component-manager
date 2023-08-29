package org.entando.kubernetes.service.digitalexchange.job;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fabric8.kubernetes.client.KubernetesClientException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.entando.kubernetes.config.tenant.TenantConfigDTO;
import org.entando.kubernetes.config.tenant.TenantConfiguration.PrimaryTenantConfig;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.model.bundle.EntandoBundle;
import org.entando.kubernetes.model.common.EntandoMultiTenancy;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpecBuilder;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTagBuilder;
import org.entando.kubernetes.model.job.EntandoBundleJob;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobStatus;
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
    private EntandoBundleJobService entandoBundleJobService;
    private PostInitServiceImpl serviceToTest;

    private static final String POST_INIT_BUNDLE_VERSION = "0.0.2";
    private static final String POST_INIT_BUNDLE_NAME = "test-bundle-entando-post-init-01";
    private static final String POST_INIT_BUNDLE_PUBLICATION_URL = "docker://docker.io/entando/post-init";
    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    public void setup() throws Exception {
        bundleService = Mockito.mock(EntandoBundleServiceImpl.class);
        installService = Mockito.mock(EntandoBundleInstallService.class);
        kubernetesService = Mockito.mock(KubernetesService.class);
        entandoBundleJobService = Mockito.mock(EntandoBundleJobService.class);

    }

    @AfterEach
    public void teardown() {
    }

    @Test
    void kubernetesServiceAppStatusError_ShouldReturnStatusUnknow() throws Exception {
        initServiceToTest(convertConfigDataToString(convertConfigDataToString()), Collections.singletonList(buildPrimaryTenantConfig()));

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
        initServiceToTest(convertConfigDataToString(convertConfigDataToString()), Collections.singletonList(buildPrimaryTenantConfig()));

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
    void postInitDefault_ShouldInstall() throws Exception {

        initServiceToTest(null, Collections.singletonList(buildPrimaryTenantConfig()),
                bundleService, installService, kubernetesService,
                entandoBundleJobService);

        EntandoDeBundle deBundle = new EntandoDeBundle();
        EntandoDeBundleSpecBuilder deBundleBuilder = new EntandoDeBundleSpecBuilder();
        EntandoDeBundleTag tag = (new EntandoDeBundleTagBuilder()).withVersion("1.0.2")
                .withTarball("docker://docker.io/entando/post-init-01").build();
        deBundle.setSpec(deBundleBuilder.addToTags(tag).build());
        when(kubernetesService.fetchBundleByName(any())).thenReturn(Optional.of(deBundle));
        when(kubernetesService.getCurrentAppStatusPhase()).thenReturn("successful");

        when(bundleService.listBundles()).thenReturn(
                new PagedMetadata(new PagedListRequest(), new ArrayList<>(), 0));
        when(bundleService.deployDeBundle(any())).thenReturn(new EntandoBundle());
        when(installService.install(any(), any(), any())).thenReturn(
                EntandoBundleJobEntity.builder().id(UUID.randomUUID()).build());
        when(entandoBundleJobService.getById(any())).thenReturn(
                Optional.of(EntandoBundleJobEntity.builder().status(JobStatus.INSTALL_COMPLETED).build()));

        serviceToTest.install();
        assertThat(serviceToTest.getStatus()).isEqualTo(PostInitStatus.SUCCESSFUL);
        assertThat(serviceToTest.isCompleted()).isTrue();
        assertThat(serviceToTest.shouldRetry()).isFalse();
        verify(bundleService, times(1)).deployDeBundle(any());
        verify(installService, times(1)).install(any(), any(), any());

    }

    @Test
    void postInit_WithoutActionShouldNotInstall() throws Exception {

        PostInitData data = convertConfigDataToString();
        data.getItems().get(0).setAction(null);
        initServiceToTest(convertConfigDataToString(data), Collections.singletonList(buildPrimaryTenantConfig()),
                bundleService, installService, kubernetesService,
                entandoBundleJobService);

        when(kubernetesService.getCurrentAppStatusPhase()).thenReturn("successful");
        when(bundleService.listBundles()).thenReturn(
                new PagedMetadata(new PagedListRequest(), new ArrayList<>(), 0));
        when(bundleService.deployDeBundle(any())).thenReturn(new EntandoBundle());

        EntandoDeBundle deBundle = new EntandoDeBundle();
        EntandoDeBundleSpecBuilder deBundleBuilder = new EntandoDeBundleSpecBuilder();
        EntandoDeBundleTag tag = (new EntandoDeBundleTagBuilder()).withVersion(POST_INIT_BUNDLE_VERSION)
                .withTarball("docker://docker.io/test/test").build();
        deBundle.setSpec(deBundleBuilder.addToTags(tag).build());
        //when(kubernetesService.fetchBundleByName(any())).thenReturn(Optional.of(deBundle));

        serviceToTest.install();
        assertThat(serviceToTest.getStatus()).isEqualTo(PostInitStatus.SUCCESSFUL);
        assertThat(serviceToTest.isCompleted()).isTrue();
        assertThat(serviceToTest.shouldRetry()).isFalse();
        verify(bundleService, times(1)).deployDeBundle(any());
        verify(installService, times(0)).install(any(), any(), any());
    }

    @Test
    void postInit_errorInputConfig_ShouldNotInstall() throws Exception {
        initServiceToTest("{%/()}", Collections.singletonList(buildPrimaryTenantConfig()),
                bundleService, installService, kubernetesService,
                entandoBundleJobService);
        assertThat(serviceToTest.getFrequencyInSeconds()).isEqualTo(5);

        initServiceToTest(" ", Collections.singletonList(buildPrimaryTenantConfig()),
                bundleService, installService, kubernetesService,
                entandoBundleJobService);
        assertThat(serviceToTest.getFrequencyInSeconds()).isEqualTo(5);

    }

    @Test
    void postInit_errorInputName_ShouldNotInstall() throws Exception {
        EntandoBundleInstallService installServiceSpy = Mockito.spy(installService);

        PostInitData data = convertConfigDataToString();
        data.getItems().get(0).setName("%$qw123");
        initServiceToTest(convertConfigDataToString(data), Collections.singletonList(buildPrimaryTenantConfig()),
                bundleService, installServiceSpy, kubernetesService,
                entandoBundleJobService);

        when(kubernetesService.getCurrentAppStatusPhase()).thenReturn("successful");
        when(bundleService.listBundles()).thenReturn(new PagedMetadata(new PagedListRequest(), new ArrayList<>(), 0));

        serviceToTest.install();
        assertThat(serviceToTest.getStatus()).isEqualTo(PostInitStatus.FAILED);
        assertThat(serviceToTest.isCompleted()).isTrue();
        assertThat(serviceToTest.shouldRetry()).isFalse();
        verify(installServiceSpy, times(0)).install(any(), any(), any());

        data.getItems().get(0).setName("-123456.");
        initServiceToTest(convertConfigDataToString(data), Collections.singletonList(buildPrimaryTenantConfig()),
                bundleService, installServiceSpy, kubernetesService,
                entandoBundleJobService);

        when(kubernetesService.getCurrentAppStatusPhase()).thenReturn("successful");
        when(bundleService.listBundles()).thenReturn(new PagedMetadata(new PagedListRequest(), new ArrayList<>(), 0));

        serviceToTest.install();
        assertThat(serviceToTest.getStatus()).isEqualTo(PostInitStatus.FAILED);
        assertThat(serviceToTest.isCompleted()).isTrue();
        assertThat(serviceToTest.shouldRetry()).isFalse();
        verify(installServiceSpy, times(0)).install(any(), any(), any());

    }

    @Test
    void isBundleOperationAllowed_shouldWork() throws Exception {
        initServiceToTest(convertConfigDataToString(convertConfigDataToString()), Collections.singletonList(buildPrimaryTenantConfig()));

        Optional<Boolean> resp = serviceToTest.isEcrActionAllowed("ciccio", "test");
        assertThat(resp.isEmpty()).isTrue();

        String bundleCode = BundleUtilities.composeBundleCode(POST_INIT_BUNDLE_NAME,
                BundleUtilities.removeProtocolAndGetBundleId(POST_INIT_BUNDLE_PUBLICATION_URL));
        resp = serviceToTest.isEcrActionAllowed(bundleCode, "test");
        assertThat(resp.get()).isFalse();

        PostInitData data = convertConfigDataToString();
        PostInitItem item = data.getItems().get(0);
        item.setEcrActions(new String[0]);
        initServiceToTest(convertConfigDataToString(data), Collections.singletonList(buildPrimaryTenantConfig()));
        resp = serviceToTest.isEcrActionAllowed(bundleCode, "uninstall");
        assertThat(resp.get()).isFalse();

        item.setEcrActions(new String[]{"undeploy"});
        initServiceToTest(convertConfigDataToString(data), Collections.singletonList(buildPrimaryTenantConfig()));
        resp = serviceToTest.isEcrActionAllowed(bundleCode, "uninstall");
        assertThat(resp.get()).isFalse();

        item.setEcrActions(new String[]{"uninstall"});
        initServiceToTest(convertConfigDataToString(data), Collections.singletonList(buildPrimaryTenantConfig()));
        resp = serviceToTest.isEcrActionAllowed(bundleCode, "uninstall");
        assertThat(resp.get()).isTrue();

    }

    @Test
    void postInit_ShouldReturnOk() throws Exception {
        initServiceToTest(convertConfigDataToString(convertConfigDataToString()), Collections.singletonList(buildPrimaryTenantConfig()));

        when(kubernetesService.getCurrentAppStatusPhase()).thenReturn("successful");
        when(bundleService.listBundles()).thenReturn(new PagedMetadata(new PagedListRequest(), new ArrayList<>(), 0));
        when(bundleService.deployDeBundle(any())).thenReturn(new EntandoBundle());

        EntandoBundleJobEntity job = EntandoBundleJobEntity.builder().id(UUID.randomUUID())
                .status(JobStatus.INSTALL_COMPLETED).build();
        when(installService.install(any(), any(), any())).thenReturn(job);
        when(entandoBundleJobService.getById(any())).thenReturn(Optional.of(job));

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

    @Test
    void postInit_ShouldUpdate() throws Exception {

        initServiceToTest(convertConfigDataToString(convertConfigDataToString()),
                Collections.singletonList(buildPrimaryTenantConfig()),
                bundleService, installService,
                kubernetesService,
                entandoBundleJobService);

        when(kubernetesService.getCurrentAppStatusPhase()).thenReturn("successful");

        String bundleCode = BundleUtilities.composeBundleCode(POST_INIT_BUNDLE_NAME,
                BundleUtilities.removeProtocolAndGetBundleId(POST_INIT_BUNDLE_PUBLICATION_URL));
        EntandoBundle installedBundle = EntandoBundle.builder().code(bundleCode)
                .repoUrl(POST_INIT_BUNDLE_PUBLICATION_URL)
                .installedJob(EntandoBundleJob.builder().componentVersion("0.0.1").status(JobStatus.INSTALL_COMPLETED)
                        .build()).build();
        when(bundleService.listBundles()).thenReturn(new PagedMetadata(new PagedListRequest(),
                Collections.singletonList(installedBundle), 0));

        EntandoBundleJobEntity job = EntandoBundleJobEntity.builder().id(UUID.randomUUID())
                .status(JobStatus.INSTALL_COMPLETED).build();
        when(installService.install(any(), any(), eq(InstallAction.OVERRIDE))).thenReturn(job);
        when(entandoBundleJobService.getById(any())).thenReturn(Optional.of(job));

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

        verify(installService, times(1)).install(any(), any(), eq(InstallAction.OVERRIDE));

    }


    private void initServiceToTest(String configData, List<TenantConfigDTO> tenantConfigurations) throws Exception {
        serviceToTest = new PostInitServiceImpl(configData, tenantConfigurations, bundleService, installService, kubernetesService,
                entandoBundleJobService);
        serviceToTest.afterPropertiesSet();
    }

    private void initServiceToTest(String configData,
                                   List<TenantConfigDTO> tenantConfigurations,
                                   EntandoBundleService bundleService,
                                   EntandoBundleInstallService installService,
            KubernetesService kubernetesService, EntandoBundleJobService entandoBundleJobService)
            throws Exception {
        serviceToTest = new PostInitServiceImpl(configData, tenantConfigurations,
                bundleService, installService, kubernetesService,
                entandoBundleJobService);
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

    private PrimaryTenantConfig buildPrimaryTenantConfig() {
        return new PrimaryTenantConfig()
                .setTenantCode(EntandoMultiTenancy.PRIMARY_TENANT)
                .setKcRealm("entando")
                .setFqdns("localhost");
    }
}
