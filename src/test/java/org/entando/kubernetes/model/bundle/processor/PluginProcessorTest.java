package org.entando.kubernetes.model.bundle.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.installable.PluginInstallable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.stubhelper.PluginStubHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@Tag("unit")
class PluginProcessorTest {

    @Mock
    private KubernetesService kubernetesService;
    @Mock
    private BundleReader bundleReader;

    private PluginProcessor processor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        processor = new PluginProcessor(kubernetesService);
    }

    @Test
    void testCreatePlugin() throws IOException, ExecutionException, InterruptedException {

        initBundleReader();

        final List<? extends Installable> installables = processor.process(bundleReader);

        assertThat(installables).hasSize(2);
        assertThat(installables.get(0)).isInstanceOf(PluginInstallable.class);
        assertThat(installables.get(0).getComponentType()).isEqualTo(ComponentType.PLUGIN);
        assertThat(installables.get(0).getName()).isEqualTo("entando/the-lucas:0.0.1-SNAPSHOT");

        assertThat(installables.get(1)).isInstanceOf(PluginInstallable.class);
        assertThat(installables.get(1).getComponentType()).isEqualTo(ComponentType.PLUGIN);
        assertThat(installables.get(1).getName()).isEqualTo("entando/the-lucas:0.0.1-SNAPSHOT");

        verify(kubernetesService, times(0)).linkPlugin(any());

        final ArgumentCaptor<EntandoPlugin> captor = ArgumentCaptor.forClass(EntandoPlugin.class);
        installables.get(0).install().get();
        installables.get(1).install().get();
        verify(kubernetesService, times(2)).linkPluginAndWaitForSuccess(captor.capture());

    }


    private void initBundleReader() throws IOException {

        final EntandoBundleJobEntity job = new EntandoBundleJobEntity();
        job.setComponentId("my-component-id");

        ComponentSpecDescriptor spec = new ComponentSpecDescriptor();
        spec.setPlugins(Arrays.asList("plugins/pluginV1.yaml", "plugins/pluginV2.yaml"));

        PluginDescriptor descriptorV1 = PluginStubHelper.stubPluginDescriptorV1();
        when(bundleReader.readDescriptorFile("plugins/pluginV1.yaml", PluginDescriptor.class))
                .thenReturn(descriptorV1);

        PluginDescriptor descriptorV2 = PluginStubHelper.stubPluginDescriptorV2();
        when(bundleReader.readDescriptorFile("plugins/pluginV2.yaml", PluginDescriptor.class))
                .thenReturn(descriptorV2);

        BundleDescriptor descriptor = new BundleDescriptor("my-component", "desc", spec);
        when(bundleReader.readBundleDescriptor())
                .thenReturn(descriptor);
    }
}
