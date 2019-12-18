package org.entando.kubernetes.exception.job;

import org.entando.kubernetes.exception.EntandoComponentManagerException;

public class JobExecutionException extends EntandoComponentManagerException {

    public JobExecutionException(final String message, final Throwable throwable) {
        super(message, throwable);
    }

    public JobExecutionException(final String message) {
        super(message);
    }

}
