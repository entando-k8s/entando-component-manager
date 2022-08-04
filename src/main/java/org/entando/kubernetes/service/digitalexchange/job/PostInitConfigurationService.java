package org.entando.kubernetes.service.digitalexchange.job;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

public interface PostInitConfigurationService {

    public static final int DEFAULT_CONFIGURATION_TIMEOUT = 600;
    public static final int DEFAULT_CONFIGURATION_FREQUENCY = 5;
    public static final String ACTION_INSTALL_OR_UPDATE = "install-or-update";

    /**
     * This method retrieves the configuration value of the frequency of post-init operation.
     *
     * @return the configuration value of the frequency of post-init operation
     */
    int getFrequencyInSeconds();

    /**
     * This method retrieves the configuration value of the global timeout of post-init operation.
     *
     * @return the configuration value of the global timeout of post-init operation
     */
    int getMaxAppWaitInSeconds();

    /**
     * This method checks, with a white list strategy,  if some ECR action (list, refresh, uninstall ...) is allowed for
     * a post init bundle.
     *
     * @param bundleCode the identifier of the bundle to check
     * @param action     the identifier of the action to check
     * @return Optional.empty if bundle not found, true if operation is allowed, otherwise false
     */
    Optional<Boolean> isEcrActionAllowed(String bundleCode, String action);


    /**
     * This method retrieves the whole configuration value of the post-init operation.
     *
     * @return whole configuration value of the post-init operation
     */
    PostInitData getConfigurationData();

    @ToString
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class PostInitData {

        @Getter
        @Setter
        private int frequency = DEFAULT_CONFIGURATION_FREQUENCY;
        @Getter
        @Setter
        private int maxAppWait = DEFAULT_CONFIGURATION_TIMEOUT;
        @Getter
        private List<PostInitItem> items = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class PostInitItem {

        private String name;
        private String[] ecrActions;
        private String action;
        private String url;
        private String version;
        private int priority;
    }
}