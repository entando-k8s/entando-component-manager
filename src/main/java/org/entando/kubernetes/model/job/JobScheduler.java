package org.entando.kubernetes.model.job;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.Optional;

public class JobScheduler {

    Deque<EntandoBundleComponentJobEntity> jobQueue;
    Deque<EntandoBundleComponentJobEntity> processedJobStack;

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

    public void addToQueue(EntandoBundleComponentJobEntity job) {
        jobQueue.addLast(job);
    }

    public void queueAll(Collection<EntandoBundleComponentJobEntity> jobs) {
        for (EntandoBundleComponentJobEntity job : jobs) {
            this.addToQueue(job);
        }
    }

    public Optional<EntandoBundleComponentJobEntity> extractFromQueue() {
        EntandoBundleComponentJobEntity nextComponentJob = null;
        if (!jobQueue.isEmpty()) {
            nextComponentJob = jobQueue.removeFirst();
        }
        return Optional.ofNullable(nextComponentJob);
    }

    public void recordProcessedComponentJob(EntandoBundleComponentJobEntity componentJob) {
        processedJobStack.addLast(componentJob);
    }

    public void activateRollbackMode() {
        Deque<EntandoBundleComponentJobEntity> rollbackQueue = new ArrayDeque<>();
        Iterator<EntandoBundleComponentJobEntity> jobIterator = this.processedJobStack
                .descendingIterator();
        while (jobIterator.hasNext()) {
            EntandoBundleComponentJobEntity duplicateJob = EntandoBundleComponentJobEntity.getNewCopy(jobIterator.next());
            duplicateJob.setStartedAt(null);
            duplicateJob.setFinishedAt(null);
            rollbackQueue.addLast(duplicateJob);
        }
        this.jobQueue = rollbackQueue;
        this.clearProcessedStack();
    }

    public Optional<EntandoBundleComponentJobEntity> componentJobWithError() {
        return this.processedJobStack.stream()
                .filter(trJob -> trJob.getStatus().isOfType(JobType.ERROR))
                .findFirst();
    }
}
