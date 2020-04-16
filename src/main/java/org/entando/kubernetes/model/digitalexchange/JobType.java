package org.entando.kubernetes.model.digitalexchange;

import static org.entando.kubernetes.model.digitalexchange.JobStatus.INSTALL_COMPLETED;
import static org.entando.kubernetes.model.digitalexchange.JobStatus.INSTALL_CREATED;
import static org.entando.kubernetes.model.digitalexchange.JobStatus.INSTALL_ERROR;
import static org.entando.kubernetes.model.digitalexchange.JobStatus.INSTALL_IN_PROGRESS;
import static org.entando.kubernetes.model.digitalexchange.JobStatus.INSTALL_ROLLBACK;
import static org.entando.kubernetes.model.digitalexchange.JobStatus.UNINSTALL_COMPLETED;
import static org.entando.kubernetes.model.digitalexchange.JobStatus.UNINSTALL_CREATED;
import static org.entando.kubernetes.model.digitalexchange.JobStatus.UNINSTALL_ERROR;
import static org.entando.kubernetes.model.digitalexchange.JobStatus.UNINSTALL_IN_PROGRESS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum JobType {

    INSTALL(INSTALL_CREATED, INSTALL_IN_PROGRESS, INSTALL_ERROR, INSTALL_COMPLETED, INSTALL_ROLLBACK),
    UNINSTALL(UNINSTALL_IN_PROGRESS, UNINSTALL_ERROR, UNINSTALL_COMPLETED),
    UNFINISHED(INSTALL_CREATED, INSTALL_IN_PROGRESS, UNINSTALL_CREATED, UNINSTALL_IN_PROGRESS),
    FINISHED(INSTALL_ERROR, INSTALL_COMPLETED, UNINSTALL_ERROR, UNINSTALL_COMPLETED, INSTALL_ROLLBACK),
    SUCCESSFUL(INSTALL_COMPLETED, UNINSTALL_COMPLETED),
    ERROR(INSTALL_ERROR, UNINSTALL_ERROR);

    private List<JobStatus> statusList;

    JobType(JobStatus... statuses) {
        this.statusList = Arrays.asList(statuses);
    }

    public boolean matches(JobStatus status) {
        return this.statusList.contains(status);
    }

    public List<JobStatus> getStatusList() {
        return new ArrayList<>(this.statusList);
    }

}
