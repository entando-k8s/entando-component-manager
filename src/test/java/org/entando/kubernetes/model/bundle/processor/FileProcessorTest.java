package org.entando.kubernetes.model.bundle.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.entando.kubernetes.client.EntandoCoreClientTestDouble;
import org.entando.kubernetes.client.core.DefaultEntandoCoreClient;
import org.entando.kubernetes.model.bundle.BundleType;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;
import org.entando.kubernetes.model.bundle.installable.FileInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.reportable.Reportable;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
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

    private List<String> resourceFolder = Arrays.asList("resources/ootb-widgets/static/css/main.ootb.chunk.css",
            "resources/ootb-widgets/static/css/sitemap.css",
            "resources/ootb-widgets/static/js/2.ootb.chunk.js",
            "resources/static/css/ootb/page-templates/index.css",
            "resources/ootb-widgets/static/js/runtime-main.ootb.js",
            "resources/ootb-widgets/static/js/main.ootb.chunk.js");

    @BeforeEach
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        Path bundleFolder = new ClassPathResource("bundle").getFile().toPath();
        bundleReader = new BundleReader(bundleFolder);
        fileProcessor = new FileProcessor(engineService);
    }

    @Test
    public void testCreateFiles() throws IOException {
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
    public void shouldConvertEntandoBundleComponentJobToDescriptor() {
        EntandoBundleComponentJobEntity bundleComponentJob = new EntandoBundleComponentJobEntity();
        bundleComponentJob.setComponentId("/my-app/static/js/lib.js");
        FileDescriptor fileDescriptor = this.fileProcessor.buildDescriptorFromComponentJob(bundleComponentJob);
        Assertions.assertThat(fileDescriptor.getFilename()).isEqualTo("lib.js");
        Assertions.assertThat(fileDescriptor.getFolder()).isEqualTo("/my-app/static/js");

    }



    @Test
    void whenCreatingReportableShouldOmitBundleCodeRootFolderIfSystemLevelBundle() throws IOException {

        List<String> expectedCodeList = Arrays
                .asList("/ootb-widgets/static/css/main.ootb.chunk.css", "/ootb-widgets/static/css/sitemap.css",
                        "/ootb-widgets/static/js/2.ootb.chunk.js", "/static/css/ootb/page-templates/index.css",
                        "/ootb-widgets/static/js/runtime-main.ootb.js", "/ootb-widgets/static/js/main.ootb.chunk.js");

        when(mockBundleReader.readBundleDescriptor()).thenReturn(BundleStubHelper.stubBundleDescriptor(null));
        when(mockBundleReader.containsResourceFolder()).thenReturn(true);
        when(mockBundleReader.getResourceFiles()).thenReturn(this.resourceFolder);

        Reportable reportable = fileProcessor.getReportable(mockBundleReader, fileProcessor);

        assertThat(reportable.getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(reportable.getCodes()).containsAll(expectedCodeList);
    }

    @Test
    void whenCreatingReportableShouldAddBundleCodeRootFolderIfStandardBundle() throws IOException {

        BundleDescriptor bundleDescriptor = BundleStubHelper.stubBundleDescriptor(null);
        bundleDescriptor.setBundleType(BundleType.STANDARD_BUNDLE);

        // prefix each expected file path with the bundle code
        List<String> expectedCodeList = Stream
                .of("/ootb-widgets/static/css/main.ootb.chunk.css", "/ootb-widgets/static/css/sitemap.css",
                        "/ootb-widgets/static/js/2.ootb.chunk.js", "/static/css/ootb/page-templates/index.css",
                        "/ootb-widgets/static/js/runtime-main.ootb.js", "/ootb-widgets/static/js/main.ootb.chunk.js")
                .map(s -> "/" + bundleDescriptor.getCode() + s)
                .collect(Collectors.toList());

        when(mockBundleReader.readBundleDescriptor()).thenReturn(bundleDescriptor);
        when(mockBundleReader.getBundleCode()).thenReturn(bundleDescriptor.getCode());
        when(mockBundleReader.containsResourceFolder()).thenReturn(true);
        when(mockBundleReader.getResourceFiles()).thenReturn(this.resourceFolder);

        Reportable reportable = fileProcessor.getReportable(mockBundleReader, fileProcessor);

        assertThat(reportable.getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(reportable.getCodes()).containsAll(expectedCodeList);
    }

    @Test
    void shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing() throws IOException {

        when(baseBundleReader.containsResourceFolder()).thenReturn(true);

        super.shouldReturnMeaningfulErrorIfExceptionAriseDuringProcessing(
                new FileProcessor(new EntandoCoreClientTestDouble()), "asset");
    }
}
