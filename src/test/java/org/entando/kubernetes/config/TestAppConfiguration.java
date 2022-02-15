package org.entando.kubernetes.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.io.IOException;
import java.nio.file.Path;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderFactory;
import org.entando.kubernetes.model.bundle.downloader.GitBundleDownloader;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.stubhelper.BundleStubHelper;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;

@TestConfiguration
@Profile("test")
public class TestAppConfiguration extends AppConfiguration {

    public TestAppConfiguration(EntandoCoreClient entandoCoreClient,
            K8SServiceClient k8SServiceClient) {
        super(entandoCoreClient, k8SServiceClient);
    }

    @Bean
    @Override
    public BundleDownloaderFactory bundleDownloaderFactory() {
        BundleDownloaderFactory factory = new BundleDownloaderFactory();
        factory.setDefaultSupplier(() -> {
            Path bundleFolder;
            Path bundleFolderInvalidSecret;
            GitBundleDownloader git = Mockito.mock(GitBundleDownloader.class);
            try {
                bundleFolder = new ClassPathResource("bundle").getFile().toPath();
                bundleFolderInvalidSecret = new ClassPathResource("bundle-invalid-secret").getFile().toPath();
                lenient().when(git.saveBundleLocally(any(EntandoDeBundle.class), any(EntandoDeBundleTag.class)))
                                .thenAnswer(invocation -> {
                                    if (invocation.getArgument(0, EntandoDeBundle.class).getMetadata().getName().equals("todomvc")) {
                                        return bundleFolder;
                                    } else {
                                        return bundleFolderInvalidSecret;
                                    }
                                });
                lenient().when(git.saveBundleLocally(anyString())).thenReturn(bundleFolder);
                lenient().when(git.createTargetDirectory()).thenReturn(bundleFolder);
                lenient().when(git.fetchRemoteTags(anyString())).thenReturn(BundleStubHelper.TAG_LIST);
            } catch (IOException e) {
                throw new RuntimeException("Impossible to read the bundle folder from test resources");
            }
            return git;
        });
        return factory;
    }
}
