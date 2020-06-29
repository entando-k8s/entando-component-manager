package org.entando.kubernetes.service.digitalexchange.job;

import org.springframework.web.client.HttpClientErrorException;

public interface EntandoBundleJobExecutor {

    default String getMeaningfulErrorMessage(Throwable th) {
        String message = th.getMessage();
        if (th.getCause() != null) {
            message = th.getCause().getMessage();
            if (th.getCause() instanceof HttpClientErrorException) {
                HttpClientErrorException httpException = (HttpClientErrorException) th.getCause();
                message =
                        httpException.getMessage() + "\n" + httpException.getResponseBodyAsString();
            }
        }
        return message;
    }

}
