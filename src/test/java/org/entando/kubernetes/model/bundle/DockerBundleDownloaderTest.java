package org.entando.kubernetes.model.bundle;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloader;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderFactory;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderType;
import org.entando.kubernetes.model.bundle.downloader.DockerBundleDownloader;
import org.entando.kubernetes.model.bundle.downloader.GitBundleDownloader;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleBuilder;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTagBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("in-process")
class DockerBundleDownloaderTest {

    @Test
    void shouldCreateBundleDownloaderFromTag() {
        BundleDownloaderFactory factory = new BundleDownloaderFactory();
        factory.setDefaultSupplier(GitBundleDownloader::new);
        factory.registerSupplier(BundleDownloaderType.DOCKER, () -> new DockerBundleDownloader(300, 3, 600));
        factory.registerSupplier(BundleDownloaderType.GIT, GitBundleDownloader::new);

        EntandoDeBundle bundle = new EntandoDeBundleBuilder()
                .withNewMetadata()
                .withName("my-name")
                .endMetadata()
                .build();

        EntandoDeBundleTag tag = new EntandoDeBundleTagBuilder()
                .withVersion("v0.0.1")
                .withTarball("docker://docker.io/gigiozzz/myapp2mysql-bundle:0.0.1")
                .build();

        BundleDownloader dockerDownloader = factory.newDownloader(tag);
        assertThat(dockerDownloader).isInstanceOf(DockerBundleDownloader.class);

        Path target = dockerDownloader.saveBundleLocally(bundle, tag);

        Path expectedFile = target.resolve("descriptor.yaml");
        assertThat(expectedFile.toFile()).exists();
    }

    @Test
    void shouldRetrieveTagsFromFullyQualifiedUrl() {
        BundleDownloaderFactory factory = new BundleDownloaderFactory();
        factory.setDefaultSupplier(GitBundleDownloader::new);
        factory.registerSupplier(BundleDownloaderType.DOCKER, () -> new DockerBundleDownloader(300, 3, 600));
        factory.registerSupplier(BundleDownloaderType.GIT, GitBundleDownloader::new);

        String url = "docker://docker.io/entando/entando-k8s-app-controller:7.0.2";
        EntandoDeBundleTag tag = new EntandoDeBundleTagBuilder()
                .withVersion("v7.0.2")
                .withTarball(url)
                .build();

        BundleDownloader dockerDownloader = factory.newDownloader(tag);
        assertThat(dockerDownloader).isInstanceOf(DockerBundleDownloader.class);

        List<String> tags = dockerDownloader.fetchRemoteTags(url);
        int sizeList = tags.size();
        assertThat(sizeList).isGreaterThan(10);

    }
}

