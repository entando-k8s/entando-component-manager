package org.entando.kubernetes.controller.digitalexchange.component;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.fabric8.kubernetes.client.KubernetesClientException;
import org.entando.kubernetes.TestEntitiesGenerator;
import org.entando.kubernetes.assertionhelper.SimpleRestResponseAssertionHelper;
import org.entando.kubernetes.model.bundle.EntandoBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.entando.kubernetes.service.digitalexchange.component.EntandoBundleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class EntandoBundleResourceControllerTest {

    private EntandoBundleResourceController controller;

    @Mock
    private EntandoBundleService bundleService;

    @BeforeEach
    public void setup() {
        controller = new EntandoBundleResourceController(bundleService, null);
    }

    @Test
    void shouldReturnTheExpectedResponseWhenSuccessfullyDeployedAnEntandoDeBundle() {
        // given that the user wants to deploy a new EntandoDeBundle and that the k8s service answer with an OK
        final EntandoDeBundle deBundle = TestEntitiesGenerator.getTestBundle();
        final EntandoBundle bundle = TestEntitiesGenerator.getTestEntandoBundle();
        when(bundleService.deployDeBundle(any())).thenReturn(bundle);

        // when the user sends the request
        final ResponseEntity<SimpleRestResponse<EntandoBundle>> response = controller.deployBundle(deBundle);

        // then the expected response in returned
        SimpleRestResponseAssertionHelper.assertOnSuccessfulResponse(response, HttpStatus.OK);
    }

    @Test
    void shouldReturnTheExpectedResponseWhenUnseccessfullyDeployedAnEntandoDeBundle() {

        // given that the user wants to deploy a new EntandoDeBundle and that the k8s service answer with a KO
        when(bundleService.deployDeBundle(any())).thenThrow(new KubernetesClientException("error"));
        final EntandoDeBundle entandoDeBundle = new EntandoDeBundle();

        // when the user sends the request
        // then a KubernetesClientException is thrown
        assertThrows(KubernetesClientException.class, () -> controller.deployBundle(entandoDeBundle));
    }

    void shouldReturnTheExpectedStatus() {
        fail();
    }
}
