package org.entando.kubernetes.service.digitalexchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.entando.kubernetes.TestEntitiesGenerator.DEFAULT_BUNDLE_NAMESPACE;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.entando.kubernetes.TestEntitiesGenerator;
import org.entando.kubernetes.client.K8SServiceClientTestDouble;
import org.entando.kubernetes.model.bundle.EntandoBundle;
import org.entando.kubernetes.model.bundle.EntandoComponentBundle;
import org.entando.kubernetes.model.bundle.EntandoComponentBundleBuilder;
import org.entando.kubernetes.model.bundle.EntandoComponentBundleSpec;
import org.entando.kubernetes.model.bundle.EntandoComponentBundleSpecBuilder;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleEntity;
import org.entando.kubernetes.model.job.EntandoBundleJob;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.model.web.request.Filter;
import org.entando.kubernetes.model.web.request.FilterOperator;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleService;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@Tag("unit")
public class EntandoBundleServiceTest {

    private K8SServiceClientTestDouble k8SServiceClient;
    private EntandoBundleService service;
    private InstalledEntandoBundleRepository installedComponentRepository;

    private List<String> availableDigitalExchanges = Collections.singletonList(DEFAULT_BUNDLE_NAMESPACE);

    @BeforeEach
    public void setup() {
        k8SServiceClient = new K8SServiceClientTestDouble();
        EntandoBundleJobRepository jobRepository = Mockito.mock(EntandoBundleJobRepository.class);
        EntandoBundleComponentJobRepository componentJobRepository = Mockito
                .mock(EntandoBundleComponentJobRepository.class);
        installedComponentRepository = Mockito.mock(InstalledEntandoBundleRepository.class);
        service = new EntandoBundleServiceImpl(k8SServiceClient, availableDigitalExchanges, jobRepository,
                componentJobRepository, installedComponentRepository);
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
        EntandoComponentBundle bundle = TestEntitiesGenerator.getTestBundle();


        EntandoBundleEntity component = EntandoBundleEntity.newFrom(bundle);
        component.setId(UUID.randomUUID());
        EntandoBundleJob installJob = getTestInstallJob(component);
        component.setInstalledJob(installJob);

        when(installedComponentRepository.findAll()).thenReturn(Collections.singletonList(component));
        PagedMetadata<EntandoBundle> components = service.listBundles();
        assertThat(components.getBody().size()).isEqualTo(1);
        assertThat(components.getBody().get(0).getEcrId()).isEqualTo(bundle.getMetadata().getName());
        assertThat(components.getTotalItems()).isEqualTo(1);

        verify(installedComponentRepository).findAll();
    }

    @Test
    public void shouldSortAndFilter() {

        EntandoComponentBundleSpec baseSpec = TestEntitiesGenerator.getTestEntandoComponentBundleSpec();
        EntandoComponentBundleSpec specBundleA = new EntandoComponentBundleSpecBuilder()
                .withCode("bundleA")
                .withTitle("bundleA")
                .withDescription(baseSpec.getDescription())
                .withOrganization(baseSpec.getOrganization())
                .withVersions(baseSpec.getVersions())
                .build();
        EntandoComponentBundleSpec specBundleB = new EntandoComponentBundleSpecBuilder()
                .withCode("bundleB")
                .withTitle("bundleB")
                .withDescription(baseSpec.getDescription())
                .withOrganization(baseSpec.getOrganization())
                .withVersions(baseSpec.getVersions())
                .build();
        EntandoComponentBundle bundleA = new EntandoComponentBundleBuilder()
                .withNewMetadata()
                .withName("my-bundleA")
                .withNamespace(DEFAULT_BUNDLE_NAMESPACE)
                .endMetadata()
                .withSpec(specBundleA)
                .build();

        EntandoComponentBundle bundleB = new EntandoComponentBundleBuilder()
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
        request.setSort("name");
        request.setDirection(Filter.DESC_ORDER);
        PagedMetadata<EntandoBundle> components = service.listBundles(request);

        assertThat(components.getTotalItems()).isEqualTo(2);
        assertThat(components.getBody().get(0).getCode()).isEqualTo(bundleB.getSpec().getTitle());
        assertThat(components.getBody().get(1).getCode()).isEqualTo(bundleA.getSpec().getTitle());

        request = new PagedListRequest();
        request.addFilter(new Filter("name", "bundleA"));
        request.setDirection(Filter.DESC_ORDER);
        components = service.listBundles(request);

        assertThat(components.getTotalItems()).isEqualTo(1);
        assertThat(components.getBody().get(0).getCode()).isEqualTo(bundleA.getSpec().getTitle());
    }

