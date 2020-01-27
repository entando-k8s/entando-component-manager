package org.entando.kubernetes.model.web.exception;

public class BadResponseException extends InternalServerException {

    public BadResponseException() {
        super("org.entando.error.badFormatResponse");
    }

}
