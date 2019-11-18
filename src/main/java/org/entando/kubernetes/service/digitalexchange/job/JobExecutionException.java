package org.entando.kubernetes.service.digitalexchange.job;

public class JobExecutionException extends RuntimeException {

    public JobExecutionException(final String message, final Throwable throwable) {
        super(message, throwable);
    }

    public JobExecutionException(final String message) {
        super(message);
    }

}
