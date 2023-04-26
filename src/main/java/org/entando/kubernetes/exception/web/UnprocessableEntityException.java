package org.entando.kubernetes.exception.web;

import org.entando.kubernetes.exception.http.HttpException;
import org.springframework.http.HttpStatus;

public class UnprocessableEntityException extends WebHttpException implements HttpException {

    public UnprocessableEntityException(final String message) {
        this(message, null);
    }

    public UnprocessableEntityException(final String message, final Throwable throwable) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message, throwable);
    }

}