    @Test
    public void shouldFilterInstalledComponents() {

        EntandoComponentBundleSpec baseSpec = TestEntitiesGenerator.getTestEntandoComponentBundleSpec();
        EntandoComponentBundleSpec specBundleA = new EntandoComponentBundleSpecBuilder()
                .withVersions(baseSpec.getVersions())
                .withCode("bundleA")
                .withTitle("bundleA")
                .withVersions(baseSpec.getVersions())
                .withDescription(baseSpec.getDescription())
                .build();
        EntandoComponentBundleSpec specBundleB = new EntandoComponentBundleSpecBuilder()
                .withVersions(baseSpec.getVersions())
                .withCode("bundleB")
                .withTitle("bundleB")
                .withVersions(baseSpec.getVersions())
                .withDescription(baseSpec.getDescription())
                .build();
        EntandoComponentBundle bundleA = new EntandoComponentBundleBuilder()
                .withNewMetadata()
                .withName("my-bundleA")
                .withNamespace(DEFAULT_BUNDLE_NAMESPACE)
                .endMetadata()
                .withSpec(specBundleA)
                .build();

        EntandoComponentBundle bundleB = new EntandoComponentBundleBuilder()
                .withNewMetadata()
                .withName("my-bundleB")
                .withNamespace(DEFAULT_BUNDLE_NAMESPACE)
                .endMetadata()
                .withSpec(specBundleB)
                .build();

        EntandoBundleEntity installedComponent = EntandoBundleEntity.newFrom(bundleB);
        installedComponent.setId(UUID.randomUUID());
        EntandoBundleJob installJob = getTestInstallJob(installedComponent);
        installedComponent.setInstalledJob(installJob);

        k8SServiceClient.addInMemoryBundle(bundleA);
        when(installedComponentRepository.findAll()).thenReturn(Collections.singletonList(installedComponent));

        PagedListRequest request = new PagedListRequest();
        request.addFilter(new Filter("installed", "true"));
        PagedMetadata<EntandoBundle> components = service.listBundles(request);

        assertThat(components.getTotalItems()).isEqualTo(1);
        assertThat(components.getBody().get(0).getCode()).isEqualTo("bundleB");

        request = new PagedListRequest();
        request.addFilter(new Filter("installed", "false"));

        components = service.listBundles(request);
        assertThat(components.getTotalItems()).isEqualTo(1);
        assertThat(components.getBody().get(0).getCode()).isEqualTo("bundleA");
    }

    private EntandoBundleJob getTestInstallJob(EntandoBundleEntity installedComponent) {
        EntandoBundleJob installJob = new EntandoBundleJob();
        installJob.setStartedAt(LocalDateTime.of(2020, 12, 13, 12, 0 ));
        installJob.setFinishedAt(LocalDateTime.of(2020, 12, 13, 12, 1));
        installJob.setComponentId(installedComponent.getEcrId());
        installJob.setComponentName(installedComponent.getCode());
        installJob.setComponentVersion("0.0.1");
        installJob. setStatus(JobStatus.INSTALL_COMPLETED);
        return installJob;
    }

    @Test
    public void shouldFilterByBundleContent() {
        EntandoComponentBundleSpec baseSpec = TestEntitiesGenerator.getTestEntandoComponentBundleSpec();
        EntandoComponentBundleSpec specBundleA = new EntandoComponentBundleSpecBuilder()
                .withVersions(baseSpec.getVersions())
                .withCode("bundleA")
                .withTitle("bundleA")
                .withVersions(baseSpec.getVersions())
                .withDescription(baseSpec.getDescription())
                .build();
        EntandoComponentBundleSpec specBundleB = new EntandoComponentBundleSpecBuilder()
                .withVersions(baseSpec.getVersions())
                .withCode("bundleB")
                .withTitle("bundleB")
                .withVersions(baseSpec.getVersions())
                .withDescription(baseSpec.getDescription())
                .build();
        EntandoComponentBundleSpec specBundleC = new EntandoComponentBundleSpecBuilder()
                .withVersions(baseSpec.getVersions())
                .withCode("bundleC")
                .withTitle("bundleC")
                .withVersions(baseSpec.getVersions())
                .withDescription(baseSpec.getDescription())
                .build();

        EntandoComponentBundle bundleA = new EntandoComponentBundleBuilder()
                .withNewMetadata()
                .withName("my-bundleA")
                .withNamespace(DEFAULT_BUNDLE_NAMESPACE)
                .addToLabels("widget", "true")
                .addToLabels("page", "true")
                .addToLabels("pageModel", "true")
                .endMetadata()
                .withSpec(specBundleA)
                .build();

        EntandoComponentBundle bundleB = new EntandoComponentBundleBuilder()
                .withNewMetadata()
                .withName("my-bundleB")
                .withNamespace(DEFAULT_BUNDLE_NAMESPACE)
                .addToLabels("widget", "true")
                .addToLabels("contentType", "true")
                .addToLabels("contentModel", "true")
                .endMetadata()
                .withSpec(specBundleB)
                .build();

        EntandoComponentBundle bundleC = new EntandoComponentBundleBuilder()
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
        assertThat(components.getBody().get(0).getCode()).isEqualTo("bundleA");
        assertThat(components.getBody().get(1).getCode()).isEqualTo("bundleB");
        assertThat(components.getBody().get(0).getComponentTypes().contains("widget")).isTrue();
        assertThat(components.getBody().get(1).getComponentTypes().contains("widget")).isTrue();

        request = new PagedListRequest();
        request.addFilter(new Filter("type", "contentType"));

        components = service.listBundles(request);
        assertThat(components.getTotalItems()).isEqualTo(2);
        assertThat(components.getBody().get(0).getCode()).isEqualTo("bundleB");
        assertThat(components.getBody().get(1).getCode()).isEqualTo("bundleC");
        assertThat(components.getBody().get(0).getComponentTypes().contains("contentType")).isTrue();
        assertThat(components.getBody().get(1).getComponentTypes().contains("contentType")).isTrue();

        request = new PagedListRequest();
        request.addFilter(new Filter("type", "page"));

        components = service.listBundles(request);
        assertThat(components.getTotalItems()).isEqualTo(1);
        assertThat(components.getBody().get(0).getCode()).isEqualTo("bundleA");
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
        assertThat(components.getBody().get(0).getCode()).isEqualTo("bundleC");

        request = new PagedListRequest();
        request.addFilter(new Filter("type", "resource"));
        components = service.listBundles(request);
        assertThat(components.getTotalItems()).isEqualTo(0);
    }
}
