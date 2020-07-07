package org.entando.kubernetes.model.job;

import static org.entando.kubernetes.model.job.JobStatus.INSTALL_COMPLETED;
import static org.entando.kubernetes.model.job.JobStatus.INSTALL_CREATED;
import static org.entando.kubernetes.model.job.JobStatus.INSTALL_ERROR;
import static org.entando.kubernetes.model.job.JobStatus.INSTALL_IN_PROGRESS;
import static org.entando.kubernetes.model.job.JobStatus.INSTALL_ROLLBACK;
import static org.entando.kubernetes.model.job.JobStatus.UNINSTALL_COMPLETED;
import static org.entando.kubernetes.model.job.JobStatus.UNINSTALL_CREATED;
import static org.entando.kubernetes.model.job.JobStatus.UNINSTALL_ERROR;
import static org.entando.kubernetes.model.job.JobStatus.UNINSTALL_IN_PROGRESS;

import java.util.EnumSet;
import java.util.Set;

public enum JobType {

    INSTALL(INSTALL_CREATED, INSTALL_IN_PROGRESS, INSTALL_ERROR, INSTALL_COMPLETED, INSTALL_ROLLBACK),
    UNINSTALL(UNINSTALL_IN_PROGRESS, UNINSTALL_ERROR, UNINSTALL_COMPLETED),
    UNFINISHED(INSTALL_CREATED, INSTALL_IN_PROGRESS, UNINSTALL_CREATED, UNINSTALL_IN_PROGRESS),
    FINISHED(INSTALL_ERROR, INSTALL_COMPLETED, UNINSTALL_ERROR, UNINSTALL_COMPLETED, INSTALL_ROLLBACK),
    SUCCESSFUL(INSTALL_COMPLETED, UNINSTALL_COMPLETED),
    ERROR(INSTALL_ERROR, UNINSTALL_ERROR);

    private Set<JobStatus> statusSet;

    JobType(JobStatus status, JobStatus... others) {
        this.statusSet = EnumSet.of(status, others);
    }

    public boolean matches(JobStatus status) {
        return this.statusSet.contains(status);
    }

    public Set<JobStatus> getStatuses() {
        return this.statusSet;
    }

}
