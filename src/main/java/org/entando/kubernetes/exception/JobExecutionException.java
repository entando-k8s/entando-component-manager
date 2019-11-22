package org.entando.kubernetes.exception;

public class JobExecutionException extends RuntimeException {

    public JobExecutionException(final String message, final Throwable throwable) {
        super(message, throwable);
    }

    public JobExecutionException(final String message) {
        super(message);
    }

}
