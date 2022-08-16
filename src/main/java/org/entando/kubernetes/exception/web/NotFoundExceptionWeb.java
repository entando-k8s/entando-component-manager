package org.entando.kubernetes.exception.web;

import org.springframework.http.HttpStatus;

public class NotFoundExceptionWeb extends WebHttpException {

    public NotFoundExceptionWeb(final String message) {
        this(message, null);
    }

    public NotFoundExceptionWeb(final String message, final Throwable throwable) {
        super(HttpStatus.NOT_FOUND, message, throwable);
    }

}
