package org.entando.kubernetes.service.digitalexchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.entando.kubernetes.TestEntitiesGenerator.DEFAULT_BUNDLE_NAMESPACE;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.entando.kubernetes.TestEntitiesGenerator;
import org.entando.kubernetes.client.K8SServiceClientTestDouble;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleBuilder;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpec;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpecBuilder;
import org.entando.kubernetes.model.digitalexchange.EntandoBundle;
import org.entando.kubernetes.model.web.request.Filter;
import org.entando.kubernetes.model.web.request.FilterOperator;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.response.PagedMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@Tag("unit")
public class EntandoBundleServiceTest {

    private K8SServiceClientTestDouble k8SServiceClient;
    private EntandoBundleService service;
    private EntandoBundleJobRepository jobRepository;
    private EntandoBundleComponentJobRepository componentJobRepository;
    private InstalledEntandoBundleRepository installedComponentRepository;

    private List<String> availableDigitalExchanges = Collections.singletonList(DEFAULT_BUNDLE_NAMESPACE);

    @BeforeEach
    public void setup() {
        k8SServiceClient = new K8SServiceClientTestDouble();
        jobRepository = Mockito.mock(EntandoBundleJobRepository.class);
        componentJobRepository = Mockito.mock(EntandoBundleComponentJobRepository.class);
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
        PagedMetadata<EntandoBundle> bundles = service.getComponents();
        assertThat(bundles.getTotalItems()).isEqualTo(1);
        assertThat(bundles.getBody().size()).isEqualTo(1);
        assertThat(bundles.getBody().get(0).getDigitalExchangeName()).isEqualTo(DEFAULT_BUNDLE_NAMESPACE);
    }

    @Test
    public void shouldReturnInstalledComponents() {
        EntandoDeBundle bundle = TestEntitiesGenerator.getTestBundle();
        EntandoBundle component = EntandoBundle.newFrom(bundle);
        component.setInstalled(true);

        when(installedComponentRepository.findAll()).thenReturn(Collections.singletonList(component));
        PagedMetadata<EntandoBundle> components = service.getComponents();
        assertThat(components.getBody().size()).isEqualTo(1);
        assertThat(components.getBody().get(0).getId()).isEqualTo(bundle.getMetadata().getName());
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
        request.setSort("name");
        request.setDirection(Filter.DESC_ORDER);
        PagedMetadata<DigitalExchangeComponent> components = service.getComponents(request);

        assertThat(components.getTotalItems()).isEqualTo(2);
        assertThat(components.getBody().get(0).getName()).isEqualTo(bundleB.getSpec().getDetails().getName());
        assertThat(components.getBody().get(1).getName()).isEqualTo(bundleA.getSpec().getDetails().getName());

        request = new PagedListRequest();
        request.addFilter(new Filter("name", "bundleA"));
        request.setDirection(Filter.DESC_ORDER);
        components = service.getComponents(request);

        assertThat(components.getTotalItems()).isEqualTo(1);
        assertThat(components.getBody().get(0).getName()).isEqualTo(bundleA.getSpec().getDetails().getName());
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

        DigitalExchangeComponent installedComponent = DigitalExchangeComponent.newFrom(bundleB);
        installedComponent.setInstalled(true);

        k8SServiceClient.addInMemoryBundle(bundleA);
        when(installedComponentRepository.findAll()).thenReturn(Collections.singletonList(installedComponent));

        PagedListRequest request = new PagedListRequest();
        request.addFilter(new Filter("installed", "true"));
        PagedMetadata<DigitalExchangeComponent> components = service.getComponents(request);

        assertThat(components.getTotalItems()).isEqualTo(1);
        assertThat(components.getBody().get(0).getName()).isEqualTo("bundleB");

        request = new PagedListRequest();
        request.addFilter(new Filter("installed", "false"));

        components = service.getComponents(request);
        assertThat(components.getTotalItems()).isEqualTo(1);
        assertThat(components.getBody().get(0).getName()).isEqualTo("bundleA");
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
                .addToLabels("page_model", "true")
                .endMetadata()
                .withSpec(specBundleA)
                .build();

        EntandoDeBundle bundleB = new EntandoDeBundleBuilder()
                .withNewMetadata()
                .withName("my-bundleB")
                .withNamespace(DEFAULT_BUNDLE_NAMESPACE)
                .addToLabels("widget", "true")
                .addToLabels("content_type", "true")
                .addToLabels("content_model", "true")
                .endMetadata()
                .withSpec(specBundleB)
                .build();

        EntandoDeBundle bundleC = new EntandoDeBundleBuilder()
                .withNewMetadata()
                .withName("my-bundleC")
                .withNamespace(DEFAULT_BUNDLE_NAMESPACE)
                .addToLabels("plugin", "true")
                .addToLabels("content_type", "true")
                .endMetadata()
                .withSpec(specBundleC)
                .build();

        k8SServiceClient.addInMemoryBundle(bundleA);
        k8SServiceClient.addInMemoryBundle(bundleB);
        k8SServiceClient.addInMemoryBundle(bundleC);
        when(installedComponentRepository.findAll()).thenReturn(Collections.emptyList());

        PagedListRequest request = new PagedListRequest();
        request.addFilter(new Filter("type", "widget"));
        PagedMetadata<DigitalExchangeComponent> components = service.getComponents(request);

        assertThat(components.getTotalItems()).isEqualTo(2);
        assertThat(components.getBody().get(0).getName()).isEqualTo("bundleA");
        assertThat(components.getBody().get(1).getName()).isEqualTo("bundleB");
        assertThat(components.getBody().get(0).getType().contains("widget")).isTrue();
        assertThat(components.getBody().get(1).getType().contains("widget")).isTrue();

        request = new PagedListRequest();
        request.addFilter(new Filter("type", "content_type"));

        components = service.getComponents(request);
        assertThat(components.getTotalItems()).isEqualTo(2);
        assertThat(components.getBody().get(0).getName()).isEqualTo("bundleB");
        assertThat(components.getBody().get(1).getName()).isEqualTo("bundleC");
        assertThat(components.getBody().get(0).getType().contains("content_type")).isTrue();
        assertThat(components.getBody().get(1).getType().contains("content_type")).isTrue();

        request = new PagedListRequest();
        request.addFilter(new Filter("type", "page"));

        components = service.getComponents(request);
        assertThat(components.getTotalItems()).isEqualTo(1);
        assertThat(components.getBody().get(0).getName()).isEqualTo("bundleA");
        assertThat(components.getBody().get(0).getType().contains("page")).isTrue();


        request = new PagedListRequest();
        Filter multiValueFilter = new Filter();
        multiValueFilter.setAttribute("type");
        multiValueFilter.setOperator(FilterOperator.EQUAL.getValue());
        multiValueFilter.setAllowedValues(new String[]{"page", "content_type", "plugin"});
        request.addFilter(multiValueFilter);
        components = service.getComponents(request);
        assertThat(components.getTotalItems()).isEqualTo(3);

        request = new PagedListRequest();
        request.addFilter(new Filter("type", "plugin"));
        components = service.getComponents(request);
        assertThat(components.getTotalItems()).isEqualTo(1);
        assertThat(components.getBody().get(0).getName()).isEqualTo("bundleC");

        request = new PagedListRequest();
        request.addFilter(new Filter("type", "resource"));
        components = service.getComponents(request);
        assertThat(components.getTotalItems()).isEqualTo(0);
    }
}
