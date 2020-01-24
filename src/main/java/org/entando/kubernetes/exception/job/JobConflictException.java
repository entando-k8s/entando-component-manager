package org.entando.kubernetes.exception.job;

import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.http.HttpException;
import org.springframework.http.HttpStatus;

public class JobConflictException extends EntandoComponentManagerException implements HttpException {

    public JobConflictException() {
        super();
    }

    public JobConflictException(String message) {
        super(message);
    }

    public JobConflictException(String message, Throwable cause) {
        super(message, cause);
    }

    public JobConflictException(Throwable cause) {
        super(cause);
    }

    protected JobConflictException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
