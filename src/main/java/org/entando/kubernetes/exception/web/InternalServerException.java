package org.entando.kubernetes.exception.web;

import org.springframework.http.HttpStatus;

public class InternalServerException extends HttpException {

    public InternalServerException(final String message) {
        this(message, null);
    }

    public InternalServerException(final String message, final Throwable throwable) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message, throwable);
    }
}
