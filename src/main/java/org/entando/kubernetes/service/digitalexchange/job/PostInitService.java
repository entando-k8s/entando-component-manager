package org.entando.kubernetes.service.digitalexchange.job;

public interface PostInitService {

    /**
     * This method tries to deploy, install or update post-init bundle according to configuration data permissions.
     */
    void install();


}
