package org.entando.kubernetes.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloader;
import org.entando.kubernetes.model.bundle.downloader.GitBundleDownloader;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;

@TestConfiguration
@Profile("test")
public class TestAppConfiguration {

    @Autowired
    ApplicationContext appContext;

    @Bean
    public BundleDownloader bundleDownloader() {
        Path bundleFolder = null;
        GitBundleDownloader git = Mockito.mock(GitBundleDownloader.class);
        try {
            bundleFolder = new ClassPathResource("bundle").getFile().toPath();
            when(git.saveBundleLocally(any(EntandoDeBundle.class), any(EntandoDeBundleTag.class)))
                    .thenReturn(bundleFolder);
            when(git.createTargetDirectory()).thenReturn(bundleFolder);
        } catch (IOException e) {
            throw new RuntimeException("Impossible to read the bundle folder from test resources");
        }
        return git;
    }

    @Bean
    public Map<ComponentType, ComponentProcessor> processorMap() {
        Map<ComponentType, ComponentProcessor> processorMap = new HashMap<>();
        for(ComponentProcessor pr:appContext.getBeansOfType(ComponentProcessor.class).values()) {
            processorMap.put(pr.getSupportedComponentType(), pr);
        }
        return processorMap;
    }
}
