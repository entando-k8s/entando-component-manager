package org.entando.kubernetes.service.digitalexchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.entando.kubernetes.TestEntitiesGenerator.DEFAULT_BUNDLE_NAMESPACE;
import static org.entando.kubernetes.TestEntitiesGenerator.getTestComponent;
import static org.entando.kubernetes.TestEntitiesGenerator.getTestJobEntity;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fabric8.kubernetes.client.KubernetesClientException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.api.Java6Assertions;
import org.entando.kubernetes.TestEntitiesGenerator;
import org.entando.kubernetes.assertionhelper.BundleAssertionHelper;
import org.entando.kubernetes.assertionhelper.SimpleRestResponseAssertionHelper;
import org.entando.kubernetes.client.K8SServiceClientTestDouble;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.EntandoValidationException;
import org.entando.kubernetes.model.bundle.BundleInfo;
import org.entando.kubernetes.model.bundle.BundleType;
import org.entando.kubernetes.model.bundle.EntandoBundle;
import org.entando.kubernetes.model.bundle.EntandoBundleVersion;
import org.entando.kubernetes.model.bundle.status.BundlesStatusItem;
import org.entando.kubernetes.model.bundle.status.BundlesStatusResult;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleBuilder;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpec;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpecBuilder;
import org.entando.kubernetes.model.job.EntandoBundleEntity;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.model.web.request.Filter;
import org.entando.kubernetes.model.web.request.FilterOperator;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.DeletedObjectResponse;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
import org.entando.kubernetes.service.digitalexchange.component.BundleStatusHelper;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleService;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleServiceImpl;
import org.entando.kubernetes.stubhelper.BundleInfoStubHelper;
import org.entando.kubernetes.stubhelper.BundleStatusItemStubHelper;
import org.entando.kubernetes.validator.ValidationFunctions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;

@Tag("unit")
public class EntandoBundleServiceTest {

    private final List<String> availableDigitalExchanges = Collections.singletonList(DEFAULT_BUNDLE_NAMESPACE);
    private K8SServiceClientTestDouble k8SServiceClient;
    private EntandoBundleService service;
    private InstalledEntandoBundleRepository installedComponentRepository;
    private EntandoBundleJobRepository jobRepository;
    private BundleStatusHelper bundleStatusHelper;
    private EntandoDeBundleComposer entandoDeBundleComposer;
    private EntandoBundleComponentJobRepository componentJobRepository;

    @BeforeEach
    public void setup() {
        k8SServiceClient = new K8SServiceClientTestDouble();
        jobRepository = Mockito.mock(EntandoBundleJobRepository.class);
        componentJobRepository = Mockito.mock(EntandoBundleComponentJobRepository.class);
        installedComponentRepository = Mockito.mock(InstalledEntandoBundleRepository.class);
        bundleStatusHelper = Mockito.mock(BundleStatusHelper.class);
        entandoDeBundleComposer = Mockito.mock(EntandoDeBundleComposer.class);

        service = new EntandoBundleServiceImpl(k8SServiceClient, availableDigitalExchanges, jobRepository,
                componentJobRepository, installedComponentRepository, bundleStatusHelper, entandoDeBundleComposer);
    }

    @AfterEach
    public void teardown() {
        k8SServiceClient.cleanInMemoryDatabases();
    }

    @Test
    public void shouldReturnAllComponentsAvailable() {
        k8SServiceClient.addInMemoryBundle(TestEntitiesGenerator.getTestBundle());
        when(installedComponentRepository.findAll()).thenReturn(Collections.emptyList());
        PagedMetadata<EntandoBundle> bundles = service.listBundles();
        assertThat(bundles.getTotalItems()).isEqualTo(1);
        assertThat(bundles.getBody().size()).isEqualTo(1);
    }

    @Test
    public void shouldReturnInstalledComponents() {
        EntandoDeBundle bundle = TestEntitiesGenerator.getTestBundle();
        EntandoBundleEntity component = getTestComponent();

        when(installedComponentRepository.findAll()).thenReturn(Collections.singletonList(component));
        PagedMetadata<EntandoBundle> components = service.listBundles();
        assertThat(components.getBody().size()).isEqualTo(1);
        assertThat(components.getBody().get(0).getCode()).isEqualTo(bundle.getMetadata().getName());
        assertThat(components.getTotalItems()).isEqualTo(1);

        verify(installedComponentRepository).findAll();
    }

