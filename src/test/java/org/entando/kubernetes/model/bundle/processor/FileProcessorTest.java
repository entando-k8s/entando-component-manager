package org.entando.kubernetes.model.bundle.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.entando.kubernetes.TestEntitiesGenerator;
import org.entando.kubernetes.client.core.DefaultEntandoCoreClient;
import org.entando.kubernetes.model.bundle.BundleProperty;
import org.entando.kubernetes.model.bundle.BundleType;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.DescriptorVersion;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;
import org.entando.kubernetes.model.bundle.installable.FileInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.reportable.Reportable;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.stubhelper.BundleInfoStubHelper;
import org.entando.kubernetes.stubhelper.BundleStubHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ClassPathResource;

@Tag("unit")
class FileProcessorTest extends BaseProcessorTest {

    @Mock
    private DefaultEntandoCoreClient engineService;
    @Mock
    BundleReader mockBundleReader;
    private BundleReader bundleReader;
    private FileProcessor fileProcessor;
    private final EntandoDeBundle entandoDeBundle = TestEntitiesGenerator.getTestBundle();

    private List<String> resourceFolderV1 = Arrays.asList("resources/ootb-widgets/static/css/main.ootb.chunk.css",
            "resources/ootb-widgets/static/css/sitemap.css",
            "resources/ootb-widgets/static/js/2.ootb.chunk.js",
            "resources/static/css/ootb/page-templates/index.css",
            "resources/ootb-widgets/static/js/runtime-main.ootb.js",
            "resources/ootb-widgets/static/js/main.ootb.chunk.js");
    private List<String> resourceFolderV5 = Arrays.asList("widgets/ootb-widgets/css/main.css",
            "widgets/ootb-widgets/static/css/sitemap.css",
            "widgets/ootb-widgets/static/js/2.ootb.chunk.js");

