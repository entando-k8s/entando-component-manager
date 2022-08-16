package org.entando.kubernetes.exception.web;

import org.springframework.http.HttpStatus;

public class ConflictException extends WebHttpException {

    public ConflictException(final String message) {
        this(message, null);
    }

    public ConflictException(final String message, final Throwable throwable) {
        super(HttpStatus.CONFLICT, message, throwable);
    }

}
