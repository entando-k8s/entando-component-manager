package org.entando.kubernetes.model.job;

import java.time.LocalDateTime;

public interface TrackableJob {

    LocalDateTime getStartedAt();
    void setStartedAt(LocalDateTime ldt);
    LocalDateTime getFinishedAt();
    void setFinishedAt(LocalDateTime ldt);
    JobStatus getStatus();
    void setStatus(JobStatus js);
    String getErrorMessage();
    void setErrorMessage(String message);
}