    @BeforeEach
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        Path bundleFolder = new ClassPathResource("bundle").getFile().toPath();
        bundleReader = new BundleReader(bundleFolder, entandoDeBundle);
        fileProcessor = new FileProcessor(engineService);
    }

    @Test
    void testCreateFilesBundleV1() {
        final EntandoBundleJobEntity job = new EntandoBundleJobEntity();
        job.setComponentId("my-component-id");

        final List<? extends Installable> installables = fileProcessor
                .process(bundleReader);

        assertThat(installables).hasSize(5);

        assertThat(installables.get(0)).isInstanceOf(FileInstallable.class);
        assertThat(installables.get(0).getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(installables.get(0).getName()).isEqualTo("/something/css/custom.css");

        assertThat(installables.get(1)).isInstanceOf(FileInstallable.class);
        assertThat(installables.get(1).getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(installables.get(1).getName()).isEqualTo("/something/css/style.css");

        assertThat(installables.get(2)).isInstanceOf(FileInstallable.class);
        assertThat(installables.get(2).getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(installables.get(2).getName()).isEqualTo("/something/js/configUiScript.js");

        assertThat(installables.get(3)).isInstanceOf(FileInstallable.class);
        assertThat(installables.get(3).getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(installables.get(3).getName()).isEqualTo("/something/js/script.js");

        assertThat(installables.get(4)).isInstanceOf(FileInstallable.class);
        assertThat(installables.get(4).getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(installables.get(4).getName()).isEqualTo("/something/vendor/jquery/jquery.js");
    }

    @Test
    void testCreateFilesBundleV5() throws IOException {
        Path bundleFolder = new ClassPathResource("bundle-v5").getFile().toPath();
        bundleReader = new BundleReader(bundleFolder, entandoDeBundle);

        final EntandoBundleJobEntity job = new EntandoBundleJobEntity();
        job.setComponentId("my-component-id");

        final List<? extends Installable> installables = fileProcessor.process(bundleReader);

        assertThat(installables).hasSize(11);

        var expectedNames = Stream.of(
                "bundles/something-4f58c204/widgets/my_widget_app_builder_descriptor_v5-4f58c204/assets/css-res.css",
                "bundles/something-4f58c204/widgets/my_widget_app_builder_descriptor_v5-4f58c204/js-res-1.js",
                "bundles/something-4f58c204/widgets/my_widget_app_builder_descriptor_v5-4f58c204/media/generic-file.txt",
                "bundles/something-4f58c204/widgets/my_widget_app_builder_descriptor_v5-4f58c204/static/js/js-res-2.js",
                "bundles/something-4f58c204/widgets/my_widget_config_descriptor_v5-4f58c204/assets/css-res.css",
                "bundles/something-4f58c204/widgets/my_widget_config_descriptor_v5-4f58c204/js-res-1.js",
                "bundles/something-4f58c204/widgets/my_widget_config_descriptor_v5-4f58c204/static/js/js-res-2.js",
                "bundles/something-4f58c204/widgets/my_widget_descriptor_v5-4f58c204/assets/css-res.css",
                "bundles/something-4f58c204/widgets/my_widget_descriptor_v5-4f58c204/js-res-1.js",
                "bundles/something-4f58c204/widgets/my_widget_descriptor_v5-4f58c204/media/generic-file.txt",
                "bundles/something-4f58c204/widgets/my_widget_descriptor_v5-4f58c204/static/js/js-res-2.js"
        ).sorted().collect(Collectors.toList());

        for (int i = 0, e = expectedNames.size(); i < e; i++) {
            assertThat(installables.get(i)).isInstanceOf(FileInstallable.class);
            assertThat(installables.get(i).getComponentType()).isEqualTo(ComponentType.RESOURCE);
            assertThat(installables.get(i).getName()).isEqualTo(expectedNames.get(i));
        }
    }

    @Test
    void whenCreatingFileInstallablesShouldOmitBundleCodeRootFolderIfSystemLevelBundleV1() throws IOException {

        execShouldOmitBundleCodeRootFolderIfSystemLevelBundle(true, BundleProperty.RESOURCES_FOLDER_PATH,
                mockBundleReader::containsResourceFolder, mockBundleReader::getResourceFiles);
    }

    @Test
    void whenCreatingFileInstallablesShouldOmitBundleCodeRootFolderIfSystemLevelBundleV5() throws IOException {
        when(mockBundleReader.getWidgetsBaseFolders()).thenReturn(Collections.singletonList(
                new ClassPathResource("bundle-v5/widgets/my_widget_descriptor_v5").getFile().getAbsolutePath()));

        when(mockBundleReader.getResourceOfType(any(), any())).thenReturn(Arrays.asList(
                BundleProperty.WIDGET_FOLDER_PATH.getValue() + "ootb-widgets/static/css/main.css",
                BundleProperty.WIDGET_FOLDER_PATH.getValue() + "ootb-widgets/static/css/sitemap.css",
                BundleProperty.WIDGET_FOLDER_PATH.getValue() + "ootb-widgets/static/js/main.js"));
        when(mockBundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);

        execShouldOmitBundleCodeRootFolderIfSystemLevelBundle(false, BundleProperty.WIDGET_FOLDER_PATH,
                mockBundleReader::containsWidgetFolder, mockBundleReader::getWidgetsFiles);
    }

    private void execShouldOmitBundleCodeRootFolderIfSystemLevelBundle(boolean isV1, BundleProperty bundleProperty,
            BooleanSupplier containsFolderFn, Supplier<List<String>> fileListFn)
            throws IOException {

        FileDescriptor fileDescriptor1 = FileDescriptor.builder()
                .folder(bundleProperty.getValue() + "ootb-widgets/static/css")
                .filename("main.css")
                .base64("asdfdfjdkfljadjslka")
                .build();
        FileDescriptor fileDescriptor2 = FileDescriptor.builder()
                .folder(bundleProperty.getValue() + "ootb-widgets/static/css")
                .filename("sitemap.css")
                .base64("asdfdfjdkfljadjslka")
                .build();
        FileDescriptor fileDescriptor3 = FileDescriptor.builder()
                .folder(bundleProperty.getValue() + "ootb-widgets/static/js")
                .filename("main.js")
                .base64("asdfdfjdkfljadjslka")
                .build();

        when(mockBundleReader.isBundleV1()).thenReturn(isV1);

        when(mockBundleReader.getResourceFileAsDescriptor(
                bundleProperty.getValue() + "ootb-widgets/static/css/main.css"))
                .thenReturn(fileDescriptor1);
        when(mockBundleReader.getResourceFileAsDescriptor(
                bundleProperty.getValue() + "ootb-widgets/static/css/sitemap.css")).thenReturn(fileDescriptor2);
        when(mockBundleReader.getResourceFileAsDescriptor(bundleProperty.getValue() + "ootb-widgets/static/js/main.js"))
                .thenReturn(fileDescriptor3);

        BundleDescriptor bundleDescriptor = BundleStubHelper.stubBundleDescriptor(null);
        bundleDescriptor.setDescriptorVersion(
                isV1 ? DescriptorVersion.V1.getVersion() : DescriptorVersion.V5.getVersion());
        when(mockBundleReader.readBundleDescriptor()).thenReturn(bundleDescriptor);
        when(containsFolderFn.getAsBoolean()).thenReturn(true);

        List<String> resourceFiles = Arrays.asList(bundleProperty.getValue() + "ootb-widgets/static/css/main.css",
                bundleProperty.getValue() + "ootb-widgets/static/css/sitemap.css",
                bundleProperty.getValue() + "ootb-widgets/static/js/main.js");

        lenient().when(fileListFn.get()).thenReturn(resourceFiles);

        final List<Installable<FileDescriptor>> installableList = fileProcessor.process(mockBundleReader);

        final List<FileDescriptor> expected = Stream.of(fileDescriptor1, fileDescriptor2, fileDescriptor3)
                .map(desc -> {
                    desc.setFolder("bundles/" + desc.getFolder());
                    return desc;
                })
                .collect(Collectors.toList());

        assertThat(installableList.get(0).getRepresentation()).isEqualToComparingFieldByField(expected.get(0));
        assertThat(installableList.get(1).getRepresentation()).isEqualToComparingFieldByField(expected.get(1));
        assertThat(installableList.get(2).getRepresentation()).isEqualToComparingFieldByField(expected.get(2));
    }

    @Test
    public void shouldConvertEntandoBundleComponentJobToDescriptor() {
        EntandoBundleComponentJobEntity bundleComponentJob = new EntandoBundleComponentJobEntity();
        bundleComponentJob.setComponentId("/my-app/static/js/lib.js");
        FileDescriptor fileDescriptor = this.fileProcessor.buildDescriptorFromComponentJob(bundleComponentJob);
        Assertions.assertThat(fileDescriptor.getFilename()).isEqualTo("lib.js");
        Assertions.assertThat(fileDescriptor.getFolder()).isEqualTo("/my-app/static/js");
    }

    @Test
    void whenCreatingReportableShouldOmitBundleCodeRootFolderIfSystemLevelBundleV1() throws IOException {

        List<String> expectedCodeList = Arrays
                .asList("/ootb-widgets/static/css/main.ootb.chunk.css",
                        "/ootb-widgets/static/css/sitemap.css",
                        "/ootb-widgets/static/js/2.ootb.chunk.js",
                        "/static/css/ootb/page-templates/index.css",
                        "/ootb-widgets/static/js/runtime-main.ootb.js",
                        "/ootb-widgets/static/js/main.ootb.chunk.js");

        when(mockBundleReader.isBundleV1()).thenReturn(true);
        when(mockBundleReader.containsResourceFolder()).thenReturn(true);
        when(mockBundleReader.readBundleDescriptor()).thenReturn(BundleStubHelper.stubBundleDescriptor(null));
        when(mockBundleReader.getResourceFiles()).thenReturn(this.resourceFolderV1);

        Reportable reportable = fileProcessor.getReportable(mockBundleReader, fileProcessor);

        assertThat(reportable.getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(reportable.getCodes()).containsAll(expectedCodeList);
    }

    @Test
    void whenCreatingReportableShouldOmitBundleCodeRootFolderIfSystemLevelBundleV5() throws IOException {

        List<String> expectedCodeList = Arrays
                .asList("bundles/widgets/ootb-widgets-77b2b10e/css/main.css",
                        "bundles/widgets/ootb-widgets-77b2b10e/static/css/sitemap.css",
                        "bundles/widgets/ootb-widgets-77b2b10e/static/js/2.ootb.chunk.js");

        when(mockBundleReader.isBundleV1()).thenReturn(false);
        when(mockBundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);
        when(mockBundleReader.readBundleDescriptor()).thenReturn(BundleStubHelper.stubBundleDescriptor(null));
        when(mockBundleReader.getWidgetsFiles()).thenReturn(this.resourceFolderV5);

        Reportable reportable = fileProcessor.getReportable(mockBundleReader, fileProcessor);

        assertThat(reportable.getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(reportable.getCodes()).containsAll(expectedCodeList);
    }

    @Test
    void whenCreatingReportableShouldAddBundleCodeRootFolderIfStandardBundleV1() throws IOException {

        BundleDescriptor bundleDescriptor = BundleStubHelper.stubBundleDescriptor(null);
        bundleDescriptor.setBundleType(BundleType.STANDARD_BUNDLE);

        // prefix each expected file path with the bundle code
        when(mockBundleReader.isBundleV1()).thenReturn(true);
        when(mockBundleReader.getBundleName()).thenReturn(BundleStubHelper.BUNDLE_NAME);
        when(mockBundleReader.containsResourceFolder()).thenReturn(true);
        when(mockBundleReader.readBundleDescriptor()).thenReturn(bundleDescriptor);
        when(mockBundleReader.getResourceFiles()).thenReturn(this.resourceFolderV1);

        List<String> expectedCodeList = Stream
                .of("/ootb-widgets/static/css/main.ootb.chunk.css", "/ootb-widgets/static/css/sitemap.css",
                        "/ootb-widgets/static/js/2.ootb.chunk.js", "/static/css/ootb/page-templates/index.css",
                        "/ootb-widgets/static/js/runtime-main.ootb.js", "/ootb-widgets/static/js/main.ootb.chunk.js")
                .map(s -> "/" + BundleStubHelper.BUNDLE_NAME + s)
                .collect(Collectors.toList());

        Reportable reportable = fileProcessor.getReportable(mockBundleReader, fileProcessor);

        assertThat(reportable.getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(reportable.getCodes()).containsAll(expectedCodeList);
    }

    @Test
    void whenCreatingReportableShouldAddBundleCodeRootFolderIfStandardBundleV5() throws IOException {

        BundleDescriptor bundleDescriptor = BundleStubHelper.stubBundleDescriptor(null);
        bundleDescriptor.setBundleType(BundleType.STANDARD_BUNDLE);
        bundleDescriptor.setDescriptorVersion(DescriptorVersion.V5.getVersion());

        List<String> expectedCodeList = Arrays
                .asList("bundles/my-component-77b2b10e/widgets/ootb-widgets-77b2b10e/css/main.css",
                        "bundles/my-component-77b2b10e/widgets/ootb-widgets-77b2b10e/static/css/sitemap.css",
                        "bundles/my-component-77b2b10e/widgets/ootb-widgets-77b2b10e/static/js/2.ootb.chunk.js");

        when(mockBundleReader.isBundleV1()).thenReturn(false);
        when(mockBundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);
        when(mockBundleReader.readBundleDescriptor()).thenReturn(bundleDescriptor);
        when(mockBundleReader.getCode()).thenReturn(
                bundleDescriptor.getCode() + "-" + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);
        when(mockBundleReader.getWidgetsFiles()).thenReturn(this.resourceFolderV5);

        Reportable reportable = fileProcessor.getReportable(mockBundleReader, fileProcessor);

        assertThat(reportable.getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(reportable.getCodes()).containsAll(expectedCodeList);
    }
}
