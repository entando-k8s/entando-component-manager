package org.entando.kubernetes.service.digitalexchange.job;

import java.util.Optional;
import org.entando.kubernetes.service.digitalexchange.job.PostInitConfigurationServiceImpl.PostInitData;

public interface PostInitConfigurationService {

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


    PostInitData getConfigurationData();
}
