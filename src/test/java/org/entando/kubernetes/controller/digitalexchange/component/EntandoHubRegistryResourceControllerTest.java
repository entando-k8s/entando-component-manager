package org.entando.kubernetes.controller.digitalexchange.component;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.List;
import org.entando.kubernetes.model.entandohub.EntandoHubRegistry;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.entando.kubernetes.service.digitalexchange.entandohub.EntandoHubRegistryService;
import org.entando.kubernetes.stubhelper.EntandoHubRegistryStubHelper;
import org.entando.kubernetes.validator.EntandoHubRegistryValidator;
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
class EntandoHubRegistryResourceControllerTest {

    @Mock
    private EntandoHubRegistryService service;
    @Mock
    private EntandoHubRegistryValidator validator;

    private EntandoHubRegistryResourceController controller;

    @BeforeEach
    public void setup() {
        controller = new EntandoHubRegistryResourceController(service, validator);
    }

    @Test
    void shouldReturnAListOfRegistriesWhenRequestedInTheExpectedSimpleRestResponse() {

        when(service.listRegistries()).thenReturn(EntandoHubRegistryStubHelper.stubListOfEntandoHubRegistry());
        final ResponseEntity<SimpleRestResponse<List<EntandoHubRegistry>>> response = controller.getRegistries();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getErrors()).hasSize(0);
        assertThat(response.getBody().getMetaData()).isNull();

        final List<EntandoHubRegistry> payload = response.getBody().getPayload();
        assertThat(payload).hasSize(2);
        assertThat(payload.get(0)).isEqualToComparingFieldByField(EntandoHubRegistryStubHelper.stubEntandoHubRegistry1());
        assertThat(payload.get(1)).isEqualToComparingFieldByField(EntandoHubRegistryStubHelper.stubEntandoHubRegistry2());
    }

    @Test
    void shouldReturnTheExpectedSimpleRestResponseOnAddANewRegistry() {

        EntandoHubRegistry registryToAdd = EntandoHubRegistryStubHelper.stubEntandoHubRegistry1();
        registryToAdd.setId(null);  // useless in this testcase because the validator is a mock

        when(service.createRegistry(any())).thenReturn(EntandoHubRegistryStubHelper.stubEntandoHubRegistry1());
        final ResponseEntity<SimpleRestResponse<EntandoHubRegistry>> response = controller.addRegistry(registryToAdd);
        assertOnCreateOrUpdateResponse(response, HttpStatus.CREATED);
    }

    @Test
    void shouldReturnTheExpectedSimpleRestResponseOnUpdateAnExistingRegistry() {

        EntandoHubRegistry registryToUpdate = EntandoHubRegistryStubHelper.stubEntandoHubRegistry1();

        when(service.updateRegistry(any())).thenReturn(registryToUpdate);
        final ResponseEntity<SimpleRestResponse<EntandoHubRegistry>> response = controller.updateRegistry(registryToUpdate);
        assertOnCreateOrUpdateResponse(response, HttpStatus.OK);
    }

    @Test
    void shouldReturnTheExpectedSimpleRestResponseOnDeleteEntandoHubRegistry() {

        doNothing().when(service).deleteRegistry(anyString());
        final ResponseEntity<Void> response = controller.deleteRegistry("myid");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    private void assertOnCreateOrUpdateResponse(ResponseEntity<SimpleRestResponse<EntandoHubRegistry>> response, HttpStatus httpStatus) {
        assertThat(response.getStatusCode()).isEqualTo(httpStatus);
        assertThat(response.getBody().getErrors()).hasSize(0);
        assertThat(response.getBody().getMetaData()).isNull();

        final EntandoHubRegistry payload = response.getBody().getPayload();
        assertThat(payload).isEqualToComparingFieldByField(EntandoHubRegistryStubHelper.stubEntandoHubRegistry1());
    }
}
