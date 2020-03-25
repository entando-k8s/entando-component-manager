package org.entando.kubernetes.service.digitalexchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.entando.kubernetes.TestEntitiesGenerator.DEFAULT_BUNDLE_NAMESPACE;

import java.util.Collections;
import java.util.List;
import org.entando.kubernetes.TestEntitiesGenerator;
import org.entando.kubernetes.client.K8SServiceClientTestDouble;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeComponent;
import org.entando.kubernetes.repository.DigitalExchangeInstalledComponentRepository;
import org.entando.kubernetes.repository.DigitalExchangeJobRepository;
import org.entando.kubernetes.service.digitalexchange.component.DigitalExchangeComponentsService;
import org.entando.kubernetes.service.digitalexchange.component.DigitalExchangeComponentsServiceImpl;
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


    @Test
    public void shouldReturnAllComponentsAvailable() {
        k8SServiceClient.addInMemoryBundle(TestEntitiesGenerator.getTestBundle());
        List<DigitalExchangeComponent> bundles = service.getComponents();
        assertThat(bundles.size()).isEqualTo(1);
        assertThat(bundles.get(0).getDigitalExchangeName()).isEqualTo(DEFAULT_BUNDLE_NAMESPACE);
    }

}
