package org.entando.kubernetes.model.digitalexchange;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;


public class JobTracker {

    EntandoBundleJob job;
    EntandoBundleJobRepository jobRepository;
    Deque<EntandoBundleComponentJob> componentJobQueue;
    Deque<EntandoBundleComponentJob> processedComponentStack;

    public JobTracker(EntandoBundleJobRepository jobRepository) {
        this.jobRepository = jobRepository;
        this.componentJobQueue = new ArrayDeque<>();
        this.processedComponentStack = new ArrayDeque<>();
    }

    public JobTracker(EntandoBundleJob job, EntandoBundleJobRepository jobRepository) {
        this.job = job;
        this.jobRepository = jobRepository;
        this.componentJobQueue = new ArrayDeque<>();
        this.processedComponentStack = new ArrayDeque<>();
    }

    public void updateTrackedJobStatus(JobStatus jobStatus) {
        this.job.setStatus(jobStatus);
        this.jobRepository.updateJobStatus(this.job.getId(), jobStatus);
    }

    public void setJob(EntandoBundleJob job) {
        this.job = job;
    }

    public EntandoBundleJob getJob() {
        return this.job;
    }

    public void clearJobQueue() {
        this.componentJobQueue.clear();
    }

    public void clearProcessedStack() {
        this.processedComponentStack.clear();
    }

    public void queueComponentJob(EntandoBundleComponentJob componentJob) {
        componentJobQueue.addLast(componentJob);
    }

    public void queueAllComponentJobs(List<EntandoBundleComponentJob> componentJobList) {
        for (EntandoBundleComponentJob componentJob : componentJobList) {
            this.queueComponentJob(componentJob);
        }
    }

    public Optional<EntandoBundleComponentJob> extractNextComponentJobToProcess() {
        EntandoBundleComponentJob nextComponentJob = null;
        if (!componentJobQueue.isEmpty()) {
            nextComponentJob = componentJobQueue.removeFirst();
        }
        return Optional.ofNullable(nextComponentJob);
    }

    public void recordProcessedComponentJob(EntandoBundleComponentJob componentJob) {
        processedComponentStack.addLast(componentJob);
    }


    public void activateRollbackMode() {
        this.clearJobQueue();
        Deque<EntandoBundleComponentJob> rollbackQueue = new ArrayDeque<>();
        Iterator<EntandoBundleComponentJob> jobIterator = this.processedComponentStack
                .descendingIterator();
        while(jobIterator.hasNext()) {
            EntandoBundleComponentJob clonedJob = jobIterator.next().duplicateAllFields();
            rollbackQueue.addLast(clonedJob);
        }
        this.componentJobQueue = rollbackQueue;
        this.clearProcessedStack();
    }

    public boolean hasAnyComponentError() {
        return this.processedComponentStack.stream().anyMatch(cj -> cj.getStatus().isOfType(JobType.ERROR));
    }
}