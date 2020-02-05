package org.entando.kubernetes.client.model.bundle.processor;

import org.entando.kubernetes.model.bundle.NpmPackageReader;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.bundle.processor.AssetProcessor;
import org.entando.kubernetes.service.digitalexchange.entandocore.EntandoCoreService;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.processor.AssetProcessor.AssetInstallable;
import org.entando.kubernetes.model.bundle.processor.AssetProcessor.DirectoryInstallable;
import org.entando.kubernetes.model.bundle.descriptor.ComponentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

public class AssetProcessorTest {

    @Mock private EntandoCoreService engineService;
    @Mock private NpmPackageReader npmPackageReader;

    private AssetProcessor assetProcessor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        assetProcessor = new AssetProcessor(engineService);
    }

    @Test
    public void testCreateFoldersAndFiles() throws IOException {
        final DigitalExchangeJob job = new DigitalExchangeJob();
        job.setComponentId("my-component-id");

        final List<String> folders = Arrays.asList("css", "js", "images");
        final List<String> files = Arrays.asList("favicon.ico", "css/styles.css", "js/script.js", "images/logo.png");

        when(npmPackageReader.containsResourceFolder()).thenReturn(true);
        when(npmPackageReader.getResourceFolders()).thenReturn(folders);
        when(npmPackageReader.getResourceFiles()).thenReturn(files);
        when(npmPackageReader.readFileAsDescriptor(eq("favicon.ico")))
                .thenReturn(file("", "favicon.ico", "base64icon"));
        when(npmPackageReader.readFileAsDescriptor(eq("css/styles.css")))
                .thenReturn(file("css", "styles.css", "base64css"));
        when(npmPackageReader.readFileAsDescriptor(eq("js/script.js")))
                .thenReturn(file("js", "script.js", "base64js"));
        when(npmPackageReader.readFileAsDescriptor(eq("images/logo.png")))
                .thenReturn(file("images", "logo.png", "base64img"));

        final List<? extends Installable> installables = assetProcessor.process(job, npmPackageReader, new ComponentDescriptor());

        assertThat(installables).hasSize(8);

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
        assertThat(installables.get(3).getName()).isEqualTo("/my-component-id/images");

        assertThat(installables.get(4)).isInstanceOf(AssetInstallable.class);
        assertThat(installables.get(4).getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(installables.get(4).getName()).isEqualTo("/my-component-id/favicon.ico");

        assertThat(installables.get(5)).isInstanceOf(AssetInstallable.class);
        assertThat(installables.get(5).getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(installables.get(5).getName()).isEqualTo("/my-component-id/css/styles.css");

        assertThat(installables.get(6)).isInstanceOf(AssetInstallable.class);
        assertThat(installables.get(6).getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(installables.get(6).getName()).isEqualTo("/my-component-id/js/script.js");

        assertThat(installables.get(7)).isInstanceOf(AssetInstallable.class);
        assertThat(installables.get(7).getComponentType()).isEqualTo(ComponentType.RESOURCE);
        assertThat(installables.get(7).getName()).isEqualTo("/my-component-id/images/logo.png");
    }

    private FileDescriptor file(final String folder, final String name, final String base64) {
        return new FileDescriptor(folder, name, base64);
    }

}
