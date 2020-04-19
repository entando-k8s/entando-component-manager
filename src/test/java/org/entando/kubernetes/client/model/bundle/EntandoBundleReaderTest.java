package org.entando.kubernetes.client.model.bundle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.assertj.core.data.Index;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.model.bundle.BundleReader;
import org.entando.kubernetes.model.bundle.descriptor.ComponentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.WidgetDescriptor.ConfigUIDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleJob;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleComponentJob;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

@Tag("unit")
public class EntandoBundleReaderTest {

    BundleReader r;
    public static final String DEFAULT_TEST_BUNDLE_NAME = "bundle.tgz";
    public static final String ALTERNATIVE_STRUCTURE_BUNDLE_NAME = "generic_bundle.tgz";
    Path bundleFolder;

    @BeforeEach
    public void readNpmPackage() throws IOException {
       bundleFolder = new ClassPathResource("bundle").getFile().toPath();
       r = new BundleReader(bundleFolder) ;
    }


    @Test
    public void shouldRebaseBundleEntriesToDescriptorRoot() throws IOException {
        ComponentDescriptor cd = r.readDescriptorFile("descriptor.yaml", ComponentDescriptor.class);
        assertThat(cd).isNotNull();
    }

    @Test
    public void shouldReadResourceFoldersFromPackage() {
        List<String> expectedResourceFolders = Arrays.asList(
                "resources/js", "resources/css", "resources/vendor", "resources/vendor/jquery"
        );
        assertThat(r.containsResourceFolder()).isTrue();
        assertThat(r.getResourceFolders()).hasSize(expectedResourceFolders.size());
        assertThat(r.getResourceFolders()).containsAll(expectedResourceFolders);
    }

    @Test
    public void shouldReadResourceFilesFromPackage() {
        List<String> expectedResourceFiles = Arrays.asList(
                "resources/css/custom.css", "resources/css/style.css", "resources/js/configUiScript.js",
                "resources/js/script.js", "resources/vendor/jquery/jquery.js"
        );
        assertThat(r.getResourceFiles()).hasSize(expectedResourceFiles.size());
        assertThat(r.getResourceFiles()).containsAll(expectedResourceFiles);
    }

    @Test
    public void shouldReadDescriptorFile() throws IOException {
        WidgetDescriptor wd = r.readDescriptorFile("widgets/another_widget_descriptor.yaml", WidgetDescriptor.class);
        assertThat(wd).isNotNull();
        assertThat(wd.getCode()).isEqualTo("another_todomvc_widget");
        assertThat(wd.getGroup()).isEqualTo("free");
        assertThat(wd.getCustomUiPath()).isEqualTo("widget.ftl");
        assertThat(wd.getTitles()).contains(
                entry("en", "TODO MVC Widget"),
                entry("it", "TODO MVC Widget"));
    }

    @Test
    public void shouldReadRelatedFileAsString() throws IOException {
        String content = r.readFileAsString("widgets/widget.ftl");
        assertThat(content).isEqualTo("<h2>Hello World Widget</h2>");
    }

    @Test
    public void shouldThrowAnExceptionWhenDescriptorNotFound() throws IOException {
        Assertions.assertThrows(InvalidBundleException.class, () -> {
            r.getResourceFileAsDescriptor("widgets/pinco-pallo.yaml");
        });
    }

    @Test
    public void shouldThrowAnExceptionWhenFileNotFound() throws IOException {
        Assertions.assertThrows(InvalidBundleException.class, () -> {
            r.getResourceFileAsDescriptor("widgets/pinco-pallo-template.ftl");
        });
    }


    @Test
    public void componentProcessorShouldReturnRelativeFolder() {
        ComponentProcessor cp = new DumbComponentProcessor();
        String newPath = cp.getRelativePath("widgets/survey-admin.yaml", "survey-admin.ftl");
        String expected = "widgets/survey-admin.ftl";
        assertThat(newPath).isEqualTo(expected);
    }

    @Test
    public void shouldReadConfigUIForWidget() throws IOException {
        WidgetDescriptor wd = r.readDescriptorFile("widgets/widget_with_config_ui.yaml", WidgetDescriptor.class);
        assertThat(wd).isNotNull();
        assertThat(wd.getConfigUi()).isInstanceOf(ConfigUIDescriptor.class);
        assertThat(wd.getConfigUi().getCustomElement()).isEqualTo("my-config");
        assertThat(wd.getConfigUi().getResources()).hasSize(1);
        assertThat(wd.getConfigUi().getResources()).contains("js/configUiScript.js", Index.atIndex(0));

    }

    @Test
    public void readResourceFileDescriptor() throws IOException {
        FileDescriptor fd = r.getResourceFileAsDescriptor("resources/css/custom.css");
        assertThat(fd.getFilename()).isEqualTo("custom.css");
        assertThat(fd.getFolder()).isEqualTo("resources/css/");
    }

    private Path getTestDefaultBundlePath() throws IOException {
        return getBundlePath(DEFAULT_TEST_BUNDLE_NAME);
    }

    private Path getBundlePath(String bundleName) throws IOException {
        return new ClassPathResource(bundleName).getFile().toPath();
    }

    private TarArchiveInputStream getGzipTarInputStream() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("inail_bundle-0.0.1.tgz");
        InputStream gzis = new GzipCompressorInputStream(is);
        return new TarArchiveInputStream(gzis);
    }

    private TarArchiveEntry nextTarArchiveEntry(TarArchiveInputStream is) throws UncheckedIOException {
        try {
            return is.getNextTarEntry();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static class DumbComponentProcessor implements ComponentProcessor {

        @Override
        public List<Installable> process(EntandoBundleJob job, BundleReader bundleReader,
                ComponentDescriptor descriptor) throws IOException {
            return null;
        }

        @Override
        public boolean shouldProcess(ComponentType componentType) {
            return false;
        }

        @Override
        public void uninstall(EntandoBundleComponentJob component) {

        }
    }

}
