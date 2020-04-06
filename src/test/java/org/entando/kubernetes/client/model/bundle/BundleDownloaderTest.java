package org.entando.kubernetes.client.model.bundle;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import org.apache.commons.lang3.SerializationUtils;
import org.entando.kubernetes.model.bundle.downloader.GitBundleDownloader;
import org.entando.kubernetes.model.bundle.downloader.NpmBundleDownloader;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleBuilder;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTagBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class BundleDownloaderTest {

    private WireMockServer wireMockServer;
    private static int port;


    static {
        port = findFreePort().orElse(9080);
    }

    public static final String BUNDLE_REMOTE_REPOSITORY = "https://github.com/Kerruba/entando-sample-bundle";
    Path target;

    @AfterEach
    public void tearDown() throws IOException {
        Files.walk(target)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
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
        target = new GitBundleDownloader().saveBundleLocally(bundle, tag);
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
        target = new GitBundleDownloader().saveBundleLocally(bundle, tag);
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
        InputStream zippedBundle = this.getClass().getResourceAsStream("/bundle.tgz");
        byte[] zbBytes = new byte[zippedBundle.available()];
        zippedBundle.read(zbBytes);
        wireMockServer = new WireMockServer(port);
        wireMockServer.start();
        wireMockServer.stubFor(
                get(urlMatching("/my-package.tar.gz"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withBody(zbBytes)));
        target = new NpmBundleDownloader().saveBundleLocally(bundle, tag);
        assertThat(target.toFile().exists());

        Path expectedFile = target.resolve("package.json");
        assertThat(expectedFile.toFile().exists());
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

