package org.entando.kubernetes.service.digitalexchange;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.entando.kubernetes.config.TestAppConfiguration;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.EntandoValidationException;
import org.entando.kubernetes.model.bundle.BundleInfo;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderFactory;
import org.entando.kubernetes.model.bundle.downloader.GitBundleDownloader;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleDetails;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.stubhelper.BundleInfoStubHelper;
import org.entando.kubernetes.stubhelper.BundleStubHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class EntandoDeBundleComposerTest {

    private final URL bundleUrl = new URL(BundleInfoStubHelper.GIT_REPO_ADDRESS);
    private final List<EntandoDeBundleTag> entandoDeBundleTags = BundleStubHelper.TAG_LIST.stream()
            .map(tag -> new EntandoDeBundleTag(tag, null, null, bundleUrl.toString()))
            .collect(Collectors.toList());
    private final BundleInfo bundleInfo = BundleInfoStubHelper.stubBunbleInfo();
    private final BundleDownloaderFactory bundleDownloaderFactory = new TestAppConfiguration(null,
            null).bundleDownloaderFactory();
    private EntandoDeBundleComposer deBundleComposer;

    EntandoDeBundleComposerTest() throws MalformedURLException {
    }

    @BeforeEach
    public void setup() {
        deBundleComposer = new EntandoDeBundleComposer(bundleDownloaderFactory);
    }

    @Test
    void shouldReturnTheExpectedBundleDescriptorWhileComposingIt() {

        Stream.of(
                        BundleInfoStubHelper.GIT_REPO_ADDRESS,
                        "git@www.github.com/entando/mybundle.git",
                        "git://www.github.com/entando/mybundle.git",
                        "ssh://www.github.com/entando/mybundle.git")
                .forEach(bundleUrl -> {

                    BundleInfo bundleInfo = BundleInfoStubHelper.stubBunbleInfo().setGitRepoAddress(bundleUrl);

                    final EntandoDeBundle deBundle = deBundleComposer.composeEntandoDeBundle(bundleInfo);

                    assertThat(deBundle.getMetadata().getName()).isEqualTo("mybundle.entando.www.github.com");
                    assertOnFullLabelsDeBundleMap(deBundle.getMetadata().getLabels());

                    final EntandoDeBundleDetails details = deBundle.getSpec().getDetails();
                    assertThat(details.getName()).isEqualTo("something");
                    assertThat(details.getDescription()).isEqualTo("bundle description");

                    assertThat(details.getThumbnail()).isEqualTo(BundleInfoStubHelper.DESCR_IMAGE);

                    List<EntandoDeBundleTag> entandoDeBundleTags = BundleStubHelper.TAG_LIST.stream()
                            .map(tag -> new EntandoDeBundleTag(tag, null, null, bundleUrl))
                            .collect(Collectors.toList());

                    assertOnVersionsAndTags(deBundle, entandoDeBundleTags);
                });
    }

    @Test
    void shouldThrowExceptionWhenComposingABundleAndReceivingANullOrInvalidUrl() {

        bundleInfo.setGitRepoAddress(null);
        assertThrows(EntandoValidationException.class, () -> deBundleComposer.composeEntandoDeBundle(bundleInfo));

        bundleInfo.setGitRepoAddress("http://.com");
        assertThrows(EntandoValidationException.class, () -> deBundleComposer.composeEntandoDeBundle(bundleInfo));
    }

    @Test
    void shouldThrowExceptionWhenComposingABundleAndReceivingANullBundleInfo() {

        assertThrows(EntandoComponentManagerException.class, () -> deBundleComposer.composeEntandoDeBundle(null));
    }

    @Test
    void shouldIgnoreBundleTagsNotCompliantWithSemver() {

        List<String> tagList = new ArrayList<>();
        tagList.add("not_working_version");
        tagList.add("strange{}tag");
        tagList.addAll(BundleStubHelper.TAG_LIST);

        bundleDownloaderFactory.setDefaultSupplier(() -> {
            Path bundleFolder;
            GitBundleDownloader git = Mockito.mock(GitBundleDownloader.class);
            try {
                bundleFolder = new ClassPathResource("bundle").getFile().toPath();
                when(git.saveBundleLocally(anyString())).thenReturn(bundleFolder);
                when(git.fetchRemoteTags(anyString())).thenReturn(tagList);
            } catch (IOException e) {
                throw new RuntimeException("Impossible to read the bundle folder from test resources");
            }
            return git;
        });

        final EntandoDeBundle deBundle = deBundleComposer.composeEntandoDeBundle(bundleInfo);
        assertOnVersionsAndTags(deBundle, entandoDeBundleTags);
    }

    @Test
    void shouldReplaceGitAndSshProtocolWithHttp() {
        Map<String, String> testCasesMap = Map.of(
                "git@github.com/entando/my_bundle.git", "http://github.com/entando/my_bundle.git",
                "git://github.com/entando/my_bundle.git", "http://github.com/entando/my_bundle.git",
                "ssh://github.com/entando/my_bundle.git", "http://github.com/entando/my_bundle.git");

        testCasesMap.forEach((key, value) -> {
            String actual = deBundleComposer.gitSshProtocolToHttp(key);
            Assertions.assertThat(actual).isEqualTo(value);
        });
    }

    @Test
    void shouldReturnTheSameStringWhenProtocolIsNotOneOfTheExpected() {
        List<String> testCasesList = List.of(
                "got@github.com/entando/my_bundle.git",
                "got://github.com/entando/my_bundle.git",
                "sssh://github.com/entando/my_bundle.git",
                "https://github.com/entando/my_bundle.git",
                "ftp://github.com/entando/my_bundle.git");

        testCasesList.forEach(url -> {
            String actual = deBundleComposer.gitSshProtocolToHttp(url);
            Assertions.assertThat(actual).isEqualTo(url);
        });
    }


    private void assertOnFullLabelsDeBundleMap(Map<String, String> labelsMap) {
        assertThat(labelsMap.get("plugin")).isEqualTo("true");
        assertThat(labelsMap.get("widget")).isEqualTo("true");
        assertThat(labelsMap.get("fragment")).isEqualTo("true");
        assertThat(labelsMap.get("category")).isEqualTo("true");
        assertThat(labelsMap.get("page")).isEqualTo("true");
        assertThat(labelsMap.get("pageTemplate")).isEqualTo("true");
        assertThat(labelsMap.get("contentType")).isEqualTo("true");
        assertThat(labelsMap.get("contentTemplate")).isEqualTo("true");
        assertThat(labelsMap.get("content")).isEqualTo("true");
        assertThat(labelsMap.get("asset")).isEqualTo("true");
        assertThat(labelsMap.get("group")).isEqualTo("true");
        assertThat(labelsMap.get("label")).isEqualTo("true");
        assertThat(labelsMap.get("language")).isEqualTo("true");
        assertThat(labelsMap.get("bundle-type")).isEqualTo("standard-bundle");
    }

    private void assertOnVersionsAndTags(EntandoDeBundle deBundle, List<EntandoDeBundleTag> entandoDeBundleTags) {

        final EntandoDeBundleDetails details = deBundle.getSpec().getDetails();

        assertThat(details.getDistTags()).hasSize(1);
        assertThat(details.getDistTags().get(BundleUtilities.LATEST_VERSION)).isEqualTo(BundleStubHelper.V1_2_0);
        assertThat(details.getVersions()).containsExactlyElementsOf(BundleStubHelper.TAG_LIST);

        final List<EntandoDeBundleTag> deBundleTags = deBundle.getSpec().getTags();
        assertThat(deBundleTags).hasSize(3);
        IntStream.range(0, entandoDeBundleTags.size())
                .forEach(i -> assertThat(deBundleTags.get(i)).isEqualToComparingFieldByField(
                        entandoDeBundleTags.get(i)));
    }
}
