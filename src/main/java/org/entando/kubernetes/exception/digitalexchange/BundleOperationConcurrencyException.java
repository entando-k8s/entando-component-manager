package org.entando.kubernetes.exception.digitalexchange;

import org.entando.kubernetes.exception.http.HttpException;
import org.entando.kubernetes.exception.web.WebHttpException;
import org.springframework.http.HttpStatus;

public class BundleOperationConcurrencyException extends WebHttpException implements HttpException {

    public BundleOperationConcurrencyException(final String message) {
        this(message, null);
    }

    public BundleOperationConcurrencyException(final String message, final Throwable throwable) {
        super(HttpStatus.CONFLICT, message, throwable);
    }
}
