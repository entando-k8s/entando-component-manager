package org.entando.kubernetes.model.bundle.downloader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloader.BundleDownloaderException;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleBuilder;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTagBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("in-process")
class DockerBundleDownloaderTest {

    private static final String ENTANDO_TEST_DOCKER_BUNDLE_ADDRESS_VAR_NAME = "ENTANDO_TEST_DOCKER_BUNDLE_ADDRESS";
    public static final String ENTANDO_TEST_DOCKER_BUNDLE_ADDRESS =
            System.getenv(ENTANDO_TEST_DOCKER_BUNDLE_ADDRESS_VAR_NAME);
    private static final String ENTANDO_TEST_DOCKER_BUNDLE_TAG_VAR_NAME = "ENTANDO_TEST_DOCKER_BUNDLE_TAG";
    public static final String ENTANDO_TEST_DOCKER_BUNDLE_TAG =
            System.getenv(ENTANDO_TEST_DOCKER_BUNDLE_TAG_VAR_NAME);
    private static final String WRONG_CREDENTIALS =
            "{\"auths\": {\"dockerXXX.io\": {\"username\": \"wronguser\",\"password\": \"wrongpassword\"}}}";
    private static final String UNUSED_CREDENTIALS =
            "{\"auths\": {\"myrepo\": {\"username\": \"unuseduser\",\"password\": \"unusedpassword\"}}}";
    private static final BundleDownloaderFactory factory = new BundleDownloaderFactory();

    @BeforeAll
    static void setup() {
        factory.setDefaultSupplier(GitBundleDownloader::new);
        factory.registerSupplier(BundleDownloaderType.GIT, GitBundleDownloader::new);

    }


    @Test
    void shouldCreateBundleDownloaderFromTag() {
        if (shouldRunDockerTest()) {
            return;
        }

        factory.registerSupplier(BundleDownloaderType.DOCKER,
                () -> new DockerBundleDownloader(300, 3, 600, UNUSED_CREDENTIALS));
        EntandoDeBundle bundle = new EntandoDeBundleBuilder()
                .withNewMetadata()
                .withName("my-name")
                .endMetadata()
                .build();

        EntandoDeBundleTag tag = new EntandoDeBundleTagBuilder()
                .withVersion(ENTANDO_TEST_DOCKER_BUNDLE_TAG)
                .withTarball(ENTANDO_TEST_DOCKER_BUNDLE_ADDRESS)
                .build();

        BundleDownloader dockerDownloader = factory.newDownloader(tag);
        assertThat(dockerDownloader).isInstanceOf(DockerBundleDownloader.class);

        Path target1 = dockerDownloader.saveBundleLocally(bundle, tag);
        Path expectedFile1 = target1.resolve("descriptor.yaml");
        assertThat(expectedFile1.toFile()).exists();

        Path target2 = dockerDownloader.saveBundleLocally(
                ENTANDO_TEST_DOCKER_BUNDLE_ADDRESS + ":" + ENTANDO_TEST_DOCKER_BUNDLE_TAG);
        Path expectedFile2 = target2.resolve("descriptor.yaml");
        assertThat(expectedFile2.toFile()).exists();

    }

    @Test
    void shouldRetrieveTagsFromFullyQualifiedUrl() {
        if (shouldRunDockerTest()) {
            return;
        }

        factory.registerSupplier(BundleDownloaderType.DOCKER,
                () -> new DockerBundleDownloader(300, 3, 600, UNUSED_CREDENTIALS));
        String url = ENTANDO_TEST_DOCKER_BUNDLE_ADDRESS + ":" + ENTANDO_TEST_DOCKER_BUNDLE_TAG;
        EntandoDeBundleTag tag = new EntandoDeBundleTagBuilder()
                .withVersion(ENTANDO_TEST_DOCKER_BUNDLE_TAG)
                .withTarball(ENTANDO_TEST_DOCKER_BUNDLE_ADDRESS)
                .build();

        BundleDownloader dockerDownloader = factory.newDownloader(tag);
        assertThat(dockerDownloader).isInstanceOf(DockerBundleDownloader.class);

        List<String> tags = dockerDownloader.fetchRemoteTags(url);
        int sizeList = tags.size();
        assertThat(sizeList).isGreaterThan(10);

    }