    @Test
    void shouldCorrectlyPopulateCustomInstallationField() {
        EntandoDeBundle bundle = TestEntitiesGenerator.getTestBundle();
        EntandoBundleEntity component = getTestComponent();

        when(installedComponentRepository.findAll()).thenReturn(Collections.singletonList(component));
        when(jobRepository.findEntandoBundleJobEntityByIdIn(any(Set.class)))
                .thenReturn(Optional.of(Collections.singletonList(getTestJobEntity())));
        PagedMetadata<EntandoBundle> components = service.listBundles();
        assertThat(components.getBody().size()).isEqualTo(1);
        assertThat(components.getBody().get(0).getCode()).isEqualTo(bundle.getMetadata().getName());
        assertThat(components.getBody().get(0).getCustomInstallation()).isTrue();
        assertThat(components.getTotalItems()).isEqualTo(1);

        verify(installedComponentRepository).findAll();
    }

    @Test
    public void shouldSortAndFilter() {

        EntandoDeBundleSpec baseSpec = TestEntitiesGenerator.getTestEntandoDeBundleSpec();
        EntandoDeBundleSpec specBundleA = new EntandoDeBundleSpecBuilder()
                .withTags(baseSpec.getTags())
                .withNewDetails()
                .withName("bundleA")
                .withDistTags(baseSpec.getDetails().getDistTags())
                .withVersions(baseSpec.getDetails().getVersions())
                .withKeywords(baseSpec.getDetails().getKeywords())
                .withDescription(baseSpec.getDetails().getDescription())
                .endDetails()
                .build();
        EntandoDeBundleSpec specBundleB = new EntandoDeBundleSpecBuilder()
                .withTags(baseSpec.getTags())
                .withNewDetails()
                .withName("bundleB")
                .withDistTags(baseSpec.getDetails().getDistTags())
                .withVersions(baseSpec.getDetails().getVersions())
                .withKeywords(baseSpec.getDetails().getKeywords())
                .withDescription(baseSpec.getDetails().getDescription())
                .endDetails()
                .build();
        EntandoDeBundle bundleA = new EntandoDeBundleBuilder()
                .withNewMetadata()
                .withName("my-bundleA")
                .withNamespace(DEFAULT_BUNDLE_NAMESPACE)
                .endMetadata()
                .withSpec(specBundleA)
                .build();

        EntandoDeBundle bundleB = new EntandoDeBundleBuilder()
                .withNewMetadata()
                .withName("my-bundleB")
                .withNamespace(DEFAULT_BUNDLE_NAMESPACE)
                .endMetadata()
                .withSpec(specBundleB)
                .build();

        k8SServiceClient.addInMemoryBundle(bundleA);
        k8SServiceClient.addInMemoryBundle(bundleB);
        when(installedComponentRepository.findAll()).thenReturn(Collections.emptyList());

        PagedListRequest request = new PagedListRequest();
        request.setSort("title");
        request.setDirection(Filter.DESC_ORDER);
        PagedMetadata<EntandoBundle> components = service.listBundles(request);

        assertThat(components.getTotalItems()).isEqualTo(2);
        assertThat(components.getBody().get(0).getCode()).isEqualTo(bundleB.getMetadata().getName());
        assertThat(components.getBody().get(1).getCode()).isEqualTo(bundleA.getMetadata().getName());

        request = new PagedListRequest();
        request.addFilter(new Filter("name", "bundleA"));
        request.setDirection(Filter.DESC_ORDER);
        components = service.listBundles(request);

        assertThat(components.getTotalItems()).isEqualTo(1);
        assertThat(components.getBody().get(0).getCode()).isEqualTo(bundleA.getMetadata().getName());
    }

