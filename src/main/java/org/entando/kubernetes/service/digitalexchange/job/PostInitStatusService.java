package org.entando.kubernetes.service.digitalexchange.job;

public interface PostInitStatusService {

    /**
     * This method retrieve the actual post-init operation global status.
     *
     * @return This method returns the actual post-init operation global status.
     */
    PostInitStatus getStatus();

    /**
     * This method retrieves the status of current operation.
     *
     * @return true if install is not running otherwise return false
     */
    boolean isCompleted();

    /**
     * This method retrieves the number of max retries.
     *
     * @return true if install should be repeated otherwise return false
     */
    boolean shouldRetry();

    /**
     * This method updates the internal status to execution started.
     */
    void startExecution();

    void endExecution(PostInitStatus status, boolean finished, boolean noMoreTry);

}
