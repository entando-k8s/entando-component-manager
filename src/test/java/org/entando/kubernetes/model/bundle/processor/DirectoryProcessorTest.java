package org.entando.kubernetes.model.bundle.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.entando.kubernetes.TestEntitiesGenerator;
import org.entando.kubernetes.client.EntandoCoreClientTestDouble;
import org.entando.kubernetes.client.core.DefaultEntandoCoreClient;
import org.entando.kubernetes.model.bundle.BundleType;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.DescriptorVersion;
import org.entando.kubernetes.model.bundle.descriptor.DirectoryDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.VersionedDescriptor;
import org.entando.kubernetes.model.bundle.downloader.DownloadedBundle;
import org.entando.kubernetes.model.bundle.installable.DirectoryInstallable;
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
class DirectoryProcessorTest extends BaseProcessorTest {

    @Mock
    private DefaultEntandoCoreClient engineService;
    @Mock
    BundleReader mockBundleReader;
    private BundleReader bundleReader;
    private DirectoryProcessor directoryProcessor;
    private final EntandoDeBundle entandoDeBundle = TestEntitiesGenerator.getTestBundle();
    private DownloadedBundle downloadedBundle;

    private List<String> resourceFolder = Arrays
            .asList("resources/ootb-widgets", "resources/ootb-widgets/static", "resources/ootb-widgets/static/css",
                    "resources/ootb-widgets/static/js", "resources/static", "resources/static/css",
                    "resources/static/css/ootb", "resources/static/css/ootb/page-templates");
    private List<String> widgetsFolder = Arrays.asList("widgets/my-widget", "widgets/my-widget/static",
            "widgets/my-widget/static/css", "widgets/my-widget/static/js");

