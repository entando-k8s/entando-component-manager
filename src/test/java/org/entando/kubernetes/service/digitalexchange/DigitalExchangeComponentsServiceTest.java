package org.entando.kubernetes.service.digitalexchange;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import org.entando.kubernetes.client.K8SServiceClientTestDouble;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeComponent;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleBuilder;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpec;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpecBuilder;
import org.entando.kubernetes.repository.DigitalExchangeJobRepository;
import org.entando.kubernetes.service.digitalexchange.component.DigitalExchangeComponentsService;
import org.entando.kubernetes.service.digitalexchange.component.DigitalExchangeComponentsServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Tag;
import org.mockito.Mockito;

@Tag("unit")
public class DigitalExchangeComponentsServiceTest {

    private static final String DEFAULT_BUNDLE_NAMESPACE = "entando-de-bundles" ;
    private K8SServiceClientTestDouble k8SServiceClient;
    private DigitalExchangeComponentsService service;
    private DigitalExchangeJobRepository jobRepository;

    private List<String> availableDigitalExchanges = Collections.singletonList(DEFAULT_BUNDLE_NAMESPACE);
    @Before
    public void setup() {
        k8SServiceClient = new K8SServiceClientTestDouble();
        jobRepository = Mockito.mock(DigitalExchangeJobRepository.class);
        service = new DigitalExchangeComponentsServiceImpl(k8SServiceClient, availableDigitalExchanges, jobRepository);
    }


    @Test
    public void shouldReturnAllComponentsAvailable() {
        k8SServiceClient.addInMemoryBundle(getTestBundle());
        List<DigitalExchangeComponent> bundles = service.getComponents();
        assertThat(bundles.size()).isEqualTo(1);
        assertThat(bundles.get(0).getDigitalExchangeName()).isEqualTo(DEFAULT_BUNDLE_NAMESPACE);
    }

    private EntandoDeBundle getTestBundle() {
        return new EntandoDeBundleBuilder()
                .withNewMetadata()
                    .withName("my-bundle")
                    .withNamespace(DEFAULT_BUNDLE_NAMESPACE)
                .endMetadata()
                .withSpec(getTestEntandoDeBundleSpec()).build();

    }

    private EntandoDeBundleSpec getTestEntandoDeBundleSpec() {
        return new EntandoDeBundleSpecBuilder()
                .withNewDetails()
                .withDescription("A bundle containing some demo components for Entando6")
                .withName("inail_bundle")
                .addNewVersion("0.0.1")
                .addNewKeyword("entando6")
                .addNewKeyword("digital-exchange")
                .addNewDistTag("latest", "0.0.1")
                .and()
                .addNewTag()
                .withVersion("0.0.1")
                .withIntegrity("sha512-n4TEroSqg/sZlEGg2xj6RKNtl/t3ZROYdNd99/dl3UrzCUHvBrBxZ1rxQg/sl3kmIYgn3+ogbIFmUZYKWxG3Ag==")
                .withShasum("4d80130d7d651176953b5ce470c3a6f297a70815")
                .withTarball("http://localhost:8081/repository/npm-internal/inail_bundle/-/inail_bundle-0.0.1.tgz")
                .endTag()
                .build();
    }
}
