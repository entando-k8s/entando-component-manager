package org.entando.kubernetes.model.bundle.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.entando.kubernetes.config.AppConfiguration;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.DescriptorVersion;
import org.entando.kubernetes.model.bundle.descriptor.plugin.EnvironmentVariable;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.installable.PluginInstallable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.repository.PluginDataRepository;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.crane.CraneCommand;
import org.entando.kubernetes.stubhelper.BundleInfoStubHelper;
import org.entando.kubernetes.stubhelper.BundleStubHelper;
import org.entando.kubernetes.stubhelper.PluginStubHelper;
import org.entando.kubernetes.utils.TestUtils;
import org.entando.kubernetes.validator.descriptor.PluginDescriptorValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testcontainers.shaded.org.apache.commons.lang3.ObjectUtils;

@Tag("unit")
class PluginProcessorTest extends BaseProcessorTest {

    private final String pluginV2Filename = "plugins/pluginV2.yaml";

    @Mock
    private KubernetesService kubernetesService;
    @Mock
    private PluginDescriptorValidator pluginDescriptorValidator;
    @Mock
    private PluginDataRepository pluginDataRepository;
    @Mock
    private BundleReader bundleReader;
    @Mock
    private CraneCommand craneCommand;

    private PluginProcessor processor;

    private YAMLMapper yamlMapper = new YAMLMapper();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        processor = new PluginProcessor(kubernetesService, pluginDescriptorValidator, pluginDataRepository, craneCommand);
    }

    @Test
    void testCreatePluginV2() throws IOException, ExecutionException, InterruptedException {
        final PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV2();
        descriptor.setDescriptorVersion(DescriptorVersion.V2.getVersion());

        final List<? extends Installable> installables = execTestCreatePlugin(descriptor,
                BundleInfoStubHelper.GIT_REPO_ADDRESS, BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);
        assertOnEndpoints(installables.get(0), "/entando/the-lucas/0-0-1-snapshot", null);
    }

    @Test
    void testCreatePluginV2WithCustomIngressPath() throws IOException, ExecutionException, InterruptedException {
        final PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV2();
        descriptor.setIngressPath("custom/ingress");
        descriptor.setDescriptorVersion(DescriptorVersion.V2.getVersion());

        final List<? extends Installable> installables = execTestCreatePlugin(descriptor,
                BundleInfoStubHelper.GIT_REPO_ADDRESS, BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);
        assertOnEndpoints(installables.get(0), "/entando/the-lucas/0-0-1-snapshot", null);
        assertOnEndpoints(installables.get(1), "/custom/ingress", null);
    }

    @Test
    void testCreatePluginV3() throws IOException, ExecutionException, InterruptedException {
        final List<? extends Installable> installables = execTestCreatePlugin(PluginStubHelper.stubPluginDescriptorV3(),
                BundleInfoStubHelper.GIT_REPO_ADDRESS, BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);
        assertOnEndpoints(installables.get(0), "/entando/the-lucas/0-0-1-snapshot", null);
        assertOnEndpoints(installables.get(1), "/entando/the-lucas", null);
    }


    @Test
    void testCreatePluginV3WithCustomIngressPath() throws IOException, ExecutionException, InterruptedException {
        final PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV3();
        descriptor.setIngressPath("custom/ingress");
        final List<? extends Installable> installables = execTestCreatePlugin(descriptor,
                BundleInfoStubHelper.GIT_REPO_ADDRESS, BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);
        assertOnEndpoints(installables.get(0), "/entando/the-lucas/0-0-1-snapshot", null);
        assertOnEndpoints(installables.get(1), "/custom/ingress", null);
    }

    @Test
    void testCreatePluginV4() throws IOException, ExecutionException, InterruptedException {
        final List<? extends Installable> installables = execTestCreatePlugin(PluginStubHelper.stubPluginDescriptorV4(),
                BundleInfoStubHelper.GIT_REPO_ADDRESS, BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);
        assertOnEndpoints(installables.get(0), "/entando/the-lucas/0-0-1-snapshot", null);
        assertOnEndpoints(installables.get(1), "/entando/the-lucas", null);
    }

    @Test
    void testCreatePluginV4WithCustomIngressPath() throws IOException, ExecutionException, InterruptedException {
        final PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV4();
        descriptor.setIngressPath("custom/ingress");
        final List<? extends Installable> installables = execTestCreatePlugin(descriptor,
                BundleInfoStubHelper.GIT_REPO_ADDRESS, BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);
        assertOnEndpoints(installables.get(0), "/entando/the-lucas/0-0-1-snapshot", null);
        assertOnEndpoints(installables.get(1), "/custom/ingress", null);
    }

    @Test
    void testCreatePluginV5() throws IOException, ExecutionException, InterruptedException {

        when(pluginDescriptorValidator.getFullDeploymentNameMaxlength()).thenReturn(200);
        when(bundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.DOCKER_REPO_ADDRESS);

        when(bundleReader.readDescriptorFile(eq("plugins/pluginV2.yaml"), any()))
                .thenReturn(PluginStubHelper.stubPluginDescriptorV5());

        initBundleReaderShortImagesName();

        PluginDescriptor descriptorV1 = PluginStubHelper.stubPluginDescriptorV1();
        descriptorV1.setDescriptorVersion(DescriptorVersion.V1.getVersion());

        ComponentSpecDescriptor spec = new ComponentSpecDescriptor();
        spec.setPlugins(Arrays.asList("plugins/pluginV1.yaml", pluginV2Filename));

        final EntandoBundleJobEntity job = new EntandoBundleJobEntity();
        job.setComponentId("my-component-id");

        BundleDescriptor descriptor = BundleStubHelper.stubBundleDescriptor(spec)
                .setCode("my-component-89f28dad");
        when(bundleReader.readBundleDescriptor())
                .thenReturn(descriptor);

        String repoSha = BundleInfoStubHelper.DOCKER_REPO_ADDRESS_8_CHARS_SHA;
        final List<? extends Installable> installables = processor.process(bundleReader);
        assertOnInstallables(installables, String.format("pn-%s-%s-entando-the-lucas",
                        repoSha, "24f085aa"),
                repoSha);

        assertOnEndpoints(installables.get(0), "/entando/the-lucas/0-0-1-snapshot", null);
        assertOnEndpoints(installables.get(1), "/my-component-89f28dad/my-bundle", null);
    }

    @Test
    void testCreatePluginV5WithCustomIngressPath() throws IOException, ExecutionException, InterruptedException {
        final PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV5().setIngressPath("custom/ingress");
        final List<? extends Installable> installables = execTestCreatePlugin(descriptor,
                BundleInfoStubHelper.GIT_REPO_ADDRESS, BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);
        assertOnEndpoints(installables.get(0), "/entando/the-lucas/0-0-1-snapshot", null);
        assertOnEndpoints(installables.get(1), "/my-component-77b2b10e/my-bundle", "/custom/ingress");
    }

    private List<? extends Installable> execTestCreatePlugin(PluginDescriptor descriptor, String bundleAddress,
            String repoSha) throws IOException, ExecutionException, InterruptedException {
        // ~
        when(pluginDescriptorValidator.getFullDeploymentNameMaxlength()).thenReturn(200);
        when(bundleReader.getBundleUrl()).thenReturn(bundleAddress);

        when(bundleReader.readDescriptorFile(eq("plugins/pluginV2.yaml"), any()))
                .thenReturn(descriptor);

        initBundleReaderShortImagesName();

        final List<? extends Installable> installables = processor.process(bundleReader);
        assertOnInstallables(installables, String.format("pn-%s-%s-entando-the-lucas",
                        repoSha, "24f085aa"),
                repoSha);
        return installables;
    }


    @Test
    void shouldThrowExceptionIfNameTooLong() throws IOException {

        when(bundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);
        when(pluginDescriptorValidator.getFullDeploymentNameMaxlength()).thenReturn(200);

        AppConfiguration.truncatePluginBaseNameIfLonger = true;
        initBundleReaderLongImagesName();

        Assertions.assertThrows(EntandoComponentManagerException.class, () -> processor.process(bundleReader));

        AppConfiguration.truncatePluginBaseNameIfLonger = false;
    }

    private void assertOnInstallables(List<? extends Installable> installables, String firstName, String repoSha)
            throws ExecutionException, InterruptedException {

        assertThat(installables).hasSize(2);
        assertThat(installables.get(0)).isInstanceOf(PluginInstallable.class);
        assertThat(installables.get(0).getComponentType()).isEqualTo(ComponentType.PLUGIN);
        assertThat(installables.get(0).getName()).isEqualTo(firstName);

        assertThat(installables.get(1)).isInstanceOf(PluginInstallable.class);
        assertThat(installables.get(1).getComponentType()).isEqualTo(ComponentType.PLUGIN);
        assertThat(installables.get(1).getName()).isEqualTo("pn-" + repoSha + "-b9cd65aa-customdepbasename");

        verify(kubernetesService, times(0)).linkPlugin(any());

        final ArgumentCaptor<EntandoPlugin> captor = ArgumentCaptor.forClass(EntandoPlugin.class);
        installables.get(0).install().get();
        installables.get(1).install().get();
        verify(kubernetesService, times(2)).linkPluginAndWaitForSuccess(captor.capture(), anyBoolean());
    }

    private void initBundleReaderShortImagesName() throws IOException {

        PluginDescriptor descriptorV1 = PluginStubHelper.stubPluginDescriptorV1();
        descriptorV1.setDescriptorVersion(DescriptorVersion.V1.getVersion());
        initBundleReaderWithTwoPlugins("plugins/pluginV1.yaml", descriptorV1);
    }

    private void initBundleReaderLongImagesName() throws IOException {

        PluginDescriptor descriptorV1 = yamlMapper
                .readValue(new File("src/test/resources/bundle/plugins/todomvcV1_docker_image_too_long.yaml"),
                        PluginDescriptor.class);
        descriptorV1.setDescriptorVersion(DescriptorVersion.V1.getVersion());

        initBundleReaderWithTwoPlugins("plugins/todomvcV1_docker_image_too_long.yaml", descriptorV1);
    }

    private void initBundleReaderWithTwoPlugins(String descriptorFilename, PluginDescriptor descriptor) throws IOException {

        when(bundleReader.readDescriptorFile(eq(descriptorFilename), any())).thenReturn(descriptor);
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
                new PluginProcessor(null, null, null, craneCommand), "plugin");
    }

    @Test
    void shouldUseDockerImageToComposePluginIdIfDeploymentBaseNameIsNotPresent() {
        PluginDescriptor descriptorV2 = PluginStubHelper.stubPluginDescriptorV2()
                .setDeploymentBaseName(null);
        final String pluginId = processor.signPluginDeploymentName(descriptorV2);
        assertThat(pluginId).isEqualTo("24f085aa-entando-the-lucas");
    }

    @Test
    void shouldUseDeploymentBaseNameOverDockerImageToComposePluginId() {
        String deploymentBaseName = "testDeploymentName";

        PluginDescriptor descriptorV2 = PluginStubHelper.stubPluginDescriptorV2();
        descriptorV2.setDeploymentBaseName(deploymentBaseName);
        final String fullDepName = processor.signPluginDeploymentName(descriptorV2);
        assertThat(fullDepName).isEqualTo(String.format("%s-%s", "50fe6023", deploymentBaseName.toLowerCase()));
    }

    @Test
    void shouldUseSignPluginCorrectly() {
        PluginDescriptor descriptor = PluginStubHelper.stubPluginDescriptorV2()
                .setImage("staging/entando/the-lucas:0.0.1-SNAPSHOT")
                .setDeploymentBaseName(null);

        Stream.of(new String[]{"org1/org2/name", "abd00576-org1-org2-name"},
                        new String[]{"org1/org2/org3/name", "061fe78c-org1-org2-org3-name"},
                        new String[]{"example.org/org1/name", "bb388b64-org1-name"},
                        new String[]{"example.org/org1/org2/name", "abd00576-org1-org2-name"},
                        new String[]{"org1/name", "bb388b64-org1-name"})
                .forEach(i -> {
                    descriptor.setImage(i[0]);
                    descriptor.setDockerImage(null);

                    assertThat(processor.signPluginDeploymentName(descriptor)).isEqualTo(i[1]);
                });
    }

    @Test
    void shouldComposeTheExpectedFullDeploymentName() {
        when(pluginDescriptorValidator.getFullDeploymentNameMaxlength()).thenReturn(200);
        final String fullDepName = processor.generateFullDeploymentName(
                BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA,
                "24f085aa-entando-the-lucas");
        assertThat(fullDepName).isEqualTo(String.format("pn-%s-%s-entando-the-lucas",
                BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA, "24f085aa"));
    }

    @Test
    void shouldAddTheCmEndpointEnvVarHttps() throws Exception {

        TestUtils.setEnv(Map.of("SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_OIDC_ISSUER_URI",
                "http://www.mykc.com/auth/realms/entando",
                "SERVER_SERVLET_CONTEXT_PATH", "/digital-exchange",
                "ENTANDO_APP_HOST_NAME", "www.myentando.com",
                "ENTANDO_APP_USE_TLS", "true"));

        processor = new PluginProcessor(kubernetesService, pluginDescriptorValidator, pluginDataRepository, craneCommand);

        final List<? extends Installable> installablesHttps = execTestCreatePlugin(
                PluginStubHelper.stubPluginDescriptorV5(), BundleInfoStubHelper.GIT_REPO_ADDRESS,
                BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);

        final PluginDescriptor representationHttps = (PluginDescriptor) installablesHttps.get(0).getRepresentation();
        final EnvironmentVariable environmentVariableHttps = representationHttps.getEnvironmentVariables().get(0);
        assertThat(environmentVariableHttps.getName()).isEqualTo("ENTANDO_ECR_INGRESS_URL");
        assertThat(environmentVariableHttps.getValue()).isEqualTo("https://www.myentando.com/digital-exchange");
    }

    @Test
    void shouldAddTheCmEndpointEnvVarHttp() throws Exception {

        TestUtils.setEnv(Map.of("SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_OIDC_ISSUER_URI",
                "http://www.mykc.com/auth/realms/entando",
                "SERVER_SERVLET_CONTEXT_PATH", "/digital-exchange",
                "ENTANDO_APP_HOST_NAME", "www.myentando.com",
                "ENTANDO_APP_USE_TLS", "false"));

        processor = new PluginProcessor(kubernetesService, pluginDescriptorValidator, pluginDataRepository, craneCommand);

        final List<? extends Installable> installablesHttp = execTestCreatePlugin(
                PluginStubHelper.stubPluginDescriptorV5(), BundleInfoStubHelper.GIT_REPO_ADDRESS,
                BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);

        final PluginDescriptor representationHttp = (PluginDescriptor) installablesHttp.get(0).getRepresentation();
        final EnvironmentVariable environmentVariableHttp = representationHttp.getEnvironmentVariables().get(0);
        assertThat(environmentVariableHttp.getName()).isEqualTo("ENTANDO_ECR_INGRESS_URL");
        assertThat(environmentVariableHttp.getValue()).isEqualTo("http://www.myentando.com/digital-exchange");

    }

    private void assertOnEndpoints(Installable installable, String endpoint, String customEndpoint) {
        var metadata = ((PluginDescriptor) installable.getRepresentation()).getDescriptorMetadata();
        assertThat(metadata.getEndpoint()).isEqualTo(endpoint);

        if (ObjectUtils.isEmpty(customEndpoint)) {
            assertThat(metadata.getCustomEndpoint()).isNull();
        } else {
            assertThat(metadata.getCustomEndpoint()).isEqualTo(customEndpoint);
        }
    }
}
