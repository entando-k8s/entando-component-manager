package org.entando.kubernetes.model.bundle.processor;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.entando.kubernetes.client.core.DefaultEntandoCoreClient;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.DirectoryDescriptor;
import org.entando.kubernetes.model.bundle.installable.DirectoryInstallable;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
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

}
