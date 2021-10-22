package org.entando.kubernetes.exception;

import org.entando.kubernetes.exception.http.HttpException;
import org.springframework.http.HttpStatus;

public class EntandoClientDataException extends EntandoComponentManagerException implements HttpException {

    public EntandoClientDataException() {
        super();
    }

    public EntandoClientDataException(String message) {
        super(message);
    }

    public EntandoClientDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public EntandoClientDataException(Throwable cause) {
        super(cause);
    }

    protected EntandoClientDataException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
