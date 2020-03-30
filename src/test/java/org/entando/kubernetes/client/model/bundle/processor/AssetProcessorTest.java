package org.entando.kubernetes.client.model.bundle.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.entando.kubernetes.model.bundle.BundleReader;
import org.entando.kubernetes.model.bundle.descriptor.ComponentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.processor.AssetProcessor;
import org.entando.kubernetes.model.bundle.processor.AssetProcessor.AssetInstallable;
import org.entando.kubernetes.model.bundle.processor.AssetProcessor.DirectoryInstallable;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.service.digitalexchange.entandocore.EntandoCoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ClassPathResource;

@Tag("unit")
public class AssetProcessorTest {

    @Mock private EntandoCoreService engineService;
    private BundleReader bundleReader;
    private AssetProcessor assetProcessor;

    @BeforeEach
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        Path bundleFolder = new ClassPathResource("bundle").getFile().toPath();
        bundleReader =new BundleReader(bundleFolder) ;
        assetProcessor = new AssetProcessor(engineService);
    }

    @Test
    public void testCreateFoldersAndFiles() throws IOException {
        final DigitalExchangeJob job = new DigitalExchangeJob();
        job.setComponentId("my-component-id");

//        final List<String> folders = Arrays.asList("css", "js", "images");
//        final List<String> files = Arrays.asList("favicon.ico", "css/styles.css", "js/script.js", "images/logo.png");

//        when(bundleReader.getBundleId()).thenReturn("my-bundle");
//        when(bundleReader.containsResourceFolder()).thenReturn(true);
//        when(bundleReader.getResourceFolders()).thenReturn(folders);
//        when(bundleReader.getResourceFiles()).thenReturn(files);
//        when(bundleReader.getResourceFileAsDescriptor(eq("favicon.ico")))
//                .thenReturn(file("", "favicon.ico", "base64icon"));
//        when(bundleReader.getResourceFileAsDescriptor(eq("css/styles.css")))
//                .thenReturn(file("css", "styles.css", "base64css"));
//        when(bundleReader.getResourceFileAsDescriptor(eq("js/script.js")))
//                .thenReturn(file("js", "script.js", "base64js"));
//        when(bundleReader.getResourceFileAsDescriptor(eq("images/logo.png")))
//                .thenReturn(file("images", "logo.png", "base64img"));

        final List<? extends Installable> installables = assetProcessor.process(job, bundleReader, new ComponentDescriptor());

        assertThat(installables).hasSize(10);

        assertThat(installables.get(0)).isInstanceOf(DirectoryInstallable.class);
        assertThat(installables.get(0).getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(installables.get(0).getName()).isEqualTo("/my-component-id");

        assertThat(installables.get(1)).isInstanceOf(DirectoryInstallable.class);
        assertThat(installables.get(1).getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(installables.get(1).getName()).isEqualTo("/my-component-id/css");

        assertThat(installables.get(2)).isInstanceOf(DirectoryInstallable.class);
        assertThat(installables.get(2).getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(installables.get(2).getName()).isEqualTo("/my-component-id/js");

        assertThat(installables.get(3)).isInstanceOf(DirectoryInstallable.class);
        assertThat(installables.get(3).getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(installables.get(3).getName()).isEqualTo("/my-component-id/vendor");

        assertThat(installables.get(4)).isInstanceOf(DirectoryInstallable.class);
        assertThat(installables.get(4).getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(installables.get(4).getName()).isEqualTo("/my-component-id/vendor/jquery");

        assertThat(installables.get(5)).isInstanceOf(AssetInstallable.class);
        assertThat(installables.get(5).getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(installables.get(5).getName()).isEqualTo("/my-component-id/css/custom.css");

        assertThat(installables.get(6)).isInstanceOf(AssetInstallable.class);
        assertThat(installables.get(6).getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(installables.get(6).getName()).isEqualTo("/my-component-id/css/style.css");

        assertThat(installables.get(7)).isInstanceOf(AssetInstallable.class);
        assertThat(installables.get(7).getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(installables.get(7).getName()).isEqualTo("/my-component-id/js/configUiScript.js");

        assertThat(installables.get(8)).isInstanceOf(AssetInstallable.class);
        assertThat(installables.get(8).getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(installables.get(8).getName()).isEqualTo("/my-component-id/js/script.js");

        assertThat(installables.get(9)).isInstanceOf(AssetInstallable.class);
        assertThat(installables.get(9).getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(installables.get(9).getName()).isEqualTo("/my-component-id/vendor/jquery/jquery.js");
    }

    private FileDescriptor file(final String folder, final String name, final String base64) {
        return new FileDescriptor(folder, name, base64);
    }

}
