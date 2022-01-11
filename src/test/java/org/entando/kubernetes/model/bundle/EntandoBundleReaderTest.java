package org.entando.kubernetes.model.bundle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.assertj.core.data.Index;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.CategoryDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentKey;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.Descriptor;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.GroupDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.LabelDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.LanguageDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.WidgetDescriptor.ConfigUIDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.plugin.EnvironmentVariable;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginPermission;
import org.entando.kubernetes.model.bundle.descriptor.plugin.SecretKeyRef;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.stubhelper.BundleStubHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

@Tag("unit")
public class EntandoBundleReaderTest {

    public static final String DEFAULT_TEST_BUNDLE_NAME = "bundle.tgz";
    public static final String ALTERNATIVE_STRUCTURE_BUNDLE_NAME = "generic_bundle.tgz";
    BundleReader bundleReader;
    Path bundleFolder;

    @BeforeEach
    public void readNpmPackage() throws IOException {
        bundleFolder = new ClassPathResource("bundle").getFile().toPath();
        bundleReader = new BundleReader(bundleFolder);
    }

    @Test
    void shouldThrowExceptionWhenNoEntandoBundleIsPassedToTheConstructor() {
        bundleReader = new BundleReader(bundleFolder);
        assertThrows(EntandoComponentManagerException.class, () -> bundleReader.getEntandoDeBundleId());
    }

    @Test
    void shouldReturnAValidBundleIdWhenEntandoBundleIsPassedToTheConstructor() {
        bundleReader = new BundleReader(bundleFolder, BundleStubHelper.stubEntandoDeBundle());
        assertThat(bundleReader.getEntandoDeBundleId()).isEqualTo(BundleStubHelper.BUNDLE_NAME);
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
    }

    @Test
    void shouldReadPageConfigurationFromBundle() throws IOException {
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
                .readListOfDescriptorFile("groups/groups.yaml", GroupDescriptor.class);
        assertThat(gd).hasSize(2);
        assertThat(gd.get(0).getCode()).isEqualTo("ecr");
        assertThat(gd.get(0).getName()).isEqualTo("Ecr");
        assertThat(gd.get(1).getCode()).isEqualTo("ps");
        assertThat(gd.get(1).getName()).isEqualTo("Professional Services");
    }

    @Test
    public void shouldReadCategoriesFromBundle() throws IOException {
        List<CategoryDescriptor> cd = bundleReader
                .readListOfDescriptorFile("categories/categories.yaml", CategoryDescriptor.class);
        assertThat(cd).hasSize(2);
        assertThat(cd.get(0).getCode()).isEqualTo("my-category");
        assertThat(cd.get(0).getParentCode()).isEqualTo("home");
        assertThat(cd.get(0).getTitles()).containsEntry("it", "La mia categoria");
        assertThat(cd.get(0).getTitles()).containsEntry("en", "My own category");
        assertThat(cd.get(1).getCode()).isEqualTo("another_category");
        assertThat(cd.get(1).getParentCode()).isEqualTo("my-category");
        assertThat(cd.get(1).getTitles()).containsEntry("it", "Altra categoria");
        assertThat(cd.get(1).getTitles()).containsEntry("en", "Another category");
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
        assertThat(ld).hasSize(2);
        assertThat(ld.get(0).getKey()).isEqualTo("HELLO");
        assertThat(ld.get(0).getTitles()).containsEntry("it", "Ciao");
        assertThat(ld.get(0).getTitles()).containsEntry("en", "Hello");
        assertThat(ld.get(1).getKey()).isEqualTo("WORLD");
        assertThat(ld.get(1).getTitles()).containsEntry("it", "Mundo");
        assertThat(ld.get(1).getTitles()).containsEntry("en", "World");
    }



