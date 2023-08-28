package org.entando.kubernetes.model.bundle;

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
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderFactory;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderType;
import org.entando.kubernetes.model.bundle.downloader.DockerBundleDownloader;
import org.entando.kubernetes.model.bundle.downloader.DownloadedBundle;
import org.entando.kubernetes.model.bundle.downloader.GitBundleDownloader;
import org.entando.kubernetes.model.bundle.downloader.NpmBundleDownloader;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleBuilder;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTagBuilder;
import org.entando.kubernetes.service.digitalexchange.crane.CraneCommand;
import org.entando.kubernetes.utils.TenantContextJunitExt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@Tag("unit")
@ExtendWith(TenantContextJunitExt.class)
public class EntandoBundleDownloaderTest {

    public static final String BUNDLE_REMOTE_REPOSITORY = "https://github.com/Kerruba/entando-sample-bundle";
    private static final int port;
    private CraneCommand craneCommand = new CraneCommand();

    static {
        port = findFreePort().orElse(9080);
    }

    BundleDownloader downloader;
    private WireMockServer wireMockServer;

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
        DownloadedBundle downloadedBundle = downloader.saveBundleLocally(bundle, tag);
        Path expectedFile = downloadedBundle.getLocalBundlePath().resolve("descriptor.yaml");
        assertThat(expectedFile.toFile().exists()).isTrue();
        assertThat(downloadedBundle.getBundleDigest()).isEmpty();
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
        downloader = BundleDownloaderFactory.getForType("git", 300, 3, 600, null, craneCommand);
        DownloadedBundle downloadedBundle = downloader.saveBundleLocally(bundle, tag);
        Path unexpectedFile = downloadedBundle.getLocalBundlePath().resolve("package.json");
        assertThat(unexpectedFile.toFile().exists()).isFalse();
        assertThat(downloadedBundle.getBundleDigest()).isEmpty();
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
        downloader = BundleDownloaderFactory.getForType("npm", 300, 3, 600, null, craneCommand);
        DownloadedBundle downloadedBundle = downloader.saveBundleLocally(bundle, tag);
        assertThat(downloadedBundle.getLocalBundlePath().toFile()).exists();
        assertThat(downloadedBundle.getBundleDigest()).isEmpty();

        Path expectedFile = downloadedBundle.getLocalBundlePath().resolve("package.json");
        assertThat(expectedFile.toFile().exists()).isTrue();
        npmRegistry.stop();
    }

    @Test
    public void shouldGetInstanceBasedOnType() {
        downloader = BundleDownloaderFactory.getForType("NPM", 300, 3, 600, null, craneCommand);
        assertThat(downloader instanceof NpmBundleDownloader).isTrue();

        downloader = BundleDownloaderFactory.getForType("GIT", 300, 3, 600, null, craneCommand);
        assertThat(downloader instanceof GitBundleDownloader).isTrue();

        downloader = BundleDownloaderFactory.getForType("ANYTHING", 300, 3, 600, null, craneCommand);
        assertThat(downloader instanceof GitBundleDownloader).isTrue();

        downloader = BundleDownloaderFactory.getForType(null, 300, 3, 600, null, craneCommand);
        assertThat(downloader instanceof GitBundleDownloader).isTrue();
    }

    @Test
    public void factoryShouldCreateCorrectly() {
        BundleDownloaderFactory factory = new BundleDownloaderFactory();
        factory.setDefaultSupplier(GitBundleDownloader::new);

        factory.registerSupplier(BundleDownloaderType.NPM, NpmBundleDownloader::new);
        factory.registerSupplier(BundleDownloaderType.GIT, GitBundleDownloader::new);
        factory.registerSupplier(BundleDownloaderType.DOCKER, () -> new DockerBundleDownloader(300, 3, 600, null, craneCommand));

        downloader = factory.newDownloader();
        assertThat(downloader).isInstanceOf(GitBundleDownloader.class);

        downloader = factory.newDownloader(BundleDownloaderType.GIT);
        assertThat(downloader).isInstanceOf(GitBundleDownloader.class);

        downloader = factory.newDownloader(BundleDownloaderType.NPM);
        assertThat(downloader).isInstanceOf(NpmBundleDownloader.class);

        downloader = factory.newDownloader(BundleDownloaderType.DOCKER);
        assertThat(downloader).isInstanceOf(DockerBundleDownloader.class);

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
}

