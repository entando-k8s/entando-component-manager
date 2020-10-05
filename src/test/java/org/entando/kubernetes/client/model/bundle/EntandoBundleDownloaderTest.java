package org.entando.kubernetes.client.model.bundle;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.Optional;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloader;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloader.BundleDownloaderException;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloader.Type;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderFactory;
import org.entando.kubernetes.model.bundle.downloader.GitBundleDownloader;
import org.entando.kubernetes.model.bundle.downloader.NpmBundleDownloader;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleBuilder;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTagBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class EntandoBundleDownloaderTest {

    private WireMockServer wireMockServer;
    private static int port;


    static {
        port = findFreePort().orElse(9080);
    }

    public static final String BUNDLE_REMOTE_REPOSITORY = "https://github.com/Kerruba/entando-sample-bundle";
    BundleDownloader downloader;

    @AfterEach
    public void tearDown() throws IOException {
        if (downloader != null) {
            downloader.cleanTargetDirectory();
        }
    }

    @Test
    public void shouldCloneGitBundle() {
        EntandoDeBundle bundle = new EntandoDeBundleBuilder()
                .withNewMetadata()
                .withName("my-name")
                .endMetadata()
                .build();
        EntandoDeBundleTag tag = new EntandoDeBundleTagBuilder()
                .withVersion("v0.0.1")
                .withTarball(BUNDLE_REMOTE_REPOSITORY)
                .build();
        downloader = new GitBundleDownloader();
        Path target = downloader.saveBundleLocally(bundle, tag);
        Path expectedFile = target.resolve("descriptor.yaml");
        assertThat(expectedFile.toFile().exists()).isTrue();
    }

    @Test
    public void shouldCloneGitBundleWithAnotherVersion() {
        EntandoDeBundleTag tag = new EntandoDeBundleTagBuilder()
                .withVersion("v0.0.2")
                .withTarball(BUNDLE_REMOTE_REPOSITORY)
                .build();

        EntandoDeBundle bundle = new EntandoDeBundleBuilder()
                .withNewMetadata()
                .withName("my-name")
                .endMetadata()
                .build();
        downloader = BundleDownloader.getForType("git");
        Path target = downloader.saveBundleLocally(bundle, tag);
        Path unexpectedFile = target.resolve("package.json");
        assertThat(unexpectedFile.toFile().exists()).isFalse();
    }

    @Test
    public void shouldReadTarZipFile() throws IOException {
        EntandoDeBundleTag tag = new EntandoDeBundleTagBuilder()
                .withVersion("v0.0.1")
                .withTarball("http://localhost:" + port + "/my-package.tar.gz")
                .build();

        EntandoDeBundle bundle = new EntandoDeBundleBuilder()
                .withNewMetadata()
                .withName("my-name")
                .endMetadata()
                .build();
        WireMockServer npmRegistry = getMockedNpmRegistry();
        npmRegistry.start();
        downloader = BundleDownloader.getForType("npm");
        Path target = downloader.saveBundleLocally(bundle, tag);
        assertThat(target.toFile().exists()).isTrue();

        Path expectedFile = target.resolve("package.json");
        assertThat(expectedFile.toFile().exists()).isTrue();
        npmRegistry.stop();
    }

    @Test
    public void shouldGetInstanceBasedOnType() {
        downloader = BundleDownloader.getForType("NPM");
        assertThat(downloader instanceof NpmBundleDownloader).isTrue();

        downloader = BundleDownloader.getForType("GIT");
        assertThat(downloader instanceof GitBundleDownloader).isTrue();

        downloader = BundleDownloader.getForType("ANYTHING");
        assertThat(downloader instanceof GitBundleDownloader).isTrue();

        downloader = BundleDownloader.getForType(null);
        assertThat(downloader instanceof GitBundleDownloader).isTrue();
    }

    @Test
    public void factoryShouldCreateCorrectly() {
        BundleDownloaderFactory factory = new BundleDownloaderFactory();
        factory.setDefaultSupplier(GitBundleDownloader::new);

        factory.registerSupplier(Type.NPM, NpmBundleDownloader::new);
        factory.registerSupplier(Type.GIT, GitBundleDownloader::new);

        downloader = factory.newDownloader();
        assertThat(downloader).isInstanceOf(GitBundleDownloader.class);

        downloader = factory.newDownloader(Type.GIT);
        assertThat(downloader).isInstanceOf(GitBundleDownloader.class);

        downloader = factory.newDownloader(Type.NPM);
        assertThat(downloader).isInstanceOf(NpmBundleDownloader.class);
    }

    private WireMockServer getMockedNpmRegistry() throws IOException {
        InputStream zippedBundle = this.getClass().getResourceAsStream("/npm-downloaded-bundle.tgz");
        byte[] zbBytes = new byte[zippedBundle.available()];
        zippedBundle.read(zbBytes);
        wireMockServer = new WireMockServer(port);
        wireMockServer.stubFor(
                get(urlMatching("/my-package.tar.gz"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withBody(zbBytes)));
        return wireMockServer;
    }


    private static Optional<Integer> findFreePort() {
        Integer port = null;
        try {
            // Get a free port
            ServerSocket s = new ServerSocket(0);
            port = s.getLocalPort();
            s.close();

        } catch (IOException e) {
            // No OPS
        }
        return Optional.ofNullable(port);
    }
}

