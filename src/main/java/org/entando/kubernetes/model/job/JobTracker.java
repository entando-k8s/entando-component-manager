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
        this.job.setProgress(0.0);
        this.job.setStartedAt(LocalDateTime.now());
        this.job = updateJob(this.job);
    }

    public void incrementProgress(double increment) {
        double currentProgress = this.getJob().getProgress();
        double newProgress = Math.floor((currentProgress + increment) * 100) / 100;
        this.job.setProgress(newProgress);
        this.job = updateJob(this.job);
    }

    public void setProgress(double progress) {
        this.job.setProgress(progress);
        this.job = updateJob(this.job);
    }

    public void stopTrackingTime(JobResult result) {
        this.job.setStatus(result.getStatus());
        this.job.setFinishedAt(LocalDateTime.now());
        if (result.hasException()) {
            this.job.setErrorMessage(result.getErrorMessage());
        }
        this.job = updateJob(this.job);
    }

    public T getJob() {
        return this.job;
    }

    private T updateJob(T job) {
        T updatedJob = repo.save(job);
        if (job instanceof HasInstallable) {
            ((HasInstallable) updatedJob).setInstallable(((HasInstallable) job).getInstallable());
        }
        return updatedJob;
    }

}