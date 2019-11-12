package org.entando.kubernetes.service.digitalexchange.job;

import java.nio.file.Path;

public class JobExecutionException extends RuntimeException {

    private Path jobAssociatedTempPath;

    public JobExecutionException(String message, Throwable throwable, Path jobAssociatedTempPath) {
        super(message, throwable);
        this.jobAssociatedTempPath = jobAssociatedTempPath;
    }

    public JobExecutionException(final String message, final Throwable throwable) {
        this(message, throwable, null);
    }

    public JobExecutionException(final String message) {
        this(message, null, null);
    }

    public Path getJobAssociatedTempPath() {
        return jobAssociatedTempPath;
    }
}
