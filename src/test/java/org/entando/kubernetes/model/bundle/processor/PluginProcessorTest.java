package org.entando.kubernetes.model.bundle.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.File;
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

    private YAMLMapper yamlMapper = new YAMLMapper();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        processor = new PluginProcessor(kubernetesService);
    }

    @Test
    void testCreatePlugin() throws IOException, ExecutionException, InterruptedException {

        initBundleReaderShortImagesName();
        final List<? extends Installable> installables = processor.process(bundleReader);
        assertOnInstallables(installables, "entando/the-lucas:0.0.1-SNAPSHOT");
    }


    @Test
    void shouldTruncatePluginBaseNameIfNameTooLong() throws IOException, ExecutionException, InterruptedException {

        initBundleReaderLongImagesName();
        final List<? extends Installable> installables = processor.process(bundleReader);
        assertOnInstallables(installables, "entando/helloworld-plugin-v1-name-too-looong:1.0.0");
    }


    private void assertOnInstallables(List<? extends Installable> installables, String firstName)
            throws ExecutionException, InterruptedException {

        assertThat(installables).hasSize(2);
        assertThat(installables.get(0)).isInstanceOf(PluginInstallable.class);
        assertThat(installables.get(0).getComponentType()).isEqualTo(ComponentType.PLUGIN);
        assertThat(installables.get(0).getName()).isEqualTo(firstName);

        assertThat(installables.get(1)).isInstanceOf(PluginInstallable.class);
        assertThat(installables.get(1).getComponentType()).isEqualTo(ComponentType.PLUGIN);
        assertThat(installables.get(1).getName()).isEqualTo("entando/the-lucas:0.0.1-SNAPSHOT");

        verify(kubernetesService, times(0)).linkPlugin(any());

        final ArgumentCaptor<EntandoPlugin> captor = ArgumentCaptor.forClass(EntandoPlugin.class);
        installables.get(0).install().get();
        installables.get(1).install().get();
        verify(kubernetesService, times(2)).linkPluginAndWaitForSuccess(captor.capture());
    }


    private void initBundleReaderShortImagesName() throws IOException {

        String firstDescriptorFilename = "plugins/pluginV1.yaml";

        PluginDescriptor descriptorV1 = PluginStubHelper.stubPluginDescriptorV1();
        when(bundleReader.readDescriptorFile(eq(firstDescriptorFilename), any()))
                .thenReturn(descriptorV1);

        this.initGenericBundleReader(firstDescriptorFilename);
    }

    private void initBundleReaderLongImagesName() throws IOException {

        String firstDescriptorFilename = "plugins/todomvcV1_docker_image_too_long.yaml";

        PluginDescriptor descriptorV1 = yamlMapper
                .readValue(new File("src/test/resources/bundle/plugins/todomvcV1_docker_image_too_long.yaml"),
                        PluginDescriptor.class);
        when(bundleReader.readDescriptorFile(eq(firstDescriptorFilename), any()))
                .thenReturn(descriptorV1);

        this.initGenericBundleReader(firstDescriptorFilename);
    }

    private void initGenericBundleReader(String longIamgeDescriptorFilename) throws IOException {

        ComponentSpecDescriptor spec = new ComponentSpecDescriptor();
        spec.setPlugins(Arrays.asList(longIamgeDescriptorFilename, "plugins/pluginV2.yaml"));

        final EntandoBundleJobEntity job = new EntandoBundleJobEntity();
        job.setComponentId("my-component-id");

        PluginDescriptor descriptorV2 = PluginStubHelper.stubPluginDescriptorV2();
        when(bundleReader.readDescriptorFile(eq("plugins/pluginV2.yaml"), any()))
                .thenReturn(descriptorV2);

        BundleDescriptor descriptor = new BundleDescriptor("my-component", "desc", spec);
        when(bundleReader.readBundleDescriptor())
                .thenReturn(descriptor);
    }
}