    @Test
    void shouldReadPluginFromBundleV1() throws IOException {
        PluginDescriptor descriptor = bundleReader.readDescriptorFile("plugins/todomvcV1.yaml", PluginDescriptor.class);
        assertThat(descriptor.getSpec().getDbms()).isEqualTo("mysql");
        assertThat(descriptor.getSpec().getHealthCheckPath()).isEqualTo("/api/v1/todos");
        assertThat(descriptor.getSpec().getImage()).isEqualTo("entando/todomvcV1:1.0.0");
        assertThat(descriptor.getDockerImage().getName()).isEqualTo("todomvcV1");
        assertThat(descriptor.getDockerImage().getOrganization()).isEqualTo("entando");
        assertThat(descriptor.getDockerImage().getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void shouldReadPluginFromBundleV2() throws IOException {

        PluginDescriptor descriptor = bundleReader
                .readDescriptorFile("plugins/todomvcV2_complete.yaml", PluginDescriptor.class);
        assertThat(descriptor.getDbms()).isEqualTo("mysql");
        assertThat(descriptor.getHealthCheckPath()).isEqualTo("/api/v1/todos");
        assertThat(descriptor.getImage()).isEqualTo("entando/todomvcV2:1.0.0");
        assertThat(descriptor.getDockerImage().getName()).isEqualTo("todomvcV2");
        assertThat(descriptor.getDockerImage().getOrganization()).isEqualTo("entando");
        assertThat(descriptor.getDockerImage().getVersion()).isEqualTo("1.0.0");
        assertThat(descriptor.getIngressPath()).isEqualTo("/myhostname.io/entando-plugin");
        assertThat(descriptor.getSecurityLevel()).isEqualTo("lenient");

        assertThat(descriptor.getPermissions()).containsExactly(
                new PluginPermission("realm-management", "manage-users"),
                new PluginPermission("realm-management", "view-users"));
    }

    @Test
    void shouldReadPluginFromBundleV3() throws IOException {

        PluginDescriptor descriptor = bundleReader
                .readDescriptorFile("plugins/todomvcV3_complete.yaml", PluginDescriptor.class);
        this.assertOnPluginDescriptorV3Properties(descriptor);
    }

    @Test
    void shouldReadPluginFromBundleV4() throws IOException {

        PluginDescriptor descriptor = bundleReader
                .readDescriptorFile("plugins/todomvcV4_complete.yaml", PluginDescriptor.class);
        this.assertOnPluginDescriptorV3Properties(descriptor);

        final List<EnvironmentVariable> environmentVariables = descriptor.getEnvironmentVariables();
        assertThat(environmentVariables).hasSize(2);

        final EnvironmentVariable envVar1 = environmentVariables.get(0);
        final EnvironmentVariable expected1 = new EnvironmentVariable()
                .setName("env1Name").setValue("env1value");
        assertThat(envVar1).isEqualTo(expected1);

        final EnvironmentVariable envVar2 = environmentVariables.get(1);
        final EnvironmentVariable expected2 = new EnvironmentVariable()
                .setName("env2Name")
                .setSecretKeyRef(
                        new SecretKeyRef("env-2-configmap-secretkey-ref-name-custombasename-todomvc", "env2ConfigMapSecretKeyRefKey"));
        assertThat(envVar2).isEqualTo(expected2);
    }

    /**
     * applies assertions shared across all plugin descriptor v3+.
     * @param descriptor the plugin descriptor to validate
     */
    private void assertOnPluginDescriptorV3Properties(PluginDescriptor descriptor) {

        assertThat(descriptor.getDbms()).isEqualTo("mysql");
        assertThat(descriptor.getHealthCheckPath()).isEqualTo("/api/v1/todos");
        assertThat(descriptor.getImage()).isEqualTo("entando/todomvcV3:1.0.0");
        assertThat(descriptor.getDockerImage().getName()).isEqualTo("todomvcV3");
        assertThat(descriptor.getDockerImage().getOrganization()).isEqualTo("entando");
        assertThat(descriptor.getDockerImage().getVersion()).isEqualTo("1.0.0");
        assertThat(descriptor.getIngressPath()).isEqualTo("/myhostname.io/entando-plugin");
        assertThat(descriptor.getSecurityLevel()).isEqualTo("lenient");

        assertThat(descriptor.getPermissions()).containsExactly(
                new PluginPermission("realm-management", "manage-users"),
                new PluginPermission("realm-management", "view-users"));
    }


    @Test
    void shouldBeTolerantWithUnknowFields() throws IOException {

        // using a descriptor with only base fields
        // should parse the descriptor without errors
        PluginDescriptor descriptor = bundleReader.readDescriptorFile("plugins/todomvcV2.yaml", PluginDescriptor.class);
        assertThat(descriptor.getDbms()).isEqualTo("mysql");
        assertThat(descriptor.getHealthCheckPath()).isEqualTo("/api/v1/todos");
        assertThat(descriptor.getImage()).isEqualTo("entando/todomvcV2:1.0.0");
        assertThat(descriptor.getDockerImage().getName()).isEqualTo("todomvcV2");
        assertThat(descriptor.getDockerImage().getOrganization()).isEqualTo("entando");
        assertThat(descriptor.getDockerImage().getVersion()).isEqualTo("1.0.0");
        assertThat(descriptor.getIngressPath()).isNullOrEmpty();
        assertThat(descriptor.getPermissions()).isNullOrEmpty();
    }


    @Test
    public void shouldReadRelatedFileAsString() throws IOException {
        String content = bundleReader.readFileAsString("widgets/widget.ftl");
        assertThat(content).isEqualTo("<h2>Hello World Widget</h2>");
    }

    @Test
    public void shouldThrowAnExceptionWhenDescriptorNotFound() throws IOException {
        assertThrows(InvalidBundleException.class, () -> {
            bundleReader.getResourceFileAsDescriptor("widgets/pinco-pallo.yaml");
        });
    }

    @Test
    public void shouldThrowAnExceptionWhenFileNotFound() throws IOException {
        assertThrows(InvalidBundleException.class, () -> {
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
        public Class<DumbDescriptor> getDescriptorClass() {
            return DumbDescriptor.class;
        }

        @Override
        public Optional<Function<ComponentSpecDescriptor, List<String>>> getComponentSelectionFn() {
            return Optional.empty();
        }

        @Override
        public List<Installable<DumbDescriptor>> process(BundleReader bundleReader) {
            return this.process(bundleReader, InstallAction.CREATE, new InstallPlan());
        }

        @Override
        public List<Installable<DumbDescriptor>> process(BundleReader bundleReader, InstallAction conflictStrategy,
                InstallPlan installPlan) {
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


        @Override
        public ComponentKey getComponentKey() {
            return new ComponentKey("dummy");
        }
    }

}
