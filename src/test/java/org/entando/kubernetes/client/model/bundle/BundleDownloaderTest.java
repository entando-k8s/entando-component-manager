package org.entando.kubernetes.client.model.bundle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.entando.kubernetes.model.bundle.GitBundleDownloader;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTagBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class BundleDownloaderTest {

    Path tempDest;

    @BeforeEach
    public void setup() throws IOException {
         tempDest = Files.createTempDirectory(null);
    }

    @AfterEach
    public void tearDown() throws IOException {
        Files.walk(tempDest)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    public void shouldCloneGitBundle() {
       EntandoDeBundleTag tag = new EntandoDeBundleTagBuilder()
               .withVersion("v0.0.1")
               .withTarball("https://github.com/Kerruba/entando-sample-bundle")
               .build();

       new GitBundleDownloader().saveBundleLocally(tag, tempDest);
       Path expectedFile = tempDest.resolve("descriptor.yaml");
       assertThat(expectedFile.toFile().exists()).isTrue();
    }

    @Test
    public void shouldCloneGitBundleWithAnotherVersion() {
        EntandoDeBundleTag tag = new EntandoDeBundleTagBuilder()
                .withVersion("v0.0.2")
                .withTarball("https://github.com/Kerruba/entando-sample-bundle")
                .build();

        new GitBundleDownloader().saveBundleLocally(tag, tempDest);
        Path unexpectedFile = tempDest.resolve("package.json");
        assertThat(unexpectedFile.toFile().exists()).isFalse();
    }
}
