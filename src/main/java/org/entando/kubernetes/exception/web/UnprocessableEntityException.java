package org.entando.kubernetes.exception.web;

import org.springframework.http.HttpStatus;

public class UnprocessableEntityException extends WebHttpException {

    public UnprocessableEntityException(final String message) {
        this(message, null);
    }

    public UnprocessableEntityException(final String message, final Throwable throwable) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message, throwable);
    }

}
