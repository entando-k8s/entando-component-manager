package org.entando.kubernetes.model.bundle;

import static org.assertj.core.api.Assertions.assertThat;

import org.entando.kubernetes.model.bundle.downloader.BundleDownloader;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderFactory;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderType;
import org.entando.kubernetes.model.bundle.downloader.DockerBundleDownloader;
import org.entando.kubernetes.model.bundle.downloader.GitBundleDownloader;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTagBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;


@Tag("unit")
class BundleDownloaderFactoryTest {

    @Test
    void shouldCreateBundleDownloaderFromTag() {
        BundleDownloaderFactory factory = new BundleDownloaderFactory();
        factory.setDefaultSupplier(GitBundleDownloader::new);
        factory.registerSupplier(BundleDownloaderType.DOCKER, () -> new DockerBundleDownloader(300, 3, 600, null));
        factory.registerSupplier(BundleDownloaderType.GIT, GitBundleDownloader::new);

        EntandoDeBundleTag tagGit = new EntandoDeBundleTagBuilder()
                .withVersion("v0.0.1")
                .withTarball("https://github.com/firegloves-bundles/myapp-mysql2.git")
                .build();

        BundleDownloader gitDownloader = factory.newDownloader(tagGit);
        assertThat(gitDownloader).isInstanceOf(GitBundleDownloader.class);

        EntandoDeBundleTag tagDocker = new EntandoDeBundleTagBuilder()
                .withVersion("v0.0.1")
                .withTarball("docker://golang:1.16-bullseye")
                .build();

        BundleDownloader dockerDownloader = factory.newDownloader(tagDocker);
        assertThat(dockerDownloader).isInstanceOf(DockerBundleDownloader.class);

        EntandoDeBundleTag tagNo = new EntandoDeBundleTagBuilder()
                .withVersion("v0.0.1")
                .withTarball("blablabla")
                .build();

        BundleDownloader tagNoDownloader = factory.newDownloader(tagNo);
        assertThat(tagNoDownloader).isInstanceOf(GitBundleDownloader.class);

    }

}

