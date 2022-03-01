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
import lombok.SneakyThrows;
import org.entando.kubernetes.config.AppConfiguration;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
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
import org.entando.kubernetes.stubhelper.BundleInfoStubHelper;
import org.entando.kubernetes.stubhelper.BundleStubHelper;
import org.entando.kubernetes.stubhelper.PluginStubHelper;
import org.entando.kubernetes.validator.PluginDescriptorValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@Tag("unit")
class PluginProcessorTest extends BaseProcessorTest {

    private final String pluginV2Filename = "plugins/pluginV2.yaml";

    @Mock
    private KubernetesService kubernetesService;
    @Mock
    private PluginDescriptorValidator pluginDescriptorValidator;
    @Mock
    private BundleReader bundleReader;

    private PluginProcessor processor;

    private YAMLMapper yamlMapper = new YAMLMapper();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        processor = new PluginProcessor(kubernetesService, pluginDescriptorValidator);
    }

    @Test
    void testCreatePluginV2() throws IOException, ExecutionException, InterruptedException {
        when(pluginDescriptorValidator.getFullDeploymentNameMaxlength()).thenReturn(200);
        when(bundleReader.getBundleId()).thenReturn(BundleStubHelper.BUNDLE_NAME);

        initBundleReaderShortImagesName();

        PluginDescriptor descriptorV2 = PluginStubHelper.stubPluginDescriptorV2();
        when(bundleReader.readDescriptorFile(eq("plugins/pluginV2.yaml"), any()))
                .thenReturn(descriptorV2);
        when(bundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);

        final List<? extends Installable> installables = processor.process(bundleReader);
        assertOnInstallables(installables, "pn-17f3be97-" + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA
                + "-" + BundleStubHelper.BUNDLE_NAME.replace(".", "-") + "-entando-the-lucas");
    }

    @Test
    void testCreatePluginV3() throws IOException, ExecutionException, InterruptedException {

        when(bundleReader.getBundleId()).thenReturn(BundleStubHelper.BUNDLE_NAME);
        when(pluginDescriptorValidator.getFullDeploymentNameMaxlength()).thenReturn(200);
        when(bundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);

        initBundleReaderShortImagesName();

        PluginDescriptor descriptorV3 = PluginStubHelper.stubPluginDescriptorV3();
        when(bundleReader.readDescriptorFile(eq("plugins/pluginV2.yaml"), any()))
                .thenReturn(descriptorV3);

        final List<? extends Installable> installables = processor.process(bundleReader);
        assertOnInstallables(installables,
                "pn-17f3be97-" + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA + "-" + BundleStubHelper.BUNDLE_NAME.replace(".", "-") + "-entando-the-lucas");
    }


    @Test
    void shouldThrowExceptionIfNameTooLong() throws IOException {

        when(bundleReader.getBundleId()).thenReturn(BundleStubHelper.BUNDLE_NAME);
        when(bundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);
        when(pluginDescriptorValidator.getFullDeploymentNameMaxlength()).thenReturn(200);

        AppConfiguration.truncatePluginBaseNameIfLonger = true;
        initBundleReaderLongImagesName();

        Assertions.assertThrows(EntandoComponentManagerException.class, () -> processor.process(bundleReader));

        AppConfiguration.truncatePluginBaseNameIfLonger = false;
    }

    private void assertOnInstallables(List<? extends Installable> installables, String firstName)
            throws ExecutionException, InterruptedException {

        assertThat(installables).hasSize(2);
        assertThat(installables.get(0)).isInstanceOf(PluginInstallable.class);
        assertThat(installables.get(0).getComponentType()).isEqualTo(ComponentType.PLUGIN);
        assertThat(installables.get(0).getName()).isEqualTo(firstName);

        assertThat(installables.get(1)).isInstanceOf(PluginInstallable.class);
        assertThat(installables.get(1).getComponentType()).isEqualTo(ComponentType.PLUGIN);
        assertThat(installables.get(1).getName()).isEqualTo("pn-c2c051cd-"
                + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA + "-my-bundle-name-customdepbasename");

        verify(kubernetesService, times(0)).linkPlugin(any());

        final ArgumentCaptor<EntandoPlugin> captor = ArgumentCaptor.forClass(EntandoPlugin.class);
        installables.get(0).install().get();
        installables.get(1).install().get();
        verify(kubernetesService, times(2)).linkPluginAndWaitForSuccess(captor.capture());
    }


    private void initBundleReaderShortImagesName() throws IOException {

        PluginDescriptor descriptorV1 = PluginStubHelper.stubPluginDescriptorV1();
        initBundleReaderWithTwoPlugins("plugins/pluginV1.yaml", descriptorV1);
    }

    private void initBundleReaderLongImagesName() throws IOException {

        PluginDescriptor descriptorV1 = yamlMapper
                .readValue(new File("src/test/resources/bundle/plugins/todomvcV1_docker_image_too_long.yaml"),
                        PluginDescriptor.class);

        initBundleReaderWithTwoPlugins("plugins/todomvcV1_docker_image_too_long.yaml", descriptorV1);
    }

    private void initBundleReaderWithTwoPlugins(String descriptorFilename, PluginDescriptor descriptor) throws IOException {

        when(bundleReader.readDescriptorFile(eq(descriptorFilename), any()))
                .thenReturn(descriptor);
        this.initGenericBundleReader(descriptorFilename, pluginV2Filename);
    }

    /**
     * init the bundle reader mock to return the expected plugins.
     * a plugin v2 is automatically added to the mocked plugin list
     * you have to mock other plugins outside of this method
     * @param pluginFilenames an array of filenames of plugin to add to the bundle plugin list
     */
    @SneakyThrows
    private void initGenericBundleReader(String... pluginFilenames) {

        ComponentSpecDescriptor spec = new ComponentSpecDescriptor();
        spec.setPlugins(Arrays.asList(pluginFilenames));

        final EntandoBundleJobEntity job = new EntandoBundleJobEntity();
        job.setComponentId("my-component-id");

        BundleDescriptor descriptor = BundleStubHelper.stubBundleDescriptor(spec);
        when(bundleReader.readBundleDescriptor())
                .thenReturn(descriptor);
    }

    @Test
    void shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing() {

        super.shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing(
                new PluginProcessor(null, null), "plugin");
    }

    @Test
    void shouldUseDockerImageToComposeFullDeploymentNameIfDeploymentBaseNameIsNotPresent() {
        when(pluginDescriptorValidator.getFullDeploymentNameMaxlength()).thenReturn(200);
        PluginDescriptor descriptorV2 = PluginStubHelper.stubPluginDescriptorV2()
                .setDeploymentBaseName(null);
        final String fullDepName = processor.generateFullDeploymentName(descriptorV2, BundleStubHelper.BUNDLE_NAME);
        assertThat(fullDepName).isEqualTo(
                "pn-17f3be97-" + BundleStubHelper.BUNDLE_NAME.replace(".", "-") + "-entando-the-lucas");
    }

    @Test
    void shouldUseDeploymentBaseNameOverDockerImageToComposeFullDeploymentName() {
        when(pluginDescriptorValidator.getFullDeploymentNameMaxlength()).thenReturn(200);
        String deploymentBaseName = "testDeploymentName";

        PluginDescriptor descriptorV2 = PluginStubHelper.stubPluginDescriptorV2();
        descriptorV2.setDeploymentBaseName(deploymentBaseName);
        final String fullDepName = processor.generateFullDeploymentName(descriptorV2, BundleStubHelper.BUNDLE_NAME);
        assertThat(fullDepName)
                .isEqualTo("pn-c2500115-" + BundleStubHelper.BUNDLE_NAME.replace(".", "-") + "-" + deploymentBaseName.toLowerCase());
    }
}
