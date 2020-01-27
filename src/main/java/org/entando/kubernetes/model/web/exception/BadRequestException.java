package org.entando.kubernetes.model.web.exception;

public class BadRequestException extends IllegalArgumentException {

    public BadRequestException(String message, Throwable e) {
        super(message, e);
    }

    public BadRequestException(Throwable e) {
        this("org.entando.error.badFormatRequest", e);
    }

    public BadRequestException() {
        this((Throwable) null);
    }

    public BadRequestException(String message) {
        this(message, null);
    }

}
