package org.entando.kubernetes.model.job;

import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;

import java.time.LocalDateTime;


public class JobTracker {

    EntandoBundleComponentJob job;
    EntandoBundleComponentJobRepository repo;

    public JobTracker(EntandoBundleComponentJob job, EntandoBundleComponentJobRepository repo) {
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

    public EntandoBundleComponentJob getJob() {
        return this.job;
    }

}