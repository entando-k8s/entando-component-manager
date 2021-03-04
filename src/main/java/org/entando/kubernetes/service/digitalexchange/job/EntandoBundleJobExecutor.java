package org.entando.kubernetes.service.digitalexchange.job;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;

public interface EntandoBundleJobExecutor {

    String ENTANDO_CORE_MESSAGE_REGEX = "message\":\"([^\n\r]*)\"";
    String ENTANDO_K8S_SERVICE_MESSAGE_REGEX = "message\\=([^\n\r]*), metadata";

    default String getMeaningfulErrorMessage(Throwable th) {
        if (th.getCause() instanceof RestClientResponseException) {
            return getMeaningfulErrorMessage((RestClientResponseException) th.getCause());
        }
        return th.getMessage();
    }

    default String getMeaningfulErrorMessage(RestClientResponseException e) {
        HttpStatus status = HttpStatus.valueOf(e.getRawStatusCode());

        String message = String
                .format("Rest client exception (status code %d) - %s", e.getRawStatusCode(), status.getReasonPhrase());
        String respBody = e.getResponseBodyAsString();
        if (!respBody.isEmpty()) {
            String parsedErrorMessage = parseEntandoCoreError(respBody);
            // object comparison: if the returned string is the same => regex didn't match
            if (respBody == parsedErrorMessage) {   // NOSONAR
                parsedErrorMessage = parseEntandoK8SServiceError(respBody);
            }
            message = message + " - " + parsedErrorMessage;
        }
        return message;
    }

    /**
     * parse the error message from an entando core error response and return it.
     * @param responseBody the response body received by entando core
     * @return the parsed error message or the received responseBody
     */
    private String parseEntandoCoreError(String responseBody) {
        Matcher matcher = Pattern.compile(ENTANDO_CORE_MESSAGE_REGEX).matcher(responseBody);
        return matcher.find() ? matcher.group(1) : responseBody;
    }

    /**
     * parse the error message from an entando-k8s-service error response and return it.
     * @param responseBody the response body received by entando-k8s-service
     * @return the parsed error message or the received responseBody
     */
    private String parseEntandoK8SServiceError(String responseBody) {
        Matcher matcher = Pattern.compile(ENTANDO_K8S_SERVICE_MESSAGE_REGEX).matcher(responseBody);
        return matcher.find() ? matcher.group(1) : responseBody;
    }
}
