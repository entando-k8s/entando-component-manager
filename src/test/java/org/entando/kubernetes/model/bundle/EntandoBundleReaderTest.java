package org.entando.kubernetes.model.bundle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.assertj.core.data.Index;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.CategoryDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.Descriptor;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.GroupDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.LabelDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.LanguageDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.WidgetDescriptor.ConfigUIDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

@Tag("unit")
public class EntandoBundleReaderTest {

    BundleReader bundleReader;
    public static final String DEFAULT_TEST_BUNDLE_NAME = "bundle.tgz";
    public static final String ALTERNATIVE_STRUCTURE_BUNDLE_NAME = "generic_bundle.tgz";
    Path bundleFolder;

    @BeforeEach
    public void readNpmPackage() throws IOException {
        bundleFolder = new ClassPathResource("bundle").getFile().toPath();
        bundleReader = new BundleReader(bundleFolder);
    }

    @Test
    public void shouldReadBundleIdCorrectly() throws IOException {
        assertThat(bundleReader.getBundleCode()).isEqualTo("something");
    }

    @Test
    public void shouldRebaseBundleEntriesToDescriptorRoot() throws IOException {
        BundleDescriptor cd = bundleReader.readDescriptorFile("descriptor.yaml", BundleDescriptor.class);
        assertThat(cd).isNotNull();
    }

    @Test
    public void shouldReadResourceFoldersFromPackage() {
        List<String> expectedResourceFolders = Arrays.asList(
                "resources/js", "resources/css", "resources/vendor", "resources/vendor/jquery"
        );
        assertThat(bundleReader.containsResourceFolder()).isTrue();
        assertThat(bundleReader.getResourceFolders()).hasSize(expectedResourceFolders.size());
        assertThat(bundleReader.getResourceFolders()).containsAll(expectedResourceFolders);
    }

    @Test
    public void shouldReadResourceFilesFromPackage() {
        List<String> expectedResourceFiles = Arrays.asList(
                "resources/css/custom.css", "resources/css/style.css", "resources/js/configUiScript.js",
                "resources/js/script.js", "resources/vendor/jquery/jquery.js"
        );
        assertThat(bundleReader.getResourceFiles()).hasSize(expectedResourceFiles.size());
        assertThat(bundleReader.getResourceFiles()).containsAll(expectedResourceFiles);
    }

    @Test
    public void shouldReadDescriptorFile() throws IOException {
        WidgetDescriptor wd = bundleReader
                .readDescriptorFile("widgets/another_widget_descriptor.yaml", WidgetDescriptor.class);
        assertThat(wd).isNotNull();
        assertThat(wd.getCode()).isEqualTo("another_todomvc_widget");
        assertThat(wd.getGroup()).isEqualTo("free");
        assertThat(wd.getCustomUiPath()).isEqualTo("widget.ftl");
        assertThat(wd.getTitles()).contains(
                entry("en", "TODO MVC Widget"),
                entry("it", "TODO MVC Widget"));
    }

    @Test
    public void shouldReadPagesFromBundle() throws IOException {
        PageDescriptor pd = bundleReader
                .readDescriptorFile("pages/my_page_descriptor.yaml", PageDescriptor.class);
        assertThat(pd).isNotNull();
        assertThat(pd.getCode()).isEqualTo("my-page");
        assertThat(pd.getParentCode()).isEqualTo("homepage");
        assertThat(pd.getTitles()).containsEntry("it", "La mia pagina");
        assertThat(pd.getTitles()).containsEntry("en", "My page");
        assertThat(pd.getPageModel()).isEqualTo("service");
        assertThat(pd.getOwnerGroup()).isEqualTo("administrators");
        assertThat(pd.getJoinGroups()).hasSize(3);
        assertThat(pd.getJoinGroups().get(0)).isEqualTo("free");
        assertThat(pd.getJoinGroups().get(1)).isEqualTo("customers");
        assertThat(pd.getJoinGroups().get(2)).isEqualTo("developers");
        assertThat(pd.isSeo()).isFalse();
        assertThat(pd.isDisplayedInMenu()).isTrue();
        assertThat(pd.getStatus()).isEqualTo("published");
        assertThat(pd.getWidgets()).hasSize(1);
        assertThat(pd.getWidgets().get(0).getCode()).isEqualTo("my-code");
    }