    @Test
    public void shouldFilterInstalledComponents() {

        EntandoDeBundleSpec baseSpec = TestEntitiesGenerator.getTestEntandoDeBundleSpec();
        EntandoDeBundleSpec specBundleA = new EntandoDeBundleSpecBuilder()
                .withTags(baseSpec.getTags())
                .withNewDetails()
                .withName("bundleA")
                .withDistTags(baseSpec.getDetails().getDistTags())
                .withVersions(baseSpec.getDetails().getVersions())
                .withKeywords(baseSpec.getDetails().getKeywords())
                .withDescription(baseSpec.getDetails().getDescription())
                .endDetails()
                .build();
        EntandoDeBundleSpec specBundleB = new EntandoDeBundleSpecBuilder()
                .withTags(baseSpec.getTags())
                .withNewDetails()
                .withName("bundleB")
                .withDistTags(baseSpec.getDetails().getDistTags())
                .withVersions(baseSpec.getDetails().getVersions())
                .withKeywords(baseSpec.getDetails().getKeywords())
                .withDescription(baseSpec.getDetails().getDescription())
                .endDetails()
                .build();
        EntandoDeBundle bundleA = new EntandoDeBundleBuilder()
                .withNewMetadata()
                .withName("my-bundleA")
                .withNamespace(DEFAULT_BUNDLE_NAMESPACE)
                .endMetadata()
                .withSpec(specBundleA)
                .build();

        EntandoDeBundle bundleB = new EntandoDeBundleBuilder()
                .withNewMetadata()
                .withName("my-bundleB")
                .withNamespace(DEFAULT_BUNDLE_NAMESPACE)
                .endMetadata()
                .withSpec(specBundleB)
                .build();

        String code = bundleB.getMetadata().getName();
        String title = bundleB.getSpec().getDetails().getName();
        final EntandoBundleJobEntity installedJob = getTestJobEntity(code, title);
        final EntandoBundleEntity installedComponent = getTestComponent(code, title);

        k8SServiceClient.addInMemoryBundle(bundleA);
        k8SServiceClient.addInMemoryBundle(bundleB);

        when(installedComponentRepository.existsById(eq(code))).thenReturn(true);
        Mockito.when(jobRepository
                        .findFirstByComponentIdAndStatusOrderByStartedAtDesc(eq(code), eq(JobStatus.INSTALL_COMPLETED)))
                .thenReturn(Optional.of(installedJob));
        Mockito.when(jobRepository.findFirstByComponentIdOrderByStartedAtDesc(eq(code)))
                .thenReturn(Optional.of(installedJob));

        when(installedComponentRepository.findAll()).thenReturn(Collections.singletonList(installedComponent));

        PagedListRequest request = new PagedListRequest();
        request.addFilter(new Filter("installed", "true"));
        PagedMetadata<EntandoBundle> components = service.listBundles(request);

        assertThat(components.getTotalItems()).isEqualTo(1);
        assertThat(components.getBody().get(0).getCode()).isEqualTo("my-bundleB");

        request = new PagedListRequest();
        request.addFilter(new Filter("installed", "false"));

        components = service.listBundles(request);
        assertThat(components.getTotalItems()).isEqualTo(1);
        assertThat(components.getBody().get(0).getCode()).isEqualTo("my-bundleA");
    }

