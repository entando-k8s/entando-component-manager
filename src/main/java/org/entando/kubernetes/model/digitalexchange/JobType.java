package org.entando.kubernetes.model.digitalexchange;

import java.util.EnumSet;
import java.util.Set;

import static org.entando.kubernetes.model.digitalexchange.JobStatus.*;

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
