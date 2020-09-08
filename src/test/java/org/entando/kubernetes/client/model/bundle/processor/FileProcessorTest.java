package org.entando.kubernetes.client.model.bundle.processor;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.entando.kubernetes.client.core.DefaultEntandoCoreClient;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;
import org.entando.kubernetes.model.bundle.installable.FileInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.processor.FileProcessor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ClassPathResource;

@Tag("unit")
public class FileProcessorTest {

    @Mock
    private DefaultEntandoCoreClient engineService;
    private BundleReader bundleReader;
    private FileProcessor fileProcessor;

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
        assertThat(installables.get(0).getComponentType()).isEqualTo(ComponentType.ASSET);
        assertThat(installables.get(0).getName()).isEqualTo("/something/css/custom.css");

        assertThat(installables.get(1)).isInstanceOf(FileInstallable.class);
        assertThat(installables.get(1).getComponentType()).isEqualTo(ComponentType.ASSET);
        assertThat(installables.get(1).getName()).isEqualTo("/something/css/style.css");

        assertThat(installables.get(2)).isInstanceOf(FileInstallable.class);
        assertThat(installables.get(2).getComponentType()).isEqualTo(ComponentType.ASSET);
        assertThat(installables.get(2).getName()).isEqualTo("/something/js/configUiScript.js");

        assertThat(installables.get(3)).isInstanceOf(FileInstallable.class);
        assertThat(installables.get(3).getComponentType()).isEqualTo(ComponentType.ASSET);
        assertThat(installables.get(3).getName()).isEqualTo("/something/js/script.js");

        assertThat(installables.get(4)).isInstanceOf(FileInstallable.class);
        assertThat(installables.get(4).getComponentType()).isEqualTo(ComponentType.ASSET);
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

}
