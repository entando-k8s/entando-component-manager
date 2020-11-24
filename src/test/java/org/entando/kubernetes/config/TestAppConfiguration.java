package org.entando.kubernetes.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderFactory;
import org.entando.kubernetes.model.bundle.downloader.GitBundleDownloader;
import org.entando.kubernetes.model.bundle.reportable.AnalysisReportFunction;
import org.entando.kubernetes.model.bundle.reportable.Reportable;
import org.entando.kubernetes.model.bundle.reportable.ReportableRemoteHandler;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.stubhelper.AnalysisReportStubHelper;
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
        });
        return factory;
    }
}
