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

    public static final String ENTANDO_TEST_DOCKER_BUNDLE_ADDRESS =
            System.getenv("ENTANDO_TEST_DOCKER_BUNDLE_ADDRESS");
    public static final String ENTANDO_TEST_DOCKER_BUNDLE_VERSION =
            System.getenv("ENTANDO_TEST_DOCKER_BUNDLE_VERSION");

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
                .withVersion(ENTANDO_TEST_DOCKER_BUNDLE_VERSION)
                .withTarball(ENTANDO_TEST_DOCKER_BUNDLE_ADDRESS)
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

        String url = ENTANDO_TEST_DOCKER_BUNDLE_ADDRESS + ":" + ENTANDO_TEST_DOCKER_BUNDLE_VERSION;
        EntandoDeBundleTag tag = new EntandoDeBundleTagBuilder()
                .withVersion(ENTANDO_TEST_DOCKER_BUNDLE_VERSION)
                .withTarball(ENTANDO_TEST_DOCKER_BUNDLE_ADDRESS)
                .build();

        BundleDownloader dockerDownloader = factory.newDownloader(tag);
        assertThat(dockerDownloader).isInstanceOf(DockerBundleDownloader.class);

        List<String> tags = dockerDownloader.fetchRemoteTags(url);
        int sizeList = tags.size();
        assertThat(sizeList).isGreaterThan(10);

    }

    private boolean shouldRunDockerTest() {
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXx");
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXx");
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXx");
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXx");
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXx");
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXx");
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXx");
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXx");
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXx");
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXx");
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXx");
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXx");
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXx");
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXx");
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXx");
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXx");
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXx");
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXx");
        System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXx");

        if (ENTANDO_TEST_DOCKER_BUNDLE_ADDRESS == null || ENTANDO_TEST_DOCKER_BUNDLE_ADDRESS.length() == 0) {
            System.out.println("ENTANDO_TEST_DOCKER_BUNDLE_ADDRESS is not defined, skipping test");  // NOSONAR
            return true;
        }
        if (ENTANDO_TEST_DOCKER_BUNDLE_VERSION == null || ENTANDO_TEST_DOCKER_BUNDLE_VERSION.length() == 0) {
            System.out.println("ENTANDO_TEST_DOCKER_BUNDLE_VERSION is not defined, skipping test");  // NOSONAR
            return true;
        }
        return false;
    }
}

