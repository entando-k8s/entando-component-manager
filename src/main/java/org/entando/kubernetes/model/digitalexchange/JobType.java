package org.entando.kubernetes.model.digitalexchange;

import static org.entando.kubernetes.model.digitalexchange.JobStatus.*;

import java.util.Arrays;
import java.util.List;

public enum JobType {

    INSTALL(INSTALL_CREATED, INSTALL_IN_PROGRESS, INSTALL_ERROR, INSTALL_COMPLETED),
    UNINSTALL(UNINSTALL_IN_PROGRESS, UNINSTALL_ERROR, UNINSTALL_COMPLETED),
    UNFINISHED(INSTALL_CREATED, INSTALL_IN_PROGRESS, UNINSTALL_CREATED, UNINSTALL_IN_PROGRESS),
    FINISHED(INSTALL_ERROR, INSTALL_COMPLETED, UNINSTALL_ERROR, UNINSTALL_COMPLETED),
    SUCCESSFUL(INSTALL_COMPLETED, UNINSTALL_COMPLETED),
    ERROR(INSTALL_ERROR, UNINSTALL_ERROR);

    private List<JobStatus> statusList;

    private JobType(JobStatus... statuses) {
        this.statusList = Arrays.asList(statuses);
    }

    private boolean isOfType(JobStatus status) {
        return this.statusList.contains(status);
    }

    public static boolean isOfType(JobStatus status, JobType type){
        return type.isOfType(status);
    }

}
