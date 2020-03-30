package org.entando.kubernetes.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import org.entando.kubernetes.model.bundle.BundleDownloader;
import org.entando.kubernetes.model.bundle.GitBundleDownloader;
import org.entando.kubernetes.model.bundle.NpmBundleDownloader;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;

@TestConfiguration
@Profile("test")
public class TestAppConfiguration {

    @Bean
    public BundleDownloader bundleDownloader() {
        Path bundleFolder = null;
        GitBundleDownloader git = Mockito.mock(GitBundleDownloader.class);
        try {
            bundleFolder = new ClassPathResource("bundle").getFile().toPath();
            when(git.saveBundleLocally(any(EntandoDeBundleTag.class))).thenReturn(bundleFolder);
            when(git.createTargetDirectory()).thenReturn(bundleFolder);
        } catch (IOException e) {
            throw new RuntimeException("Impossible to read the bundle folder from test resources");
        }
        return git;
    }

}
