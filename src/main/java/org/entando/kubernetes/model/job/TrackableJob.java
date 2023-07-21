package org.entando.kubernetes.model.job;

import java.time.LocalDateTime;

public interface TrackableJob {

    LocalDateTime getStartedAt();

    void setStartedAt(LocalDateTime ldt);

    LocalDateTime getFinishedAt();

    void setFinishedAt(LocalDateTime ldt);

    JobStatus getStatus();

    void setStatus(JobStatus js);

    Integer getInstallErrorCode();

    void setInstallErrorCode(Integer code);

    String getInstallErrorMessage();

    void setInstallErrorMessage(String message);

    Integer getUninstallErrorCode();

    void setUninstallErrorCode(Integer code);

    String getUninstallErrorMessage();

    void setUninstallErrorMessage(String message);

    String getUninstallErrors();

    void setUninstallErrors(String message);

    String getRollbackErrorMessage();

    void setRollbackErrorMessage(String message);

    Integer getRollbackErrorCode();

    void setRollbackErrorCode(Integer code);

}