    @Test
    public void shouldReadGroupsFromBundle() throws IOException {
        List<GroupDescriptor> gd = bundleReader
                .readListOfDescriptorFile("groups/my-group.yaml", GroupDescriptor.class);
        assertThat(gd).hasSize(1);
        assertThat(gd.get(0).getCode()).isEqualTo("ecr");
        assertThat(gd.get(0).getName()).isEqualTo("Ecr");
    }

    @Test
    public void shouldReadCategoriesFromBundle() throws IOException {
        List<CategoryDescriptor> cd = bundleReader
                .readListOfDescriptorFile("categories/my-category.yaml", CategoryDescriptor.class);
        assertThat(cd).hasSize(1);
        assertThat(cd.get(0).getCode()).isEqualTo("my-category");
        assertThat(cd.get(0).getParentCode()).isEqualTo("home");
        assertThat(cd.get(0).getTitles()).containsEntry("it", "La mia categoria");
        assertThat(cd.get(0).getTitles()).containsEntry("en", "My own category");
    }

    @Test
    public void shouldReadLanguagesFromDedicatedFile() throws IOException {
        List<LanguageDescriptor> ld = bundleReader
                .readListOfDescriptorFile("languages/languages.yaml", LanguageDescriptor.class)
                .stream().sorted(Comparator.comparing(langDescriptor -> langDescriptor.getCode().toLowerCase()))
                .collect(Collectors.toList());

        assertThat(ld).hasSize(2);
        assertThat(ld.get(0).getCode()).isEqualTo("en");
        assertThat(ld.get(0).getDescription()).isEqualTo("English");
        assertThat(ld.get(1).getCode()).isEqualTo("it");
        assertThat(ld.get(1).getDescription()).isEqualTo("Italiano");
    }

    @Test
    public void shouldReadLabelsFromDedicatedFile() throws IOException {
        List<LabelDescriptor> ld = bundleReader
                .readListOfDescriptorFile("labels/labels.yaml", LabelDescriptor.class);
        assertThat(ld).hasSize(1);
        assertThat(ld.get(0).getKey()).isEqualTo("HELLO");
        assertThat(ld.get(0).getTitles()).containsEntry("it", "Mio Titolo");
        assertThat(ld.get(0).getTitles()).containsEntry("en", "My Title");

    }

    @Test
    public void shouldReadRelatedFileAsString() throws IOException {
        String content = bundleReader.readFileAsString("widgets/widget.ftl");
        assertThat(content).isEqualTo("<h2>Hello World Widget</h2>");
    }

    @Test
    public void shouldThrowAnExceptionWhenDescriptorNotFound() throws IOException {
        Assertions.assertThrows(InvalidBundleException.class, () -> {
            bundleReader.getResourceFileAsDescriptor("widgets/pinco-pallo.yaml");
        });
    }

    @Test
    public void shouldThrowAnExceptionWhenFileNotFound() throws IOException {
        Assertions.assertThrows(InvalidBundleException.class, () -> {
            bundleReader.getResourceFileAsDescriptor("widgets/pinco-pallo-template.ftl");
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
        WidgetDescriptor wd = bundleReader
                .readDescriptorFile("widgets/widget_with_config_ui.yaml", WidgetDescriptor.class);
        assertThat(wd).isNotNull();
        assertThat(wd.getConfigUi()).isInstanceOf(ConfigUIDescriptor.class);
        assertThat(wd.getConfigUi().getCustomElement()).isEqualTo("my-config");
        assertThat(wd.getConfigUi().getResources()).hasSize(1);
        assertThat(wd.getConfigUi().getResources()).contains("something/js/configUiScript.js", Index.atIndex(0));

    }

    @Test
    public void readResourceFileDescriptor() throws IOException {
        FileDescriptor fd = bundleReader.getResourceFileAsDescriptor("resources/css/custom.css");
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

    private static class DumbComponentProcessor implements ComponentProcessor<DumbDescriptor> {

        @Override
        public List<Installable<DumbDescriptor>> process(BundleReader bundleReader) {
            return null;
        }

        @Override
        public List<Installable<DumbDescriptor>> process(List<EntandoBundleComponentJobEntity> components) {
            return null;
        }

        @Override
        public DumbDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component) {
            return null;
        }

        @Override
        public ComponentType getSupportedComponentType() {
            return null;
        }
    }

    private static class DumbDescriptor implements Descriptor {

    }

}
