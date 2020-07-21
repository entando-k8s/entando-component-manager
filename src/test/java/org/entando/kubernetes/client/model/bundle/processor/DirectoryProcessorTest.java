package org.entando.kubernetes.client.model.bundle.processor;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.entando.kubernetes.client.core.DefaultEntandoCoreClient;
import org.entando.kubernetes.model.bundle.BundleReader;
import org.entando.kubernetes.model.bundle.descriptor.DirectoryDescriptor;
import org.entando.kubernetes.model.bundle.installable.DirectoryInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.processor.DirectoryProcessor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
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
    private BundleReader bundleReader;
    private DirectoryProcessor directoryProcessor;

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

        assertThat(installables).hasSize(5);

        assertThat(installables.get(0)).isInstanceOf(DirectoryInstallable.class);
        assertThat(installables.get(0).getComponentType()).isEqualTo(ComponentType.DIRECTORY);
        assertThat(installables.get(0).getName()).isEqualTo("/something");

        assertThat(installables.get(1)).isInstanceOf(DirectoryInstallable.class);
        assertThat(installables.get(1).getComponentType()).isEqualTo(ComponentType.DIRECTORY);
        assertThat(installables.get(1).getName()).isEqualTo("/something/css");

        assertThat(installables.get(2)).isInstanceOf(DirectoryInstallable.class);
        assertThat(installables.get(2).getComponentType()).isEqualTo(ComponentType.DIRECTORY);
        assertThat(installables.get(2).getName()).isEqualTo("/something/js");

        assertThat(installables.get(3)).isInstanceOf(DirectoryInstallable.class);
        assertThat(installables.get(3).getComponentType()).isEqualTo(ComponentType.DIRECTORY);
        assertThat(installables.get(3).getName()).isEqualTo("/something/vendor");

        assertThat(installables.get(4)).isInstanceOf(DirectoryInstallable.class);
        assertThat(installables.get(4).getComponentType()).isEqualTo(ComponentType.DIRECTORY);
        assertThat(installables.get(4).getName()).isEqualTo("/something/vendor/jquery");
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

}
