package org.entando.kubernetes.model.digitalexchange;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;


public class JobTracker {

    EntandoBundleJob job;
    Deque<EntandoBundleComponentJob> componentJobQueue;
    Deque<EntandoBundleComponentJob> processedComponentStack;

    public JobTracker() {
        this.componentJobQueue = new ArrayDeque<>();
        this.processedComponentStack = new ArrayDeque<>();
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

    public Optional<EntandoBundleComponentJob> extractLastProcessedComponentJob() {
        EntandoBundleComponentJob lastProcessedComponentJob = null;
        if (!processedComponentStack.isEmpty()) {
            lastProcessedComponentJob = componentJobQueue.removeLast();
        }
        return Optional.ofNullable(lastProcessedComponentJob);
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
    }

    public boolean hasAnyComponentError() {
        return this.processedComponentStack.stream().anyMatch(cj -> cj.getStatus().isOfType(JobType.ERROR));
    }
}