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
import org.entando.kubernetes.model.digitalexchange.EntandoBundle;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.entando.kubernetes.repository.InstalledEntandoBundleRepository;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
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
        List<EntandoBundle> entandoBundles = service.getComponents();
        assertThat(entandoBundles.size()).isEqualTo(1);
        assertThat(entandoBundles.get(0).getDigitalExchangeName()).isEqualTo(DEFAULT_BUNDLE_NAMESPACE);
    }

    @Test
    public void shouldReturnInstalledComponents() {
        EntandoDeBundle bundle = TestEntitiesGenerator.getTestBundle();
        EntandoBundle component = EntandoBundle.newFrom(bundle);
        component.setInstalled(true);

        when(installedComponentRepository.findAll()).thenReturn(Collections.singletonList(component));
        List<EntandoBundle> components = service.getComponents();
        assertThat(components.size()).isEqualTo(1);
        assertThat(components.get(0).getId()).isEqualTo(bundle.getMetadata().getName());

        verify(installedComponentRepository).findAll();
    }
}