    @Test
    void retrieveTags_shouldReturnError_FromWrongUrl() {
        if (shouldRunDockerTest()) {
            return;
        }

        factory.registerSupplier(BundleDownloaderType.DOCKER, () -> new DockerBundleDownloader(300, 3, 600, null));
        String url = ENTANDO_TEST_DOCKER_BUNDLE_ADDRESS + "WRONG_TAG" + ":" + ENTANDO_TEST_DOCKER_BUNDLE_TAG;
        EntandoDeBundleTag tag = new EntandoDeBundleTagBuilder()
                .withVersion(ENTANDO_TEST_DOCKER_BUNDLE_TAG)
                .withTarball(ENTANDO_TEST_DOCKER_BUNDLE_ADDRESS)
                .build();

        BundleDownloader dockerDownloader = factory.newDownloader(tag);
        assertThat(dockerDownloader).isInstanceOf(DockerBundleDownloader.class);

        assertThrows(BundleDownloaderException.class, () -> dockerDownloader.fetchRemoteTags(url));

    }

    @Test
    void downloadBundleWithPassword_shouldReturnError_FromWrongPassword() {
        if (shouldRunDockerTest()) {
            return;
        }

        factory.registerSupplier(BundleDownloaderType.DOCKER,
                () -> new DockerBundleDownloader(300, 3, 600, WRONG_CREDENTIALS));
        EntandoDeBundle bundle = new EntandoDeBundleBuilder()
                .withNewMetadata()
                .withName("my-name")
                .endMetadata()
                .build();

        EntandoDeBundleTag tag = new EntandoDeBundleTagBuilder()
                .withVersion("1.0.0")
                .withTarball("docker://dockerXXX.io/entando/testtest")
                .build();

        BundleDownloader dockerDownloader = factory.newDownloader(tag);
        assertThat(dockerDownloader).isInstanceOf(DockerBundleDownloader.class);

        assertThrows(BundleDownloaderException.class, () -> dockerDownloader.saveBundleLocally(bundle, tag));

    }

    @Test
    void downloadBundleWithPassword_shouldReturnError_FromWrongJsonCredentials() {
        if (shouldRunDockerTest()) {
            return;
        }

        factory.registerSupplier(BundleDownloaderType.DOCKER,
                () -> new DockerBundleDownloader(300, 3, 600, "{as , ]}"));
        EntandoDeBundle bundle = new EntandoDeBundleBuilder()
                .withNewMetadata()
                .withName("my-name")
                .endMetadata()
                .build();

        EntandoDeBundleTag tag = new EntandoDeBundleTagBuilder()
                .withVersion("1.0.0")
                .withTarball("docker://dockerXXX.io/entando/testtest")
                .build();

        BundleDownloader dockerDownloader = factory.newDownloader(tag);
        assertThat(dockerDownloader).isInstanceOf(DockerBundleDownloader.class);

        assertThrows(BundleDownloaderException.class, () -> dockerDownloader.saveBundleLocally(bundle, tag));

    }

    private boolean shouldRunDockerTest() {
        if (ENTANDO_TEST_DOCKER_BUNDLE_ADDRESS == null || ENTANDO_TEST_DOCKER_BUNDLE_ADDRESS.length() == 0) {
            System.out.println(
                    ENTANDO_TEST_DOCKER_BUNDLE_ADDRESS_VAR_NAME + " is not defined, skipping test");  // NOSONAR
            return true;
        }
        if (ENTANDO_TEST_DOCKER_BUNDLE_TAG == null || ENTANDO_TEST_DOCKER_BUNDLE_TAG.length() == 0) {
            System.out.println(ENTANDO_TEST_DOCKER_BUNDLE_TAG_VAR_NAME + " is not defined, skipping test");  // NOSONAR
            return true;
        }
        return false;
    }
}

