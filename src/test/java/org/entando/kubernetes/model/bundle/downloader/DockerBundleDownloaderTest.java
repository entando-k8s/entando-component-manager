package org.entando.kubernetes.model.bundle.downloader;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleBuilder;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTagBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("in-process")
class DockerBundleDownloaderTest {

    private static final String ENTANDO_OPT_DOCKER_BUNDLE_ADDRESS_VAR_NAME = "ENTANDO_OPT_DOCKER_BUNDLE_ADDRESS";
    public static final String ENTANDO_OPT_DOCKER_BUNDLE_ADDRESS =
            System.getenv(ENTANDO_OPT_DOCKER_BUNDLE_ADDRESS_VAR_NAME);
    private static final String ENTANDO_OPT_DOCKER_BUNDLE_TAG_VAR_NAME = "ENTANDO_OPT_DOCKER_BUNDLE_TAG";
    public static final String ENTANDO_OPT_DOCKER_BUNDLE_TAG =
            System.getenv(ENTANDO_OPT_DOCKER_BUNDLE_TAG_VAR_NAME);

    @Test
    void shouldCreateBundleDownloaderFromTag() {
        if (shouldRunDockerTest()) {
            return;
        }

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
                .withVersion(ENTANDO_OPT_DOCKER_BUNDLE_TAG)
                .withTarball(ENTANDO_OPT_DOCKER_BUNDLE_ADDRESS)
                .build();

        BundleDownloader dockerDownloader = factory.newDownloader(tag);
        assertThat(dockerDownloader).isInstanceOf(DockerBundleDownloader.class);

        Path target = dockerDownloader.saveBundleLocally(bundle, tag);

        Path expectedFile = target.resolve("descriptor.yaml");
        assertThat(expectedFile.toFile()).exists();
    }

    @Test
    void shouldRetrieveTagsFromFullyQualifiedUrl() {
        if (shouldRunDockerTest()) {
            return;
        }

        BundleDownloaderFactory factory = new BundleDownloaderFactory();
        factory.setDefaultSupplier(GitBundleDownloader::new);
        factory.registerSupplier(BundleDownloaderType.DOCKER, () -> new DockerBundleDownloader(300, 3, 600));
        factory.registerSupplier(BundleDownloaderType.GIT, GitBundleDownloader::new);

        String url = ENTANDO_OPT_DOCKER_BUNDLE_ADDRESS + ":" + ENTANDO_OPT_DOCKER_BUNDLE_TAG;
        EntandoDeBundleTag tag = new EntandoDeBundleTagBuilder()
                .withVersion(ENTANDO_OPT_DOCKER_BUNDLE_TAG)
                .withTarball(ENTANDO_OPT_DOCKER_BUNDLE_ADDRESS)
                .build();

        BundleDownloader dockerDownloader = factory.newDownloader(tag);
        assertThat(dockerDownloader).isInstanceOf(DockerBundleDownloader.class);

        List<String> tags = dockerDownloader.fetchRemoteTags(url);
        int sizeList = tags.size();
        assertThat(sizeList).isGreaterThan(10);

    }

    private boolean shouldRunDockerTest() {
        if (ENTANDO_OPT_DOCKER_BUNDLE_ADDRESS == null || ENTANDO_OPT_DOCKER_BUNDLE_ADDRESS.length() == 0) {
            System.out.println(
                    ENTANDO_OPT_DOCKER_BUNDLE_ADDRESS_VAR_NAME + " is not defined, skipping test");  // NOSONAR
            return true;
        }
        if (ENTANDO_OPT_DOCKER_BUNDLE_TAG == null || ENTANDO_OPT_DOCKER_BUNDLE_TAG.length() == 0) {
            System.out.println(ENTANDO_OPT_DOCKER_BUNDLE_TAG_VAR_NAME + " is not defined, skipping test");  // NOSONAR
            return true;
        }
        return false;
    }
}

