package org.entando.kubernetes.service.digitalexchange.job;

import java.util.Optional;

public interface PostInitService {

    /**
     * This method tries to deploy, install or update post-init bundle according to configuration data permissions.
     */
    void install();

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
     * This method retrieves the configuration value of the frequency of post-init operation.
     *
     * @return the configuration value of the frequency of post-init operation
     */
    int getFrequencyInSeconds();

    /**
     * This method checks, with a white list strategy,  if some ECR action (list, refresh, uninstall ...) is allowed for
     * a post init bundle.
     *
     * @param bundleCode the identifier of the bundle to check
     * @param action     the identifier of the action to check
     * @return Optional.empty if bundle not found, true if operation is allowed, otherwise false
     */
    Optional<Boolean> isEcrActionAllowed(String bundleCode, String action);

}
