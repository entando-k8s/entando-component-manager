package org.entando.kubernetes.exception;

public class EntandoGeneralException extends RuntimeException {

    public EntandoGeneralException() {
        super();
    }

    public EntandoGeneralException(String message) {
        super(message);
    }

    public EntandoGeneralException(String message, Throwable cause) {
        super(message, cause);
    }

    public EntandoGeneralException(Throwable cause) {
        super(cause);
    }

    protected EntandoGeneralException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
