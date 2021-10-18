package org.entando.kubernetes.exception;

import org.entando.kubernetes.exception.http.HttpException;
import org.springframework.http.HttpStatus;

public class EntandoValidationException extends EntandoComponentManagerException implements HttpException {

    public EntandoValidationException() {
        super();
    }

    public EntandoValidationException(String message) {
        super(message);
    }

    public EntandoValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public EntandoValidationException(Throwable cause) {
        super(cause);
    }

    protected EntandoValidationException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
