package org.entando.kubernetes.service.digitalexchange.job;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientResponseException;

@Tag("unit")
class EntandoBundleJobExecutorTest {

    private EntandoBundleJobExecutor entandoBundleJobExecutor = new EntandoBundleJobExecutor() {
    };


    @Test
    void shouldParseEntandoCoreErrorMessage() {
        String errorMessage = "[{\"payload\":[],\"metaData\":[],\"errors\":[{\"code\":\"1\",\"message\":\"The Widget "
                + "conference-table-widget already exists\"}]}]";
        RestClientResponseException exception = new RestClientResponseException(errorMessage, 409, "Conflict", null,
                errorMessage.getBytes(), null);

        String parsedMessage = entandoBundleJobExecutor.getMeaningfulErrorMessage(exception);
        assertThat(parsedMessage).isEqualTo(
                "Rest client exception (status code 409) - Conflict - The Widget conference-table-widget already exists");
    }

    @Test
    void shouldParseEntandoK8SServiceErrorMessage() {
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

        String parsedMessage = entandoBundleJobExecutor.getMeaningfulErrorMessage(exception);
        assertThat(parsedMessage).isEqualTo("Rest client exception (status code 409) - Conflict - "
                + "entandoapppluginlinks.entando.org \"quickstart-lcorsettientando-xmasbundle-link\" already exists");
    }

    @Test
    void shouldReturnExceptionMessageIfExceptionIsNOTInstanceOfRestClientResponseException() {

        String error = "Simple error";
        Exception exception = new Exception(error);

        String parsedMessage = entandoBundleJobExecutor.getMeaningfulErrorMessage(exception);
        assertThat(parsedMessage).isEqualTo(error);
    }
}