    @Test
    public void shouldFilterByBundleContent() {

        EntandoDeBundleSpec baseSpec = TestEntitiesGenerator.getTestEntandoDeBundleSpec();
        EntandoDeBundleSpec specBundleA = new EntandoDeBundleSpecBuilder()
                .withTags(baseSpec.getTags())
                .withNewDetails()
                .withName("bundleA")
                .withDistTags(baseSpec.getDetails().getDistTags())
                .withVersions(baseSpec.getDetails().getVersions())
                .withKeywords(baseSpec.getDetails().getKeywords())
                .withDescription(baseSpec.getDetails().getDescription())
                .endDetails()
                .build();
        EntandoDeBundleSpec specBundleB = new EntandoDeBundleSpecBuilder()
                .withTags(baseSpec.getTags())
                .withNewDetails()
                .withName("bundleB")
                .withDistTags(baseSpec.getDetails().getDistTags())
                .withVersions(baseSpec.getDetails().getVersions())
                .withKeywords(baseSpec.getDetails().getKeywords())
                .withDescription(baseSpec.getDetails().getDescription())
                .endDetails()
                .build();
        EntandoDeBundleSpec specBundleC = new EntandoDeBundleSpecBuilder()
                .withTags(baseSpec.getTags())
                .withNewDetails()
                .withName("bundleC")
                .withDistTags(baseSpec.getDetails().getDistTags())
                .withVersions(baseSpec.getDetails().getVersions())
                .withKeywords(baseSpec.getDetails().getKeywords())
                .withDescription(baseSpec.getDetails().getDescription())
                .endDetails()
                .build();

        EntandoDeBundle bundleA = new EntandoDeBundleBuilder()
                .withNewMetadata()
                .withName("my-bundleA")
                .withNamespace(DEFAULT_BUNDLE_NAMESPACE)
                .addToLabels("widget", "true")
                .addToLabels("page", "true")
                .addToLabels("pageModel", "true")
                .endMetadata()
                .withSpec(specBundleA)
                .build();

        EntandoDeBundle bundleB = new EntandoDeBundleBuilder()
                .withNewMetadata()
                .withName("my-bundleB")
                .withNamespace(DEFAULT_BUNDLE_NAMESPACE)
                .addToLabels("widget", "true")
                .addToLabels("contentType", "true")
                .addToLabels("contentModel", "true")
                .endMetadata()
                .withSpec(specBundleB)
                .build();

        EntandoDeBundle bundleC = new EntandoDeBundleBuilder()
                .withNewMetadata()
                .withName("my-bundleC")
                .withNamespace(DEFAULT_BUNDLE_NAMESPACE)
                .addToLabels("plugin", "true")
                .addToLabels("contentType", "true")
                .endMetadata()
                .withSpec(specBundleC)
                .build();

        k8SServiceClient.addInMemoryBundle(bundleA);
        k8SServiceClient.addInMemoryBundle(bundleB);
        k8SServiceClient.addInMemoryBundle(bundleC);
        when(installedComponentRepository.findAll()).thenReturn(Collections.emptyList());

        PagedListRequest request = new PagedListRequest();
        request.addFilter(new Filter("type", "widget"));
        PagedMetadata<EntandoBundle> components = service.listBundles(request);

        assertThat(components.getTotalItems()).isEqualTo(2);
        assertThat(components.getBody().get(0).getCode()).isEqualTo("my-bundleA");
        assertThat(components.getBody().get(1).getCode()).isEqualTo("my-bundleB");
        assertThat(components.getBody().get(0).getComponentTypes().contains("widget")).isTrue();
        assertThat(components.getBody().get(1).getComponentTypes().contains("widget")).isTrue();

        request = new PagedListRequest();
        request.addFilter(new Filter("type", "contentType"));

        components = service.listBundles(request);
        assertThat(components.getTotalItems()).isEqualTo(2);
        assertThat(components.getBody().get(0).getCode()).isEqualTo("my-bundleB");
        assertThat(components.getBody().get(1).getCode()).isEqualTo("my-bundleC");
        assertThat(components.getBody().get(0).getComponentTypes().contains("contentType")).isTrue();
        assertThat(components.getBody().get(1).getComponentTypes().contains("contentType")).isTrue();

        request = new PagedListRequest();
        request.addFilter(new Filter("type", "page"));

        components = service.listBundles(request);
        assertThat(components.getTotalItems()).isEqualTo(1);
        assertThat(components.getBody().get(0).getCode()).isEqualTo("my-bundleA");
        assertThat(components.getBody().get(0).getComponentTypes().contains("page")).isTrue();

        request = new PagedListRequest();
        Filter multiValueFilter = new Filter();
        multiValueFilter.setAttribute("type");
        multiValueFilter.setOperator(FilterOperator.EQUAL.getValue());
        multiValueFilter.setAllowedValues(new String[]{"page", "contentType", "plugin"});
        request.addFilter(multiValueFilter);
        components = service.listBundles(request);
        assertThat(components.getTotalItems()).isEqualTo(3);

        request = new PagedListRequest();
        request.addFilter(new Filter("type", "plugin"));
        components = service.listBundles(request);
        assertThat(components.getTotalItems()).isEqualTo(1);
        assertThat(components.getBody().get(0).getCode()).isEqualTo("my-bundleC");

        request = new PagedListRequest();
        request.addFilter(new Filter("type", "resource"));
        components = service.listBundles(request);
        assertThat(components.getTotalItems()).isEqualTo(0);
    }

    @Test
    public void shouldReturnCorrectBundleVersions() {
        EntandoDeBundle bundle = TestEntitiesGenerator.getTestBundle();
        bundle.getSpec().getDetails().getVersions().add("version-non-present-in-tags");

        k8SServiceClient.addInMemoryBundle(bundle);
        when(installedComponentRepository.findAll()).thenReturn(Collections.emptyList());

        PagedMetadata<EntandoBundle> bundles = service.listBundles();

        assertThat(bundles.getTotalItems()).isEqualTo(1);
        assertThat(bundles.getBody().size()).isEqualTo(1);
        assertThat(bundles.getBody().get(0).getVersions().size()).isEqualTo(1);
        assertThat(bundles.getBody().get(0).getVersions().get(0).getVersion()).isEqualTo("0.0.1");
    }

