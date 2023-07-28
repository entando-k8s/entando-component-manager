package org.entando.kubernetes.model.bundle.installable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.ExecutionException;
import org.entando.kubernetes.client.PluginDataRepositoryTestDouble;
import org.entando.kubernetes.config.tenant.thread.TenantContextHolder;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor.DescriptorMetadata;
import org.entando.kubernetes.model.job.PluginDataEntity;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.repository.PluginDataRepository;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.stubhelper.PluginStubHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class PluginInstallableTest {

    private PluginInstallable pluginInstallable;
    private PluginDataRepository pluginDataRepository;
    @Mock
    private KubernetesService kubernetesService;


    @BeforeEach
    public void setup() {
        pluginDataRepository = spy(PluginDataRepositoryTestDouble.class);
    }

    @Test
    void shouldCreateNewRecordWhileInstallingPluginWithCreateAction() throws ExecutionException, InterruptedException {
        // given an empty db
        assertThat(pluginDataRepository.count()).isZero();

        ArgumentCaptor<EntandoPlugin> pluginCaptor = ArgumentCaptor.forClass(EntandoPlugin.class);
        doNothing().when(kubernetesService).linkPluginAndWaitForSuccess(pluginCaptor.capture());

        // when the plugin is installed in CREATE mode
        PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV5();
        pluginInstallable = new PluginInstallable(kubernetesService, descriptor, InstallAction.CREATE,
                pluginDataRepository);
        pluginInstallable.install().get();

        // then the linkPluginAndWaitForSuccess has been called as expected
        verify(kubernetesService, times(1)).linkPluginAndWaitForSuccess(pluginCaptor.capture());

        // and the db contains only 1 record
        assertThat(pluginDataRepository.count()).isEqualTo(1);

        // and the record has been correctly populated
        final DescriptorMetadata metadata = descriptor.getDescriptorMetadata();
        final PluginDataEntity pluginDataEntity = pluginDataRepository.findByBundleIdAndPluginName(
                        metadata.getBundleId(),
                        metadata.getPluginName())
                .get();
        assertThat(pluginDataEntity.getBundleId()).isEqualTo(metadata.getBundleId());
        assertThat(pluginDataEntity.getPluginName()).isEqualTo(metadata.getPluginName());
        assertThat(pluginDataEntity.getPluginCode()).isEqualTo(metadata.getPluginCode());
        assertThat(pluginDataEntity.getEndpoint()).isEqualTo(metadata.getEndpoint());
        assertThat(pluginDataEntity.getDockerTag()).isEqualTo(descriptor.getDockerImage().getTag());
        assertThat(pluginDataEntity.getDockerSha256()).isEqualTo(descriptor.getDockerImage().getSha256());
    }

    @Test
    void shouldUpdateAnExistingRecordWhileInstallingPluginWithOverrideAction()
            throws ExecutionException, InterruptedException {

        // given a record already present in the db
        assertThat(pluginDataRepository.count()).isZero();
        populateDbWithStartingRecords();
        assertThat(pluginDataRepository.count()).isEqualTo(1);

        ArgumentCaptor<EntandoPlugin> pluginCaptor = ArgumentCaptor.forClass(EntandoPlugin.class);
        doNothing().when(kubernetesService).linkPluginAndWaitForSuccess(pluginCaptor.capture());
        ArgumentCaptor<String> pluginNameCaptor = ArgumentCaptor.forClass(String.class);
        doNothing().when(kubernetesService).unlink(pluginNameCaptor.capture());

        // when the plugin is installed in OVERRIDE mode
        PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV5();
        pluginInstallable = new PluginInstallable(kubernetesService, descriptor, InstallAction.OVERRIDE,
                pluginDataRepository);
        pluginInstallable.install().get();

        // then the unlink and linkPluginAndWaitForSuccess have been called as expected
        verify(kubernetesService, times(1)).unlink(pluginNameCaptor.capture());
        verify(kubernetesService, times(1)).linkPluginAndWaitForSuccess(pluginCaptor.capture());

        // and the db contains only 1 record
        assertThat(pluginDataRepository.count()).isEqualTo(1);

        // and the record has been correctly updated
        final DescriptorMetadata metadata = descriptor.getDescriptorMetadata();
        final PluginDataEntity pluginDataEntity = pluginDataRepository.findByBundleIdAndPluginName(
                        metadata.getBundleId(),
                        metadata.getPluginName())
                .get();
        assertThat(pluginDataEntity.getBundleId()).isEqualTo(metadata.getBundleId());
        assertThat(pluginDataEntity.getPluginName()).isEqualTo(metadata.getPluginName());
        assertThat(pluginDataEntity.getPluginCode()).isEqualTo(metadata.getPluginCode());
        assertThat(pluginDataEntity.getEndpoint()).isEqualTo(metadata.getEndpoint());
        assertThat(pluginDataEntity.getDockerTag()).isEqualTo(descriptor.getDockerImage().getTag());
        assertThat(pluginDataEntity.getDockerSha256()).isEqualTo(descriptor.getDockerImage().getSha256());
    }

    @Test
    void shouldDeleteAnExistingRecordWhileUninstallingPluginWithCreateAction()
            throws ExecutionException, InterruptedException {

        // given a record already present in the db
        assertThat(pluginDataRepository.count()).isZero();
        populateDbWithStartingRecords();
        assertThat(pluginDataRepository.count()).isEqualTo(1);

        ArgumentCaptor<String> pluginCodeCaptor = ArgumentCaptor.forClass(String.class);
        doNothing().when(kubernetesService).unlinkAndScaleDownPlugin(pluginCodeCaptor.capture());

        // when the plugin is uninstalled in CREATE mode
        PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV5();
        pluginInstallable = new PluginInstallable(kubernetesService, descriptor, InstallAction.CREATE,
                pluginDataRepository);
        pluginInstallable.uninstallFromEcr().get();

        // then the unlink and linkPluginAndWaitForSuccess have been called as expected
        verify(kubernetesService, times(1)).unlinkAndScaleDownPlugin(pluginCodeCaptor.capture());

        // and the db contains exactly 0 records
        assertThat(pluginDataRepository.count()).isZero();
    }

    private void populateDbWithStartingRecords() {
        PluginDataEntity startingPluginDataEntity = new PluginDataEntity()
                .setBundleId(PluginStubHelper.BUNDLE_ID)
                .setPluginName(PluginStubHelper.EXPECTED_PLUGIN_NAME)
                .setPluginCode(
                        PluginStubHelper.TEST_DESCRIPTOR_IMAGE_SHA + "-" + PluginStubHelper.EXPECTED_PLUGIN_NAME)
                .setEndpoint("my-endpoint")
                .setDockerTag("MY-TAG")
                .setDockerSha256("MY-SHA");
        pluginDataRepository.save(startingPluginDataEntity);
    }
}
