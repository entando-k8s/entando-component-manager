package org.entando.kubernetes.model.bundle.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.entando.kubernetes.TestEntitiesGenerator;
import org.entando.kubernetes.client.EntandoCoreClientTestDouble;
import org.entando.kubernetes.client.core.DefaultEntandoCoreClient;
import org.entando.kubernetes.model.bundle.BundleProperty;
import org.entando.kubernetes.model.bundle.BundleType;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;
import org.entando.kubernetes.model.bundle.installable.FileInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.reportable.Reportable;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
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

    public static final String BUNDLE_ID = "-4f58c204";

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
    public void testCreateFilesBundleV1() {
        final EntandoBundleJobEntity job = new EntandoBundleJobEntity();
        job.setComponentId("my-component-id");

        final List<? extends Installable> installables = fileProcessor.process(bundleReader);

        assertThat(installables).hasSize(5);

        assertThat(installables.get(0)).isInstanceOf(FileInstallable.class);
        assertThat(installables.get(0).getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(installables.get(0).getName()).isEqualTo("/bundles/something-4f58c204/resources/css/custom.css");

        assertThat(installables.get(1)).isInstanceOf(FileInstallable.class);
        assertThat(installables.get(1).getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(installables.get(1).getName()).isEqualTo("/bundles/something-4f58c204/resources/css/style.css");

        assertThat(installables.get(2)).isInstanceOf(FileInstallable.class);
        assertThat(installables.get(2).getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(installables.get(2).getName()).isEqualTo("/bundles/something-4f58c204/resources/js/configUiScript.js");

        assertThat(installables.get(3)).isInstanceOf(FileInstallable.class);
        assertThat(installables.get(3).getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(installables.get(3).getName()).isEqualTo("/bundles/something-4f58c204/resources/js/script.js");

        assertThat(installables.get(4)).isInstanceOf(FileInstallable.class);
        assertThat(installables.get(4).getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(installables.get(4).getName()).isEqualTo("/bundles/something-4f58c204/resources/vendor/jquery/jquery.js");
    }

    @Test
    void testCreateFilesBundleV5() throws IOException {
        Path bundleFolder = new ClassPathResource("bundle-v5").getFile().toPath();
        bundleReader = new BundleReader(bundleFolder, entandoDeBundle);

        final EntandoBundleJobEntity job = new EntandoBundleJobEntity();
        job.setComponentId("my-component-id");

        final List<? extends Installable> installables = fileProcessor.process(bundleReader);

        assertThat(installables).hasSize(3);

        assertThat(installables.get(0)).isInstanceOf(FileInstallable.class);
        assertThat(installables.get(0).getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(installables.get(0).getName()).isEqualTo(
                "/bundles/something-4f58c204/widgets/my_widget_descriptor_v5-4f58c204/assets/css-res.css");

        assertThat(installables.get(1)).isInstanceOf(FileInstallable.class);
        assertThat(installables.get(1).getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(installables.get(1).getName()).isEqualTo(
                "/bundles/something-4f58c204/widgets/my_widget_descriptor_v5-4f58c204/js-res-1.js");

        assertThat(installables.get(2)).isInstanceOf(FileInstallable.class);
        assertThat(installables.get(2).getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(installables.get(2).getName()).isEqualTo(
                "/bundles/something-4f58c204/widgets/my_widget_descriptor_v5-4f58c204/static/js/js-res-2.js");
    }

    @Test
    void whenCreatingFileInstallablesShouldOmitBundleCodeRootFolderIfSystemLevelBundleV1() throws IOException {

        execShouldOmitBundleCodeRootFolderIfSystemLevelBundle(true, BundleProperty.RESOURCES_FOLDER_PATH,
                mockBundleReader::containsResourceFolder, mockBundleReader::getResourceFiles);
    }

    @Test
    void whenCreatingFileInstallablesShouldOmitBundleCodeRootFolderIfSystemLevelBundleV5() throws IOException {

        execShouldOmitBundleCodeRootFolderIfSystemLevelBundle(false, BundleProperty.WIDGET_FOLDER_PATH,
                mockBundleReader::containsWidgetFolder, mockBundleReader::getWidgetsFiles);
    }

    private void execShouldOmitBundleCodeRootFolderIfSystemLevelBundle(boolean isV1, BundleProperty bundleProperty,
            BooleanSupplier containsFolderFn, Supplier<List<String>> fileListFn)
            throws IOException {

        List<String> resourceFiles = Arrays.asList(bundleProperty.getValue() + "ootb-widgets/static/css/main.css",
                        bundleProperty.getValue() + "ootb-widgets/static/css/sitemap.css",
                        bundleProperty.getValue() + "ootb-widgets/static/js/main.js");

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
        when(mockBundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);

        when(mockBundleReader.getResourceFileAsDescriptor(eq(bundleProperty.getValue() + "ootb-widgets/static/css/main.css")))
                .thenReturn(fileDescriptor1);
        when(mockBundleReader.getResourceFileAsDescriptor(
                eq(bundleProperty.getValue() + "ootb-widgets/static/css/sitemap.css"))).thenReturn(fileDescriptor2);
        when(mockBundleReader.getResourceFileAsDescriptor(eq(bundleProperty.getValue() + "ootb-widgets/static/js/main.js")))
                .thenReturn(fileDescriptor3);

        when(mockBundleReader.readBundleDescriptor()).thenReturn(BundleStubHelper.stubBundleDescriptor(null));
        when(containsFolderFn.getAsBoolean()).thenReturn(true);
        when(fileListFn.get()).thenReturn(resourceFiles);

        final List<Installable<FileDescriptor>> installableList = fileProcessor.process(mockBundleReader);

        final List<FileDescriptor> expected = Stream.of(fileDescriptor1, fileDescriptor2, fileDescriptor3)
                .map(desc -> {
                    desc.setFolder("/bundles/" + desc.getFolder());
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
                .asList("/bundles/resources/ootb-widgets/static/css/main.ootb.chunk.css",
                        "/bundles/resources/ootb-widgets/static/css/sitemap.css",
                        "/bundles/resources/ootb-widgets/static/js/2.ootb.chunk.js",
                        "/bundles/resources/static/css/ootb/page-templates/index.css",
                        "/bundles/resources/ootb-widgets/static/js/runtime-main.ootb.js",
                        "/bundles/resources/ootb-widgets/static/js/main.ootb.chunk.js");

        when(mockBundleReader.isBundleV1()).thenReturn(true);
        when(mockBundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);
        when(mockBundleReader.readBundleDescriptor()).thenReturn(BundleStubHelper.stubBundleDescriptor(null));
        when(mockBundleReader.getResourceFiles()).thenReturn(this.resourceFolderV1);

        Reportable reportable = fileProcessor.getReportable(mockBundleReader, fileProcessor);

        assertThat(reportable.getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(reportable.getCodes()).containsAll(expectedCodeList);
    }

    @Test
    void whenCreatingReportableShouldOmitBundleCodeRootFolderIfSystemLevelBundleV5() throws IOException {

        List<String> expectedCodeList = Arrays
                .asList("/bundles/widgets/ootb-widgets-77b2b10e/css/main.css",
                        "/bundles/widgets/ootb-widgets-77b2b10e/static/css/sitemap.css",
                        "/bundles/widgets/ootb-widgets-77b2b10e/static/js/2.ootb.chunk.js");

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
        when(mockBundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);
        when(mockBundleReader.getBundleCode()).thenReturn(bundleDescriptor.getCode());
        when(mockBundleReader.isBundleV1()).thenReturn(true);
        when(mockBundleReader.readBundleDescriptor()).thenReturn(bundleDescriptor);
        when(mockBundleReader.getResourceFiles()).thenReturn(this.resourceFolderV1);
        String signedBundleCode = BundleUtilities.composeSignedBundleFolder(mockBundleReader);

        List<String> expectedCodeList = Stream
                .of("/ootb-widgets/static/css/main.ootb.chunk.css", "/ootb-widgets/static/css/sitemap.css",
                        "/ootb-widgets/static/js/2.ootb.chunk.js", "/static/css/ootb/page-templates/index.css",
                        "/ootb-widgets/static/js/runtime-main.ootb.js", "/ootb-widgets/static/js/main.ootb.chunk.js")
                .map(s -> signedBundleCode + "/resources" + s)
                .collect(Collectors.toList());

        Reportable reportable = fileProcessor.getReportable(mockBundleReader, fileProcessor);

        assertThat(reportable.getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(reportable.getCodes()).containsAll(expectedCodeList);
    }

    @Test
    void whenCreatingReportableShouldAddBundleCodeRootFolderIfStandardBundleV5() throws IOException {

        BundleDescriptor bundleDescriptor = BundleStubHelper.stubBundleDescriptor(null);
        bundleDescriptor.setBundleType(BundleType.STANDARD_BUNDLE);

        List<String> expectedCodeList = Arrays
                .asList("/bundles/my-component-77b2b10e/widgets/ootb-widgets-77b2b10e/css/main.css",
                        "/bundles/my-component-77b2b10e/widgets/ootb-widgets-77b2b10e/static/css/sitemap.css",
                        "/bundles/my-component-77b2b10e/widgets/ootb-widgets-77b2b10e/static/js/2.ootb.chunk.js");

        when(mockBundleReader.isBundleV1()).thenReturn(false);
        when(mockBundleReader.getBundleUrl()).thenReturn(BundleInfoStubHelper.GIT_REPO_ADDRESS);
        when(mockBundleReader.readBundleDescriptor()).thenReturn(bundleDescriptor);
        when(mockBundleReader.getBundleCode()).thenReturn(bundleDescriptor.getCode());
        when(mockBundleReader.getWidgetsFiles()).thenReturn(this.resourceFolderV5);

        Reportable reportable = fileProcessor.getReportable(mockBundleReader, fileProcessor);

        assertThat(reportable.getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(reportable.getCodes()).containsAll(expectedCodeList);
    }

    @Test
    void shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing() throws IOException {

        super.shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing(
                new FileProcessor(new EntandoCoreClientTestDouble()), "asset");
    }
}
