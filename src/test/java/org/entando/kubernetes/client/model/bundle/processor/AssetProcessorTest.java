package org.entando.kubernetes.client.model.bundle.processor;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.entando.kubernetes.client.core.DefaultEntandoCoreClient;
import org.entando.kubernetes.model.bundle.BundleReader;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;
import org.entando.kubernetes.model.bundle.installable.AssetInstallable;
import org.entando.kubernetes.model.bundle.installable.DirectoryInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.processor.AssetProcessor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ClassPathResource;

@Tag("unit")
public class AssetProcessorTest {

    @Mock
    private DefaultEntandoCoreClient engineService;
    private BundleReader bundleReader;
    private AssetProcessor assetProcessor;

    @BeforeEach
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        Path bundleFolder = new ClassPathResource("bundle").getFile().toPath();
        bundleReader = new BundleReader(bundleFolder);
        assetProcessor = new AssetProcessor(engineService);
    }

    @Test
    public void testCreateFiles() throws IOException {
        final EntandoBundleJob job = new EntandoBundleJob();
        job.setComponentId("my-component-id");

        final List<? extends Installable> installables = assetProcessor
                .process(job, bundleReader);

        assertThat(installables).hasSize(5);

        assertThat(installables.get(0)).isInstanceOf(AssetInstallable.class);
        assertThat(installables.get(0).getComponentType()).isEqualTo(ComponentType.ASSET);
        assertThat(installables.get(0).getName()).isEqualTo("/something/css/custom.css");

        assertThat(installables.get(1)).isInstanceOf(AssetInstallable.class);
        assertThat(installables.get(1).getComponentType()).isEqualTo(ComponentType.ASSET);
        assertThat(installables.get(1).getName()).isEqualTo("/something/css/style.css");

        assertThat(installables.get(2)).isInstanceOf(AssetInstallable.class);
        assertThat(installables.get(2).getComponentType()).isEqualTo(ComponentType.ASSET);
        assertThat(installables.get(2).getName()).isEqualTo("/something/js/configUiScript.js");

        assertThat(installables.get(3)).isInstanceOf(AssetInstallable.class);
        assertThat(installables.get(3).getComponentType()).isEqualTo(ComponentType.ASSET);
        assertThat(installables.get(3).getName()).isEqualTo("/something/js/script.js");

        assertThat(installables.get(4)).isInstanceOf(AssetInstallable.class);
        assertThat(installables.get(4).getComponentType()).isEqualTo(ComponentType.ASSET);
        assertThat(installables.get(4).getName()).isEqualTo("/something/vendor/jquery/jquery.js");
    }

}
