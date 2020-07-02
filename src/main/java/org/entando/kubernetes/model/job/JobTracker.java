package org.entando.kubernetes.model.job;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;


public class JobTracker<T extends TrackableJob> {

    T job;
    JpaRepository<T, UUID> repo;

    public JobTracker(T job, JpaRepository<T, UUID> repo) {
        this.job = job;
        this.repo = repo;
    }

    public void startTracking(JobStatus js) {
        this.job.setStatus(js);
        this.job.setStartedAt(LocalDateTime.now());
        repo.save(this.job);
    }

    public void stopTrackingTime(JobResult result) {
        this.job.setStatus(result.getStatus());
        this.job.setFinishedAt(LocalDateTime.now());
        if (result.hasException()) {
            this.job.setErrorMessage(result.getErrorMessage());
        }
        repo.save(this.job);
    }

    public T getJob() {
        return this.job;
    }

}