    @Test
    void shouldGetLatestVersionFromPropertySpecDistTagsLatest() {

        EntandoDeBundle bundle = TestEntitiesGenerator.getTestBundle();
        EntandoBundle entandoBundle = service.convertToBundleFromEcr(bundle);
        assertThat(entandoBundle.getLatestVersion().get().getVersion()).isEqualTo("0.0.15");
    }

    @Test
    void shouldGetLatestVersionFromVersionListIfNotManuallySpecified() {

        EntandoDeBundle bundle = TestEntitiesGenerator.getTestBundle();
        bundle.getSpec().getDetails().getDistTags().remove("latest");
        bundle.getSpec().getDetails().getVersions().add("0.0.5");
        EntandoBundle entandoBundle = service.convertToBundleFromEcr(bundle);
        assertThat(entandoBundle.getLatestVersion().get().getVersion()).isEqualTo("0.0.5");
    }

    @Test
    void shouldSuccessfullyDeployADeBundle() {
        final BundleInfo bundleInfo = BundleInfoStubHelper.stubBunbleInfo();
        final EntandoDeBundle deBundle = TestEntitiesGenerator.getTestBundle();
        when(entandoDeBundleComposer.composeEntandoDeBundle(any())).thenReturn(deBundle);

        final EntandoBundle bundle = service.deployDeBundle(bundleInfo);
        BundleAssertionHelper.assertOnBundleAndDeBundle(bundle, deBundle, BundleType.STANDARD_BUNDLE, null, null, null,
                new EntandoBundleVersion().setVersion(TestEntitiesGenerator.LATEST_VERSION));
    }

    @Test
    void shouldThrowExceptionWhileDeployingABundleAndTheComposerThrowsIt() {
        when(entandoDeBundleComposer.composeEntandoDeBundle(any())).thenThrow(EntandoComponentManagerException.class);
        assertThrows(EntandoComponentManagerException.class, () -> service.deployDeBundle(null));
    }

    @Test
    void shouldThrowExceptionWhileDeployingABundleAndTheK8sServiceClientThrowsIt() {
        K8SServiceClient mockK8SServiceClient = Mockito.mock(K8SServiceClient.class);
        service = new EntandoBundleServiceImpl(mockK8SServiceClient, availableDigitalExchanges, jobRepository,
                componentJobRepository, installedComponentRepository, bundleStatusHelper, entandoDeBundleComposer);
        when(mockK8SServiceClient.deployDeBundle(any())).thenThrow(RestClientResponseException.class);
        assertThrows(RestClientResponseException.class, () -> service.deployDeBundle(null));
    }

    @Test
    void shouldSuccessfullyUndeployAnExistingEntandoDeBundleAndReturnItsName() {

        // given that the k8s-service answer with a 200
        // when trying to undeploy a bundle
        final String deleteBundleName = service.undeployDeBundle(BundleInfoStubHelper.NAME);

        // then the expected bundle name is returned
        assertThat(deleteBundleName).isEqualTo(BundleInfoStubHelper.NAME);
    }


    @Test
    void shouldThrowExceptionWhenEndeployingABundleWithAnEmptyOrNullName() {

        assertThrows(EntandoValidationException.class, () -> service.undeployDeBundle(null));
        assertThrows(EntandoValidationException.class, () -> service.undeployDeBundle(""));
    }

