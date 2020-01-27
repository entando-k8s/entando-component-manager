package org.entando.kubernetes.model.web.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends HttpException {

    public ConflictException(final String message) {
        this(message, null);
    }

    public ConflictException(final String message, final Throwable throwable) {
        super(HttpStatus.CONFLICT, message, throwable);
    }

}
