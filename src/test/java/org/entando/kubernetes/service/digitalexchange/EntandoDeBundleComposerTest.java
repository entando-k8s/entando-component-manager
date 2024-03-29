package org.entando.kubernetes.service.digitalexchange;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.entando.kubernetes.client.K8SServiceClientTestDouble;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.config.TestAppConfiguration;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.EntandoValidationException;
import org.entando.kubernetes.model.bundle.BundleInfo;
import org.entando.kubernetes.model.bundle.BundleType;
import org.entando.kubernetes.model.bundle.downloader.BundleDownloaderFactory;
import org.entando.kubernetes.model.bundle.downloader.GitBundleDownloader;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleDetails;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpecBuilder;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;
import org.entando.kubernetes.service.digitalexchange.crane.CraneCommand;
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

    private final String pbcAnnotationsPrefix = "entando.org/pbc";
    private final URL bundleUrl = new URL(BundleInfoStubHelper.GIT_REPO_ADDRESS);
    private final List<EntandoDeBundleTag> entandoDeBundleTags = BundleStubHelper.TAG_LIST.stream()
            .map(tag -> new EntandoDeBundleTag(tag, null, null, bundleUrl.toString()))
            .collect(Collectors.toList());
    private final BundleInfo bundleInfo = BundleInfoStubHelper.stubBunbleInfo();
    private final BundleDownloaderFactory bundleDownloaderFactory = new TestAppConfiguration(null,
            null).bundleDownloaderFactory(new CraneCommand());
    private EntandoDeBundleComposer deBundleComposer;
    private ObjectMapper objectMapper = new ObjectMapper();
    private K8SServiceClientTestDouble k8SServiceClient;
    private BundleTagFilterManager bundleTagFilterManager;

    EntandoDeBundleComposerTest() throws MalformedURLException {
    }

    @BeforeEach
    public void setup() {
        k8SServiceClient = new K8SServiceClientTestDouble();
        bundleTagFilterManager = mock(BundleTagFilterManager.class);
        deBundleComposer = new EntandoDeBundleComposer(bundleDownloaderFactory, k8SServiceClient, bundleTagFilterManager);
    }

    @Test
    void shouldReturnTheExpectedBundleDescriptorWhileComposingIt() {

        when(bundleTagFilterManager.filterTagsByEnvironment(any())).thenAnswer(i -> ((Collection<?>)i.getArguments()[0]).stream());

        Stream.of(
                        BundleInfoStubHelper.GIT_REPO_ADDRESS,
                        "git@www.github.com/entando/mybundle.git",
                        "git://www.github.com/entando/mybundle.git",
                        "ssh://www.github.com/entando/mybundle.git")
                .forEach(bundleUrl -> {

                    BundleInfo bundleInfo = BundleInfoStubHelper.stubBunbleInfo().setGitRepoAddress(bundleUrl);

                    final EntandoDeBundle deBundle = deBundleComposer.composeEntandoDeBundle(bundleInfo);

                    assertOnComposedEntandoDeBundle(deBundle, bundleUrl);
                    assertOnPbcAnnotations(deBundle);
                });
    }

    @Test
    void shouldBeAbleToComposeEntandoDeBundleEvenWithoutBundleGroups() {
        BundleInfo bundleInfo = BundleInfoStubHelper.stubBunbleInfo().setBundleGroups(null);
        when(bundleTagFilterManager.filterTagsByEnvironment(any())).thenAnswer(i -> ((Collection<?>)i.getArguments()[0]).stream());

        final EntandoDeBundle deBundle = deBundleComposer.composeEntandoDeBundle(bundleInfo);

        assertOnComposedEntandoDeBundle(deBundle, bundleInfo.getGitRepoAddress());

        final List<String> pbcAnnotations = extractPbcAnnotationsFrom(deBundle);
        assertThat(pbcAnnotations).hasSize(0);
    }

    @Test
    void shouldBeAbleToComposeEntandoDeBundleEvenHavingOnlyBundleGroupsName() {

        when(bundleTagFilterManager.filterTagsByEnvironment(any())).thenAnswer(i -> ((Collection<?>)i.getArguments()[0]).stream());

        // given a bundle info with null bundle groups id
        final BundleInfo bundleInfo = BundleInfoStubHelper.stubBunbleInfo();
        bundleInfo.getBundleGroups().forEach(bg -> bg.setId(null));

        // when the debundle gets composed
        final EntandoDeBundle deBundle = deBundleComposer.composeEntandoDeBundle(bundleInfo);

        // then the result is the expected one
        assertOnComposedEntandoDeBundle(deBundle, bundleInfo.getGitRepoAddress());

        // and the bundle groups are correctly populated
        assertOnPbcAnnotations(deBundle);
    }

    @Test
    void shouldBeAbleToComposeEntandoDeBundleEvenHavingOnlyBundleGroupsId() {

        when(bundleTagFilterManager.filterTagsByEnvironment(any())).thenAnswer(i -> ((Collection<?>)i.getArguments()[0]).stream());

        // given a bundle info with null bundle groups name
        final BundleInfo bundleInfo = BundleInfoStubHelper.stubBunbleInfo();
        bundleInfo.getBundleGroups().forEach(bg -> bg.setName(null));

        // when the debundle gets composed
        final EntandoDeBundle deBundle = deBundleComposer.composeEntandoDeBundle(bundleInfo);

        // then the result is the expected one
        assertOnComposedEntandoDeBundle(deBundle, bundleInfo.getGitRepoAddress());

        // and no bundle groups have been added
        final List<String> pbcAnnotations = extractPbcAnnotationsFrom(deBundle);
        assertThat(pbcAnnotations).hasSize(0);
    }

    @Test
    void shouldBeAbleToComposeEntandoDeBundleWithBundleGroupsFromKubeCr() throws JsonProcessingException {

        when(bundleTagFilterManager.filterTagsByEnvironment(any())).thenAnswer(i -> ((Collection<?>)i.getArguments()[0]).stream());

        // given a bundle info with null bundle groups name
        final BundleInfo bundleInfo = BundleInfoStubHelper.stubBunbleInfo().setBundleGroups(null);

        final EntandoDeBundle bundle = BundleStubHelper.stubEntandoDeBundle();
        Map<String, String> annotations = new HashMap<>();
        annotations.put(pbcAnnotationsPrefix, objectMapper.writeValueAsString(BundleInfoStubHelper.GROUPS_NAME));
        bundle.getMetadata().setAnnotations(annotations);
        bundle.getMetadata().setName("something-" + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);

        bundle.setSpec(new EntandoDeBundleSpecBuilder()
                .withNewDetails()
                .withDescription("A bundle containing some demo components for Entando6")
                .withName(BundleInfoStubHelper.NAME).endDetails().build());

        K8SServiceClient k8SServiceClientLocal = mock(K8SServiceClient.class);
        when(k8SServiceClientLocal.getBundlesInObservedNamespaces(any())).thenReturn(Collections.singletonList(bundle));
        EntandoDeBundleComposer deBundleComposerLocal = new EntandoDeBundleComposer(bundleDownloaderFactory,
                k8SServiceClientLocal, bundleTagFilterManager);

        // when the debundle gets composed
        final EntandoDeBundle deBundle = deBundleComposerLocal.composeEntandoDeBundle(bundleInfo);

        // then the result is the expected one
        assertOnComposedEntandoDeBundle(deBundle, bundleInfo.getGitRepoAddress());

        // and no bundle groups have been added
        final List<String> pbcAnnotations = extractPbcAnnotationsFrom(deBundle);
        assertThat(pbcAnnotations).hasSize(BundleInfoStubHelper.GROUPS_NAME.size());
        assertThat(pbcAnnotations).contains(BundleInfoStubHelper.GROUPS_NAME.toArray(new String[]{}));
    }

    private void assertOnComposedEntandoDeBundle(EntandoDeBundle deBundle, String bundleUrl) {
        String name = deBundle.getMetadata().getName();
        assertThat(name).isEqualTo("something-" + BundleInfoStubHelper.GIT_REPO_ADDRESS_8_CHARS_SHA);
        assertOnFullLabelsDeBundleMap(deBundle.getMetadata().getLabels());

        final EntandoDeBundleDetails details = deBundle.getSpec().getDetails();
        assertThat(details.getName()).isEqualTo("something");
        assertThat(details.getDescription()).isEqualTo("bundle description");

        assertThat(details.getThumbnail()).isEqualTo(BundleInfoStubHelper.DESCR_IMAGE);

        List<EntandoDeBundleTag> entandoDeBundleTags = BundleStubHelper.TAG_LIST.stream()
                .map(tag -> new EntandoDeBundleTag(tag, null, null, bundleUrl))
                .collect(Collectors.toList());

        assertOnVersionsAndTags(deBundle, entandoDeBundleTags);
    }

    private void assertOnPbcAnnotations(EntandoDeBundle deBundle) {
        final List<String> actualPbcs = extractPbcAnnotationsFrom(deBundle);
        assertThat(actualPbcs).containsExactlyInAnyOrder(BundleInfoStubHelper.GROUPS_NAME.toArray(String[]::new));
    }

    private List<String> extractPbcAnnotationsFrom(EntandoDeBundle deBundle) throws RuntimeException {
        try {
            String pbcsValue = deBundle.getMetadata().getAnnotations().entrySet().stream()
                    .filter(e -> e.getKey().equals(pbcAnnotationsPrefix))
                    .findFirst()
                    .map(Entry::getValue)
                    .orElse(null);
            return Arrays.asList(objectMapper.readValue(pbcsValue, String[].class));
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Test
    void shouldThrowExceptionWhenComposingABundleAndReceivingANullOrInvalidUrl() {

        when(bundleTagFilterManager.filterTagsByEnvironment(any())).thenAnswer(i -> ((Collection<?>)i.getArguments()[0]).stream());

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

        when(bundleTagFilterManager.filterTagsByEnvironment(any())).thenAnswer(i -> ((Collection<?>)i.getArguments()[0]).stream());

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
    void shouldBeAbleToComposeEntandoDeBundleWithThumbnailFromDescriptorForBundleV5() {
        when(bundleTagFilterManager.filterTagsByEnvironment(any())).thenAnswer(i -> ((Collection<?>)i.getArguments()[0]).stream());

        final String DESCR_IMAGE = "data:image/png;base64,dGhpcyBpcyBub3QgYW4gaW1hZ2UK";
        List<String> tagList = new ArrayList<>();
        tagList.addAll(BundleStubHelper.TAG_LIST);

        bundleDownloaderFactory.setDefaultSupplier(() -> {
            Path bundleFolder;
            GitBundleDownloader git = Mockito.mock(GitBundleDownloader.class);
            try {
                bundleFolder = new ClassPathResource("bundle-v5").getFile().toPath();
                when(git.saveBundleLocally(anyString())).thenReturn(bundleFolder);
                when(git.fetchRemoteTags(anyString())).thenReturn(tagList);
            } catch (IOException e) {
                throw new RuntimeException("Impossible to read the bundle folder from test resources");
            }
            return git;
        });

        final EntandoDeBundle deBundle = deBundleComposer.composeEntandoDeBundle(bundleInfo);
        assertThat(deBundle.getSpec().getDetails().getThumbnail()).isEqualTo(DESCR_IMAGE);
        assertThat(deBundle.getMetadata().getLabels().get("bundle-type")).isEqualTo(BundleType.STANDARD_BUNDLE.getType());

    }

    @Test
    void shouldBeAbleToComposeEntandoDeBundleWithThumbnailFromDescriptorForBundleV1() {
        when(bundleTagFilterManager.filterTagsByEnvironment(any())).thenAnswer(i -> ((Collection<?>)i.getArguments()[0]).stream());

        List<String> tagList = new ArrayList<>();
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
        assertThat(deBundle.getSpec().getDetails().getThumbnail()).isEqualTo(bundleInfo.getDescriptionImage());

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
