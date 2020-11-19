package org.entando.kubernetes.service.digitalexchange.job;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;

public interface EntandoBundleJobExecutor {

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
            message = message + "\n" + respBody;
        }
        return message;

    }

}
