package org.entando.kubernetes.model.job;

import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;

import java.time.LocalDateTime;


public class JobTracker {

    EntandoBundleComponentJob job;
    Installable installable;
    EntandoBundleComponentJobRepository repo;

    public JobTracker(Installable installable, EntandoBundleJob parentJob, EntandoBundleComponentJobRepository jobRepository) {
        if(installable.getJob() == null) {
            this.job = EntandoBundleComponentJob.create(installable, parentJob);
            installable.setJob(this.job);
        } else {
            this.job = installable.getJob().duplicateMetadataWithStatus();
            installable.setJob(this.job);
        }
        this.installable = installable;
        this.repo = jobRepository;
    }

    public void startTracking(JobStatus js) {
        this.job.setStatus(js);
        this.job.setStartedAt(LocalDateTime.now());
        this.job = repo.save(this.job);
    }

    public void stopTrackingTime(JobResult result) {
        this.job.setStatus(result.getStatus());
        this.job.setFinishedAt(LocalDateTime.now());
        if (result.hasException()) {
            this.job.setErrorMessage(result.getErrorMessage());
        }
        this.job = repo.save(this.job);
    }

    public EntandoBundleComponentJob getTrackedJob() {
        return this.job;
    }

}