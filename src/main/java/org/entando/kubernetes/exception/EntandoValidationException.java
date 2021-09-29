package org.entando.kubernetes.exception;

public class EntandoValidationException extends EntandoComponentManagerException {

    public EntandoValidationException() {
        super();
    }

    public EntandoValidationException(String message) {
        super(message);
    }

    public EntandoValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public EntandoValidationException(Throwable cause) {
        super(cause);
    }

    protected EntandoValidationException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