    @Test
    void shouldReturnTheExpectedBundlesStatusResult() throws MalformedURLException {

        when(bundleStatusHelper.composeBundleStatusItemByURL(
                eq(new URL(BundleStatusItemStubHelper.ID_INSTALLED_NOT_DEPLOYED)),
                any(), any(), any())).thenReturn(BundleStatusItemStubHelper.stubBundleStatusItemInstalledNotDeployed());
        when(bundleStatusHelper.composeBundleStatusItemByURL(eq(new URL(BundleStatusItemStubHelper.ID_INSTALLED)),
                any(), any(), any())).thenReturn(BundleStatusItemStubHelper.stubBundleStatusItemInstalled());
        when(bundleStatusHelper.composeBundleStatusItemByURL(eq(new URL(BundleStatusItemStubHelper.ID_DEPLOYED)),
                any(), any(), any())).thenReturn(BundleStatusItemStubHelper.stubBundleStatusItemDeployed());
        when(bundleStatusHelper.composeBundleStatusItemByURL(eq(new URL(BundleStatusItemStubHelper.ID_NOT_FOUND)),
                any(), any(), any())).thenReturn(BundleStatusItemStubHelper.stubBundleStatusItemNotFoundByUrl());

        // given a list of bundle id (repo url)
        final List<URL> bundleIds = List.of(new URL(BundleStatusItemStubHelper.ID_INSTALLED),
                new URL(BundleStatusItemStubHelper.ID_INSTALLED_NOT_DEPLOYED),
                new URL(BundleStatusItemStubHelper.ID_NOT_FOUND), new URL(BundleStatusItemStubHelper.ID_DEPLOYED));

        // when I ask for their status
        final BundlesStatusResult bundlesStatusResult = service.getBundlesStatus(bundleIds);

        // then I expect to receive the correct list of bundle status item
        List<BundlesStatusItem> expectedList = List.of(BundleStatusItemStubHelper.stubBundleStatusItemInstalled(),
                BundleStatusItemStubHelper.stubBundleStatusItemInstalledNotDeployed(),
                BundleStatusItemStubHelper.stubBundleStatusItemNotFoundByUrl(),
                BundleStatusItemStubHelper.stubBundleStatusItemDeployed());
        assertThat(bundlesStatusResult.getBundlesStatuses()).containsExactlyElementsOf(expectedList);
    }

    @Test
    void shouldReturnTheExpectedBundleItem() {

        // given that the bundle status helper returns the correct bundle status item
        when(bundleStatusHelper.composeBundleStatusItemByName(
                eq(BundleStatusItemStubHelper.NAME_INSTALLED), any(), any(), any()))
                .thenReturn(BundleStatusItemStubHelper.stubBundleStatusItemInstalled());

        // when I ask for their status
        final BundlesStatusItem bundlesStatusItem = service.getSingleBundleStatus(BundleStatusItemStubHelper.NAME_INSTALLED);

        // then I expect to receive the correct list of bundle status item
        BundlesStatusItem expected = BundleStatusItemStubHelper.stubBundleStatusItemInstalled();
        assertThat(bundlesStatusItem).isEqualToComparingFieldByField(expected);
    }

    @Test
    void getBundleByRepoUrl_withInvalidUrl_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> service.getBundleByRepoUrl("invalid_url"));
    }

    @Test
    void getBundleByRepoUrl_withValidUrlAndInstalledBundle_shouldReturnInstalledBundle() {

        final EntandoBundleEntity bundleEntity = getTestComponent();
        when(installedComponentRepository.findFirstByRepoUrlWithUrl(
                ValidationFunctions.composeUrlOrThrow(TestEntitiesGenerator.REPO_URL, "null URL",
                        "invalid URL"))).thenReturn(Optional.of(bundleEntity));
        Optional<EntandoBundle> entandoBundle = service.getBundleByRepoUrl(
                "aHR0cHM6Ly9naXRodWIuY29tL2ZpcmVnbG92ZXMtYnVuZGxlcy94bWFzYnVuZGxlLmdpdA");

        assertThat(entandoBundle).isPresent();
    }

    @Test
    void getBundleByRepoUrl_withValidUrlAndAvailableBundle_shouldReturnAvailableBundle() {

        EntandoDeBundle bundle = TestEntitiesGenerator.getTestBundle();
        k8SServiceClient.addInMemoryBundle(bundle);

        when(installedComponentRepository.findFirstByRepoUrlWithUrl(any())).thenReturn(Optional.empty());
        Optional<EntandoBundle> entandoBundle = service.getBundleByRepoUrl(
                "aHR0cDovL2xvY2FsaG9zdDo4MDgxL3JlcG9zaXRvcnkvbnBtLWludGVybmFsL215LWJ1bmRsZS8tL215LWJ1bmRsZS0wLjAuMS50Z3o");

        assertThat(entandoBundle).isPresent();
    }

    @Test
    void getBundleByRepoUrl_withValidUrlAndNoBundles_shouldReturnEmptyBundle() {

        when(installedComponentRepository.findFirstByRepoUrlWithUrl(any())).thenReturn(Optional.empty());
        Optional<EntandoBundle> entandoBundle = service.getBundleByRepoUrl(
                "aHR0cDovL2xvY2FsaG9zdDo4MDgxL3JlcG9zaXRvcnkvbnBtLWludGVybmFsL215LWJ1bmRsZS8tL215LWJ1bmRsZS0wLjAuMS50Z3oK");

        assertThat(entandoBundle).isEmpty();
    }
}
