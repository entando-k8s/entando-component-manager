package org.entando.kubernetes.exception.web;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class HttpException extends RuntimeException {

    private final HttpStatus status;
    private final transient Object[] args;

    public HttpException(final HttpStatus status, final String message) {
        super(message);
        this.status = status;
        this.args = null;
    }

    public HttpException(final HttpStatus status, final String message, final Object[] args) {
        super(message);
        this.status = status;
        this.args = args;
    }

    public HttpException(final HttpStatus status, final String message, final Throwable throwable) {
        super(message, throwable);
        this.status = status;
        this.args = null;
    }

}
