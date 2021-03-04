package org.entando.kubernetes.exception;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class EntandoComponentManagerException extends RuntimeException {

    @Getter
    @Setter
    @Accessors(chain = true)
    private int errorCode = EntandoBundleJobErrors.GENERIC.getCode();

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
