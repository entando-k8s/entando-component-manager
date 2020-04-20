package org.entando.kubernetes.exception;

public class EntandoGeneralSignatureException extends RuntimeException {

    public EntandoGeneralSignatureException() {
        super();
    }

    public EntandoGeneralSignatureException(String message) {
        super(message);
    }

    public EntandoGeneralSignatureException(String message, Throwable cause) {
        super(message, cause);
    }

    public EntandoGeneralSignatureException(Throwable cause) {
        super(cause);
    }

    protected EntandoGeneralSignatureException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
