package org.entando.kubernetes.exception.web;

import org.springframework.http.HttpStatus;

public class InternalServerExceptionWeb extends WebHttpException {

    public InternalServerExceptionWeb(final String message) {
        this(message, null);
    }

    public InternalServerExceptionWeb(final String message, final Throwable throwable) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message, throwable);
    }
}
