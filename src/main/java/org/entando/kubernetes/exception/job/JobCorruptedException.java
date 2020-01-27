package org.entando.kubernetes.exception.job;

import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.http.HttpException;
import org.springframework.http.HttpStatus;

public class JobCorruptedException extends EntandoComponentManagerException implements HttpException {

    public JobCorruptedException() {
        super();
    }

    public JobCorruptedException(String message) {
        super(message);
    }

    public JobCorruptedException(String message, Throwable cause) {
        super(message, cause);
    }

    public JobCorruptedException(Throwable cause) {
        super(cause);
    }

    protected JobCorruptedException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
