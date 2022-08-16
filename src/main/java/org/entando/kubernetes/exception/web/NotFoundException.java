package org.entando.kubernetes.exception.web;

import org.springframework.http.HttpStatus;

public class NotFoundException extends WebHttpException {

    public NotFoundException(final String message) {
        this(message, null);
    }

    public NotFoundException(final String message, final Throwable throwable) {
        super(HttpStatus.NOT_FOUND, message, throwable);
    }

}
