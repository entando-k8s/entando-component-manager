package org.entando.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;
import org.entando.kubernetes.model.bundle.NpmPackageReader;
import org.entando.kubernetes.model.bundle.descriptor.ComponentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJobComponent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

public class NpmPackageReaderTest {

    NpmPackageReader r;

    @Before
    public void readNpmPackage() throws IOException {
       r = new NpmPackageReader(getBundlePath()) ;
    }

    @After
    public void cleanUp() {
        r.getTarEntries().values()
                .forEach(File::delete);
    }


    @Test
    public void shouldReadNpmPackageCorrectly() {
        assertThat(r.getTarEntries().isEmpty()).isFalse();
    }

    @Test
    public void shouldNotReportPackageInEntryName() {
        assertThat(r.getTarEntries().keySet().stream().filter(p -> p.startsWith(NpmPackageReader.PACKAGE_ROOT))).isEmpty();
    }

    @Test
    public void shouldReadResourcesFromPackage() {
        List<String> expectedResourceFolders = Arrays.asList(
                "js", "img", "css", "less", "fonts", "static",
                "static/js", "fonts/Lora", "static/css", "fonts/Roboto_Mono",
                "fonts/Titillium_Web"
        );
        assertThat(r.containsResourceFolder()).isTrue();
        assertThat(r.getResourceFolders()).hasSize(expectedResourceFolders.size());
        assertThat(r.getResourceFolders()).containsAll(expectedResourceFolders);
    }

    @Test
    public void shouldReadDescriptorFile() throws IOException {
        WidgetDescriptor wd = r.readDescriptorFile("widgets/survey-admin.yaml", WidgetDescriptor.class);
        assertThat(wd).isNotNull();
        assertThat(wd.getCode()).isEqualTo("inail_survey_admin");
        assertThat(wd.getGroup()).isEqualTo("free");
        assertThat(wd.getCustomUiPath()).isEqualTo("survey-admin.ftl");
        assertThat(wd.getTitles()).contains(
                entry("en", "Inail Survey Administration"),
                entry("it", "Inail Survey Administration"));
    }

    @Test
    public void shouldReadRelatedFileAsString() throws IOException {
        String content = r.readFileAsString("widgets/survey-admin.ftl");
        assertThat(content) .startsWith("<#assign wp=JspTaglibs[\"/aps-core\"]>");
        assertThat(content).endsWith("<#else>    You have to be logged in to fill the survey</#if>");
    }



    @Test
    public void componentProcessorShouldReturnRelativeFolder() {
        ComponentProcessor cp = new DumbComponentProcessor();
        String newPath = cp.getRelativePath("widgets/survey-admin.yaml", "survey-admin.ftl");
        String expected = "widgets/survey-admin.ftl";
        assertThat(newPath).isEqualTo(expected);
    }

    private Path getBundlePath() throws IOException {
        return new ClassPathResource("inail_bundle-0.0.1.tgz").getFile().toPath();
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
        public List<Installable> process(DigitalExchangeJob job, NpmPackageReader npmPackageReader,
                ComponentDescriptor descriptor) throws IOException {
            return null;
        }

        @Override
        public boolean shouldProcess(ComponentType componentType) {
            return false;
        }

        @Override
        public void uninstall(DigitalExchangeJobComponent component) {

        }
    }

}
