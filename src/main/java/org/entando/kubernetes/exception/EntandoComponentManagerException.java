package org.entando.kubernetes.exception;

public class EntandoComponentManagerException extends RuntimeException{

    public EntandoComponentManagerException() {
        super();
    }

    public EntandoComponentManagerException(String message) {
        super(message);
    }

    public EntandoComponentManagerException(String message, Throwable cause) {
        super(message, cause);
    }

    public EntandoComponentManagerException(Throwable cause) {
        super(cause);
    }

    protected EntandoComponentManagerException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
