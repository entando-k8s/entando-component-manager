package org.entando.kubernetes.model.digitalexchange;

import org.entando.kubernetes.service.digitalexchange.installable.Installable;

public class InstallableInstallResult {

    private Installable installable;
    private JobStatus jobStatus;
    private Exception exception;

    public InstallableInstallResult(Installable installable, JobStatus jobStatus, Exception exception) {
        this.installable = installable;
        this.jobStatus = jobStatus;
        this.exception = exception;
    }

    public InstallableInstallResult(Installable installable, JobStatus jobStatus) {
        this(installable, jobStatus, null);
    }

    public Installable getInstallable() {
        return installable;
    }

    public JobStatus getJobStatus() {
        return jobStatus;
    }

    public Exception getException() {
        return exception;
    }

    public boolean hasException() {
        return this.exception != null;
    }
}
