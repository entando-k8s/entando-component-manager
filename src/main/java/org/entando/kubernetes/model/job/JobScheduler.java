package org.entando.kubernetes.model.job;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.Optional;

public class JobScheduler {

    Deque<EntandoBundleComponentJob> jobQueue;
    Deque<EntandoBundleComponentJob> processedJobStack;


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

    public void addToQueue(EntandoBundleComponentJob job) {
        jobQueue.addLast(job);
    }

    public void queueAll(Collection<EntandoBundleComponentJob> jobs) {
        for (EntandoBundleComponentJob job : jobs) {
            this.addToQueue(job);
        }
    }

    public Optional<EntandoBundleComponentJob> extractFromQueue() {
        EntandoBundleComponentJob nextComponentJob = null;
        if (!jobQueue.isEmpty()) {
            nextComponentJob = jobQueue.removeFirst();
        }
        return Optional.ofNullable(nextComponentJob);
    }

    public void recordProcessedComponentJob(EntandoBundleComponentJob componentJob) {
        processedJobStack.addLast(componentJob);
    }

    public void activateRollbackMode() {
        Deque<EntandoBundleComponentJob> rollbackQueue = new ArrayDeque<>();
        Iterator<EntandoBundleComponentJob> jobIterator = this.processedJobStack
                .descendingIterator();
        while(jobIterator.hasNext()) {
            EntandoBundleComponentJob duplicateJob = EntandoBundleComponentJob.getNewCopy(jobIterator.next());
            duplicateJob.setStartedAt(null);
            duplicateJob.setFinishedAt(null);
            rollbackQueue.addLast(duplicateJob);
        }
        this.jobQueue = rollbackQueue;
        this.clearProcessedStack();
    }

    public Optional<EntandoBundleComponentJob> componentJobWithError() {
        return this.processedJobStack.stream()
                .filter(trJob -> trJob.getStatus().isOfType(JobType.ERROR))
                .findFirst();
    }
}
