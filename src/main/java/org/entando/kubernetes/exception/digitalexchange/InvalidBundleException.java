package org.entando.kubernetes.exception.digitalexchange;

import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.http.HttpException;
import org.springframework.http.HttpStatus;

public class InvalidBundleException extends EntandoComponentManagerException
        implements HttpException {

    public InvalidBundleException() {
        super();
    }

    public InvalidBundleException(String message) {
        super(message);
    }

    public InvalidBundleException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidBundleException(Throwable cause) {
        super(cause);
    }

    protected InvalidBundleException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
