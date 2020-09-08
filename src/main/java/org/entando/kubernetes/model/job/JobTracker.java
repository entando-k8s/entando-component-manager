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
        this.setProgress(this.job, 0.0);
        this.job.setStartedAt(LocalDateTime.now());
        this.job = updateJob(this.job);
    }

    public void incrementProgress(double increment) {
        this.job = this.incrementProgress(this.job, increment);
        this.job = updateJob(this.job);
    }

    public void setProgress(double progress) {
        this.setProgress(this.job, progress);
        this.job = updateJob(this.job);
    }

    public void finishTracking(JobResult result) {
        this.job.setStatus(result.getStatus());
        this.job.setFinishedAt(LocalDateTime.now());
        if (result.hasException()) {
            this.job.setErrorMessage(result.getErrorMessage());
        }
        if (result.getProgress() != null) {
            this.setProgress(result.getProgress());
        }
        this.job = updateJob(this.job);
    }

    public T getJob() {
        return this.job;
    }

    private T incrementProgress(T job, double progressIncrement) {
        if (job instanceof HasProgress) {
            HasProgress j = (HasProgress) job;
            double lastProgress = j.getProgress();
            j.setProgress(roundProgress(lastProgress + progressIncrement));
        }
        return job;
    }

    private T setProgress(T job, double progress) {
       if (job instanceof HasProgress)  {
           ((HasProgress) job).setProgress(roundProgress(progress));
       }
       return job;
    }

    private T updateJob(T job) {
        T updatedJob = repo.save(job);
        if (job instanceof HasInstallable) {
            ((HasInstallable) updatedJob).setInstallable(((HasInstallable) job).getInstallable());
        }
        return updatedJob;
    }

    private double roundProgress(double progress) {
        return Math.max(0.0, Math.min(1.0, Math.floor(progress * 100) / 100));
    }

}