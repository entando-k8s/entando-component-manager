package org.entando.kubernetes.exception;

import lombok.Getter;

public class EntandoComponentManagerException extends RuntimeException {

    @Getter
    private final int errorCode;

    public EntandoComponentManagerException() {
        this(EntandoBundleJobErrors.GENERIC.getCode());
    }

    public EntandoComponentManagerException(String message) {
        this(message, EntandoBundleJobErrors.GENERIC.getCode());
    }

    public EntandoComponentManagerException(String message, Throwable cause) {
        this(message, cause, EntandoBundleJobErrors.GENERIC.getCode());
    }

    public EntandoComponentManagerException(Throwable cause) {
        this(cause, EntandoBundleJobErrors.GENERIC.getCode());
    }

    protected EntandoComponentManagerException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        this(message, cause, enableSuppression, writableStackTrace, EntandoBundleJobErrors.GENERIC.getCode());
    }

    public EntandoComponentManagerException(int errorCode) {
        super();
        this.errorCode = errorCode;
    }

    public EntandoComponentManagerException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public EntandoComponentManagerException(String message, Throwable cause, int errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public EntandoComponentManagerException(Throwable cause, int errorCode) {
        super(cause);
        this.errorCode = errorCode;
    }

    protected EntandoComponentManagerException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace, int errorCode) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.errorCode = errorCode;
    }
}
