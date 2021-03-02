package org.entando.kubernetes.model.bundle.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.entando.kubernetes.client.core.DefaultEntandoCoreClient;
import org.entando.kubernetes.model.bundle.BundleType;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.DirectoryDescriptor;
import org.entando.kubernetes.model.bundle.installable.DirectoryInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.reportable.Reportable;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.stubhelper.BundleStubHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ClassPathResource;

@Tag("unit")
public class DirectoryProcessorTest {

    @Mock
    private DefaultEntandoCoreClient engineService;
    @Mock
    BundleReader mockBundleReader;
    private BundleReader bundleReader;
    private DirectoryProcessor directoryProcessor;

    private List<String> resourceFolder = Arrays
            .asList("resources/ootb-widgets", "resources/ootb-widgets/static", "resources/ootb-widgets/static/css",
                    "resources/ootb-widgets/static/js", "resources/static", "resources/static/css",
                    "resources/static/css/ootb", "resources/static/css/ootb/page-templates");

    @BeforeEach
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        Path bundleFolder = new ClassPathResource("bundle").getFile().toPath();
        bundleReader = new BundleReader(bundleFolder);
        directoryProcessor = new DirectoryProcessor(engineService);
    }

    @Test
    public void testCreateFolders() throws IOException {
        final EntandoBundleJobEntity job = new EntandoBundleJobEntity();
        job.setComponentId("my-component-id");

        final List<? extends Installable> installables = directoryProcessor.process(bundleReader);

        assertThat(installables).hasSize(1);

        assertThat(installables.get(0)).isInstanceOf(DirectoryInstallable.class);
        assertThat(installables.get(0).getComponentType()).isEqualTo(ComponentType.DIRECTORY);
        assertThat(installables.get(0).getName()).isEqualTo("/something");
    }

    @Test
    public void shouldExtractRootFolder() {
        final EntandoBundleJobEntity job = new EntandoBundleJobEntity();
        job.setComponentId("my-component-id");

        List<DirectoryDescriptor> rootFolders = directoryProcessor
                .process(bundleReader).stream()
                .map(Installable::getRepresentation)
                .filter(DirectoryDescriptor::isRoot)
                .collect(Collectors.toList());

        assertThat(rootFolders).hasSize(1);
        assertThat(rootFolders.get(0).getName()).isEqualTo("/something");
    }


    @Test
    void whenCreatingReportableShouldOmitBundleCodeRootFolderIfSystemLevelBundle() throws IOException {

        List<String> expectedCodeList = Arrays
                .asList("/static/css/ootb", "/static/css/ootb/page-templates", "/static/css", "/ootb-widgets",
                        "/static", "/ootb-widgets/static/js", "/ootb-widgets/static", "/ootb-widgets/static/css");

        when(mockBundleReader.readBundleDescriptor()).thenReturn(BundleStubHelper.stubBundleDescriptor(null));
        when(mockBundleReader.getResourceFolders()).thenReturn(this.resourceFolder);

        Reportable reportable = directoryProcessor.getReportable(mockBundleReader, directoryProcessor);

        assertThat(reportable.getComponentType()).isEqualTo(ComponentType.DIRECTORY);
        assertThat(reportable.getCodes()).containsAll(expectedCodeList);
    }

    @Test
    void whenCreatingReportableShouldAddBundleCodeRootFolderIfStandardBundle() throws IOException {

        BundleDescriptor bundleDescriptor = BundleStubHelper.stubBundleDescriptor(null);
        bundleDescriptor.setBundleType(BundleType.STANDARD_BUNDLE);

        // prefix each expected file path with the bundle code
        List<String> expectedCodeList = Stream
                .of("/static/css/ootb", "/static/css/ootb/page-templates", "/static/css", "/ootb-widgets",
                        "/static", "/ootb-widgets/static/js", "/ootb-widgets/static", "/ootb-widgets/static/css")
                .map(s -> "/" + bundleDescriptor.getCode() + s)
                .collect(Collectors.toList());

        when(mockBundleReader.readBundleDescriptor()).thenReturn(bundleDescriptor);
        when(mockBundleReader.getBundleCode()).thenReturn(bundleDescriptor.getCode());
        when(mockBundleReader.getResourceFolders()).thenReturn(this.resourceFolder);

        Reportable reportable = directoryProcessor.getReportable(mockBundleReader, directoryProcessor);

        assertThat(reportable.getComponentType()).isEqualTo(ComponentType.DIRECTORY);
        assertThat(reportable.getCodes()).containsAll(expectedCodeList);
    }

}
