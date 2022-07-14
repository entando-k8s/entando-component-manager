package org.entando.kubernetes.service.digitalexchange.job;

import java.util.Optional;

public interface PostInitService {

    //  This method tries to deploy, install or update post-init bundle according to configuration data permissions.
    void install();

    /*
     * This method returns the actual post-init operation global status.
     *
     * @return the global status
     */
    PostInitStatus getStatus();

    /*
     * This method returns true if install is not running otherwise return false.
     *
     * @return the status of current operation;
     */
    boolean isCompleted();

    boolean shouldRetry();

    /*
     * This method returns the configuration value of the frequency of post-init operation.
     *
     * @return the configuration value of the frequency of post-init operation
     */
    int getFrequencyInSeconds();

    /**
     * This method checks if some operation is allowed for a post init bundle.
     *
     * @param bundleCode the identifier of the bundle to check
     * @param operation  the identifier of the operation to check
     * @return Optional.empty if bundle not found, true if operation is allowed, otherwise false
     */
    Optional<Boolean> isBundleOperationAllowed(String bundleCode, String operation);

}
