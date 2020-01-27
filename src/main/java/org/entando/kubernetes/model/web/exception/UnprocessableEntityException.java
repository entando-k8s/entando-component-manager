package org.entando.kubernetes.model.web.exception;

import org.springframework.http.HttpStatus;

public class UnprocessableEntityException extends HttpException {

    public UnprocessableEntityException(final String message) {
        this(message, null);
    }

    public UnprocessableEntityException(final String message, final Throwable throwable) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message, throwable);
    }

}