    @BeforeEach
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        Path bundleFolderV5 = new ClassPathResource("bundle-v5").getFile().toPath();
        downloadedBundle = new DownloadedBundle(bundleFolderV5, BundleStubHelper.BUNDLE_DIGEST);
        bundleReader = new BundleReader(downloadedBundle, entandoDeBundle);
        directoryProcessor = new DirectoryProcessor(engineService);
    }

    @Test
    void testCreateFoldersV1() throws IOException {

        Path bundleFolder = new ClassPathResource("bundle").getFile().toPath();
        DownloadedBundle downloadedBundle = new DownloadedBundle(bundleFolder, BundleStubHelper.BUNDLE_DIGEST);
        BundleReader bundleReader = new BundleReader(downloadedBundle, entandoDeBundle);

        final EntandoBundleJobEntity job = new EntandoBundleJobEntity();
        job.setComponentId("my-component-id");

        final List<? extends Installable> installables = directoryProcessor.process(bundleReader);

        assertThat(installables).hasSize(1);

        assertThat(installables.get(0)).isInstanceOf(DirectoryInstallable.class);
        assertThat(installables.get(0).getComponentType()).isEqualTo(ComponentType.DIRECTORY);
        assertThat(installables.get(0).getName()).isEqualTo("/something");
    }

    @Test
    void testCreateFoldersV5() {
        final EntandoBundleJobEntity job = new EntandoBundleJobEntity();
        job.setComponentId("my-component-id");

        bundleReader = new BundleReader(downloadedBundle, entandoDeBundle);
        final List<? extends Installable> installables = directoryProcessor.process(bundleReader);

        assertThat(installables).hasSize(1);

        assertThat(installables.get(0)).isInstanceOf(DirectoryInstallable.class);
        assertThat(installables.get(0).getComponentType()).isEqualTo(ComponentType.DIRECTORY);
        assertThat(installables.get(0).getName()).isEqualTo("bundles/something-4f58c204");
    }

    @Test
    void shouldExtractRootFolderV1() throws IOException {
        final EntandoBundleJobEntity job = new EntandoBundleJobEntity();
        job.setComponentId("my-component-id");

        Path bundleFolder = new ClassPathResource("bundle").getFile().toPath();
        DownloadedBundle downloadedBundle = new DownloadedBundle(bundleFolder, BundleStubHelper.BUNDLE_DIGEST);
        BundleReader bundleReader = new BundleReader(downloadedBundle, entandoDeBundle);

        List<DirectoryDescriptor> rootFolders = directoryProcessor
                .process(bundleReader).stream()
                .map(Installable::getRepresentation)
                .filter(DirectoryDescriptor::isRoot)
                .collect(Collectors.toList());

        assertThat(rootFolders).hasSize(1);
        assertThat(rootFolders.get(0).getName()).isEqualTo("/something");
    }


    @Test
    void whenCreatingReportableShouldOmitBundleCodeRootFolderIfSystemLevelBundleV1() throws IOException {

        List<String> expectedCodeList = Arrays
                .asList("/static/css/ootb", "/static/css/ootb/page-templates", "/static/css", "/ootb-widgets",
                        "/static", "/ootb-widgets/static/js", "/ootb-widgets/static", "/ootb-widgets/static/css");


        when(mockBundleReader.isBundleV1()).thenReturn(true);
        when(mockBundleReader.readBundleDescriptor()).thenReturn(BundleStubHelper.stubBundleDescriptor(null));
        when(mockBundleReader.getResourceFolders()).thenReturn(this.resourceFolder);

        Reportable reportable = directoryProcessor.getReportable(mockBundleReader, directoryProcessor);

        assertThat(reportable.getComponentType()).isEqualTo(ComponentType.DIRECTORY);
        assertThat(reportable.getCodes()).containsAll(expectedCodeList);
    }

    @Test
    void whenCreatingReportableShouldOmitBundleCodeRootFolderIfSystemLevelBundleV5() throws IOException {

        List<String> expectedCodeList = Stream
                .of("widgets/my-widget-" + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA,
                        "widgets/my-widget-" + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA + "/static",
                        "widgets/my-widget-" + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA + "/static/css",
                        "widgets/my-widget-" + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA + "/static/js")
                .map(s -> "bundles/" + s)
                .collect(Collectors.toList());

        final BundleDescriptor descriptor = (BundleDescriptor) BundleStubHelper.stubBundleDescriptor(null)
                .setDescriptorVersion(DescriptorVersion.V5.getVersion());
        when(mockBundleReader.readBundleDescriptor()).thenReturn(descriptor);

        when(mockBundleReader.getWidgetsFolders()).thenReturn(this.widgetsFolder);

        Reportable reportable = directoryProcessor.getReportable(mockBundleReader, directoryProcessor);

        assertThat(reportable.getComponentType()).isEqualTo(ComponentType.DIRECTORY);
        assertThat(reportable.getCodes()).containsAll(expectedCodeList);
    }

    @Test
    void whenCreatingReportableShouldAddBundleCodeRootFolderIfStandardBundleV1() throws IOException {

        BundleDescriptor bundleDescriptor = BundleStubHelper.stubBundleDescriptor(null);
        bundleDescriptor.setBundleType(BundleType.STANDARD_BUNDLE);

        // prefix each expected file path with the bundle code
        List<String> expectedCodeList = Stream
                .of("/static/css/ootb", "/static/css/ootb/page-templates", "/static/css", "/ootb-widgets",
                        "/static", "/ootb-widgets/static/js", "/ootb-widgets/static", "/ootb-widgets/static/css")
                .map(s -> "/" + bundleDescriptor.getName() + s)
                .collect(Collectors.toList());

        when(mockBundleReader.isBundleV1()).thenReturn(true);
        when(mockBundleReader.readBundleDescriptor()).thenReturn(bundleDescriptor);
        when(mockBundleReader.getResourceFolders()).thenReturn(this.resourceFolder);
        when(mockBundleReader.getCode()).thenReturn(bundleDescriptor.getCode());

        Reportable reportable = directoryProcessor.getReportable(mockBundleReader, directoryProcessor);

        assertThat(reportable.getComponentType()).isEqualTo(ComponentType.DIRECTORY);
        assertThat(reportable.getCodes()).containsAll(expectedCodeList);
    }

    @Test
    void whenCreatingReportableShouldAddBundleCodeRootFolderIfStandardBundleV5() throws IOException {

        BundleDescriptor bundleDescriptor = BundleStubHelper.stubBundleDescriptor(null);
        bundleDescriptor.setBundleType(BundleType.STANDARD_BUNDLE);
        bundleDescriptor.setDescriptorVersion(DescriptorVersion.V5.getVersion());

        // prefix each expected file path with the bundle code
        List<String> expectedCodeList = Stream
                .of("widgets/my-widget-"  + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA,
                        "widgets/my-widget-" + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA + "/static",
                        "widgets/my-widget-" + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA + "/static/css",
                        "widgets/my-widget-" + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA + "/static/js")
                .map(s -> "bundles/" + bundleDescriptor.getCode() + "/" + s)
                .collect(Collectors.toList());

        when(mockBundleReader.isBundleV1()).thenReturn(false);
        when(mockBundleReader.readBundleDescriptor()).thenReturn(bundleDescriptor);
        when(mockBundleReader.getWidgetsFolders()).thenReturn(this.widgetsFolder);

        Reportable reportable = directoryProcessor.getReportable(mockBundleReader, directoryProcessor);

        assertThat(reportable.getComponentType()).isEqualTo(ComponentType.DIRECTORY);
        assertThat(reportable.getCodes()).containsAll(expectedCodeList);
    }

    @Test
    void shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing() throws IOException {

        when(baseBundleReader.isBundleV1()).thenReturn(true);
        when(baseBundleReader.containsBundleResourceFolder()).thenReturn(true);

        super.shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing(
                new DirectoryProcessor(new EntandoCoreClientTestDouble()), "directory");
    }

    @Test
    void shouldReturnTheExpectedNonRootDirectoryDescriptorFromTheEntandoBundleComponentJobEntity() {

        String compId = "bundles/new-bundle-test-f13d35ad/widgets/table-widget/static/js/main.js";

        EntandoBundleComponentJobEntity entity = new EntandoBundleComponentJobEntity();
        entity.setComponentId(compId);

        final DirectoryDescriptor descriptor = directoryProcessor.buildDescriptorFromComponentJob(entity);
        assertThat(descriptor.getName()).isEqualTo(compId);
        assertThat(descriptor.isRoot()).isFalse();
    }

    @Test
    void shouldReturnTheExpectedRootDirectoryDescriptorFromTheEntandoBundleComponentJobEntity() {

        String compId = "bundles/new-bundle-test-f13d35ad";

        EntandoBundleComponentJobEntity entity = new EntandoBundleComponentJobEntity();
        entity.setComponentId(compId);

        final DirectoryDescriptor descriptor = directoryProcessor.buildDescriptorFromComponentJob(entity);
        assertThat(descriptor.getName()).isEqualTo(compId);
        assertThat(descriptor.isRoot()).isTrue();
    }
}
