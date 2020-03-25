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
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeComponent;
import org.entando.kubernetes.repository.DigitalExchangeInstalledComponentRepository;
import org.entando.kubernetes.repository.DigitalExchangeJobRepository;
import org.entando.kubernetes.service.digitalexchange.component.DigitalExchangeComponentsService;
import org.entando.kubernetes.service.digitalexchange.component.DigitalExchangeComponentsServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@Tag("unit")
public class DigitalExchangeComponentsServiceTest {

    private K8SServiceClientTestDouble k8SServiceClient;
    private DigitalExchangeComponentsService service;
    private DigitalExchangeJobRepository jobRepository;
    private DigitalExchangeInstalledComponentRepository installedComponentRepository;

    private List<String> availableDigitalExchanges = Collections.singletonList(DEFAULT_BUNDLE_NAMESPACE);

    @BeforeEach
    public void setup() {
        k8SServiceClient = new K8SServiceClientTestDouble();
        jobRepository = Mockito.mock(DigitalExchangeJobRepository.class);
        installedComponentRepository = Mockito.mock(DigitalExchangeInstalledComponentRepository.class);
        service = new DigitalExchangeComponentsServiceImpl(k8SServiceClient, availableDigitalExchanges, jobRepository,
                installedComponentRepository);
    }

    @AfterEach
    public void teardown() {
        k8SServiceClient.cleanInMemoryDatabases();
    }

    @Test
    public void shouldReturnAllComponentsAvailable() {
        k8SServiceClient.addInMemoryBundle(TestEntitiesGenerator.getTestBundle());
        when(installedComponentRepository.findAll()).thenReturn(Collections.emptyList());
        List<DigitalExchangeComponent> bundles = service.getComponents();
        assertThat(bundles.size()).isEqualTo(1);
        assertThat(bundles.get(0).getDigitalExchangeName()).isEqualTo(DEFAULT_BUNDLE_NAMESPACE);
    }

    @Test
    public void shouldReturnInstalledComponents() {
        EntandoDeBundle bundle = TestEntitiesGenerator.getTestBundle();
        DigitalExchangeComponent component = DigitalExchangeComponent.newFrom(bundle);
        component.setInstalled(true);

        when(installedComponentRepository.findAll()).thenReturn(Collections.singletonList(component));
        List<DigitalExchangeComponent> components = service.getComponents();
        assertThat(components.size()).isEqualTo(1);
        assertThat(components.get(0).getComponentId()).isEqualTo(bundle.getMetadata().getName());

        verify(installedComponentRepository).findAll();
    }
}
