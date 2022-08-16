package org.entando.kubernetes.exception.web;

import org.springframework.http.HttpStatus;

public class UnprocessableEntityExceptionWeb extends WebHttpException {

    public UnprocessableEntityExceptionWeb(final String message) {
        this(message, null);
    }

    public UnprocessableEntityExceptionWeb(final String message, final Throwable throwable) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message, throwable);
    }

}
