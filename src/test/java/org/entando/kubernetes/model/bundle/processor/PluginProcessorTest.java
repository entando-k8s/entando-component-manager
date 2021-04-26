package org.entando.kubernetes.model.bundle.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
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
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.entando.kubernetes.config.AppConfiguration;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
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
import org.entando.kubernetes.stubhelper.BundleStubHelper;
import org.entando.kubernetes.stubhelper.PluginStubHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@Tag("unit")
class PluginProcessorTest extends BaseProcessorTest {

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
    void testCreatePluginV2() throws IOException, ExecutionException, InterruptedException {

        initBundleReaderShortImagesName();

        PluginDescriptor descriptorV2 = PluginStubHelper.stubPluginDescriptorV2();
        when(bundleReader.readDescriptorFile(eq("plugins/pluginV2.yaml"), any()))
                .thenReturn(descriptorV2);

        final List<? extends Installable> installables = processor.process(bundleReader);
        assertOnInstallables(installables, "entando-the-lucas");
    }

    @Test
    void testCreatePluginV3() throws IOException, ExecutionException, InterruptedException {

        initBundleReaderShortImagesName();

        PluginDescriptor descriptorV3 = PluginStubHelper.stubPluginDescriptorV3();
        when(bundleReader.readDescriptorFile(eq("plugins/pluginV2.yaml"), any()))
                .thenReturn(descriptorV3);

        final List<? extends Installable> installables = processor.process(bundleReader);
        assertOnInstallables(installables, "entando-the-lucas");
    }


    @Test
    void shouldTruncatePluginBaseNameIfNameTooLong() throws IOException, ExecutionException, InterruptedException {

        AppConfiguration.truncatePluginBaseNameIfLonger = true;
        initBundleReaderLongImagesName();

        PluginDescriptor descriptorV2 = PluginStubHelper.stubPluginDescriptorV2();
        when(bundleReader.readDescriptorFile(eq("plugins/pluginV2.yaml"), any()))
                .thenReturn(descriptorV2);

        final List<? extends Installable> installables = processor.process(bundleReader);
        assertOnInstallables(installables, "entando-helloworld-plugin-v1-nam");
    }

    @Test
    void shouldThrowExceptionWhenPluginDescriptorVersionNotCompliant() throws Exception {

        initBundleReaderShortImagesName();

        PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV2();
        Stream.of("va", "1", "a1", "v").forEach(version -> {
            descriptor.setDescriptorVersion(version);

            try {
                when(bundleReader.readDescriptorFile(eq("plugins/pluginV2.yaml"), any())).thenReturn(descriptor);
            } catch (Exception e) {
                fail();
            }

            assertThrows(InvalidBundleException.class, () -> processor.process(bundleReader));
        });
    }

    @Test
    void shouldThrowExceptionWhenPluginSecurityLevelIsUnknown() throws Exception {

        String firstDescriptorFilename = "plugins/plugin-unknown-security-level.yaml";

        // plugin descriptor V1
        PluginDescriptor descriptorV1 = PluginStubHelper.stubPluginDescriptorV1();
        descriptorV1.getSpec().setSecurityLevel("unknown");

        // plugin descriptor V2
        PluginDescriptor descriptorV2 = PluginStubHelper.stubPluginDescriptorV2();
        descriptorV2.setSecurityLevel("unknown");

        List<PluginDescriptor> pluginDescriptorList = Arrays.asList(descriptorV1, descriptorV2);

        for (PluginDescriptor pluginDescriptor : pluginDescriptorList) {

            when(bundleReader.readDescriptorFile(eq(firstDescriptorFilename), any()))
                    .thenReturn(pluginDescriptor);

            this.initGenericBundleReader(firstDescriptorFilename);

            Assertions.assertThrows(InvalidBundleException.class, () -> processor.process(bundleReader));
        }
    }


    private void assertOnInstallables(List<? extends Installable> installables, String firstName)
            throws ExecutionException, InterruptedException {

        assertThat(installables).hasSize(2);
        assertThat(installables.get(0)).isInstanceOf(PluginInstallable.class);
        assertThat(installables.get(0).getComponentType()).isEqualTo(ComponentType.PLUGIN);
        assertThat(installables.get(0).getName()).isEqualTo(firstName);

        assertThat(installables.get(1)).isInstanceOf(PluginInstallable.class);
        assertThat(installables.get(1).getComponentType()).isEqualTo(ComponentType.PLUGIN);
        assertThat(installables.get(1).getName()).isEqualTo(PluginStubHelper.TEST_DESCRIPTOR_DEPLOYMENT_BASE_NAME);

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

    /**
     * init the bundle reader mock to return the expected plugins.
     * a plugin v2 is automatically added to the mocked plugin list
     * you have to mock other plugins outside of this method
     * @param longImageDescriptorFilename the filename of the docker image with a name longer than the accepted
     */
    @SneakyThrows
    private void initGenericBundleReader(String longImageDescriptorFilename) {

        ComponentSpecDescriptor spec = new ComponentSpecDescriptor();
        spec.setPlugins(Arrays.asList(longImageDescriptorFilename, "plugins/pluginV2.yaml"));

        final EntandoBundleJobEntity job = new EntandoBundleJobEntity();
        job.setComponentId("my-component-id");

        BundleDescriptor descriptor = BundleStubHelper.stubBundleDescriptor(spec);
        when(bundleReader.readBundleDescriptor())
                .thenReturn(descriptor);
    }

    @Test
    void shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing() {

        super.shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing(
                new PluginProcessor(null), "plugin");
    }
}
