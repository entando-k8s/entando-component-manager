package org.entando.kubernetes.service.digitalexchange.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.FragmentDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientResponseException;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class EntandoBundleJobExecutorTest {

    private final ComponentType componentType = ComponentType.CONTENT_TYPE;
    private final String componentCode = "BRN";
    @Mock
    private Installable<FragmentDescriptor> installable;
    private EntandoBundleJobExecutor entandoBundleJobExecutor = new EntandoBundleJobExecutor() {
    };

    @Test
    void shouldParseEntandoCoreErrorMessage() {
        when(installable.getComponentType()).thenReturn(componentType);
        when(installable.getName()).thenReturn(componentCode);

        String errorMessage = "[{\"payload\":[],\"metaData\":[],\"errors\":[{\"code\":\"1\",\"message\":\"The Widget "
                + "conference-table-widget already exists\"}]}]";
        RestClientResponseException exception = new RestClientResponseException(errorMessage, 409, "Conflict", null,
                errorMessage.getBytes(), null);

        String parsedMessage = entandoBundleJobExecutor.getMeaningfulErrorMessage(exception, installable);
        assertThat(parsedMessage).isEqualTo(
                "ComponentType: " + componentType.getTypeName() + " - Code: " + componentCode
                        + " --- Rest client exception (status code 409) - Conflict - The Widget conference-table-widget already exists");
    }

    @Test
    void shouldParseEntandoK8SServiceErrorMessage() {
        when(installable.getComponentType()).thenReturn(componentType);
        when(installable.getName()).thenReturn(componentCode);

        String errorMessage = "io.fabric8.kubernetes.client.KubernetesClientException: An error occurred while linking "
                + "app quickstart to plugin lcorsettientando-xmasbundle: 500 - {\"title\":\"Internal Server Error\","
                + "\"status\":500,\"detail\":\"Failure executing: POST at: "
                + "https://10.43.0.1/apis/entando.org/v1/namespaces/fire/entandoapppluginlinks. "
                + "Message: entandoapppluginlinks.entando.org \"quickstart-lcorsettientando-xmasbundle-link\" "
                + "already exists. Received status: Status(apiVersion=v1, code=409, details=StatusDetails(causes=[], "
                + "group=entando.org, kind=entandoapppluginlinks, name=quickstart-lcorsettientando-xmasbundle-link, "
                + "retryAfterSeconds=null, uid=null, additionalProperties={}), kind=Status, "
                + "message=entandoapppluginlinks.entando.org \"quickstart-lcorsettientando-xmasbundle-link\" "
                + "already exists, metadata=ListMeta(_continue=null, remainingItemCount=null, resourceVersion=null, "
                + "selfLink=null, additionalProperties={}), reason=AlreadyExists, status=Failure, "
                + "additionalProperties={}).\"}";
        RestClientResponseException exception = new RestClientResponseException(errorMessage, 409, "Conflict", null,
                errorMessage.getBytes(), null);

        String parsedMessage = entandoBundleJobExecutor.getMeaningfulErrorMessage(exception, installable);
        assertThat(parsedMessage).isEqualTo(
                "ComponentType: " + componentType.getTypeName() + " - Code: " + componentCode
                        + " --- Rest client exception (status code 409) - Conflict - "
                        + "entandoapppluginlinks.entando.org \"quickstart-lcorsettientando-xmasbundle-link\" already exists");
    }

    @Test
    void shouldReturnExceptionMessageIfExceptionIsNOTInstanceOfRestClientResponseException() {

        String error = "Simple error";
        Exception exception = new Exception(error);

        String parsedMessage = entandoBundleJobExecutor.getMeaningfulErrorMessage(exception, installable);
        assertThat(parsedMessage).isEqualTo(error);
    }
}
