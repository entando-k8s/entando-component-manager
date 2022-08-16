package org.entando.kubernetes.exception.web;

import org.springframework.http.HttpStatus;

public class ConflictExceptionWeb extends WebHttpException {

    public ConflictExceptionWeb(final String message) {
        this(message, null);
    }

    public ConflictExceptionWeb(final String message, final Throwable throwable) {
        super(HttpStatus.CONFLICT, message, throwable);
    }

}
