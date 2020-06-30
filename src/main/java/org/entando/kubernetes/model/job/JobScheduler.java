package org.entando.kubernetes.model.job;

import org.entando.kubernetes.model.bundle.installable.Installable;

import java.util.*;

public class JobScheduler {

    Deque<Installable> jobQueue;
    Deque<JobTracker> processedJobStack;


    public JobScheduler() {
        this.jobQueue = new ArrayDeque<>();
        this.processedJobStack = new ArrayDeque<>();
    }

    public void clearJobQueue() {
        this.jobQueue.clear();
    }

    public void clearProcessedStack() {
        this.processedJobStack.clear();
    }

    public void addToQueue(Installable job) {
        jobQueue.addLast(job);
    }

    public void queueAll(List<Installable> jobs) {
        for (Installable job : jobs) {
            this.addToQueue(job);
        }
    }

    public Optional<Installable> extractFromQueue() {
        Installable nextComponentJob = null;
        if (!jobQueue.isEmpty()) {
            nextComponentJob = jobQueue.removeFirst();
        }
        return Optional.ofNullable(nextComponentJob);
    }

    public void recordProcessedComponentJob(JobTracker componentJob) {
        processedJobStack.addLast(componentJob);
    }

    public void activateRollbackMode() {
        Deque<Installable> rollbackQueue = new ArrayDeque<>();
        Iterator<JobTracker> jobIterator = this.processedJobStack
                .descendingIterator();
        while(jobIterator.hasNext()) {
            JobTracker trackedJob = jobIterator.next();
            rollbackQueue.addLast(trackedJob.installable);
        }
        this.jobQueue = rollbackQueue;
        this.clearProcessedStack();
    }

    public Optional<JobTracker> componentJobWithError() {
        return this.processedJobStack.stream()
                .filter(trJob -> trJob.getTrackedJob().getStatus().isOfType(JobType.ERROR))
                .findFirst();
    }
}
