package org.entando.kubernetes.model.bundle.processor;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.controller.digitalexchange.job.model.ComponentInstallPlan;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.controller.digitalexchange.job.model.Status;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.descriptor.Descriptor;

public abstract class BaseComponentProcessor<T extends Descriptor> implements ComponentProcessor<T> {

    public static final String BUNDLE_ID_PLACEHOLDER = "__BUNDLE_ID__";

    protected InstallAction extractInstallAction(String componentCode,
            InstallAction conflictStrategy, InstallPlan installPlan) {

        Map<String, ComponentInstallPlan> compInstallPlansByType = installPlan
                .getPlanByType(getSupportedComponentType());
        if (installActionExistForComponent(compInstallPlansByType, componentCode)) {
            return compInstallPlansByType.get(componentCode).getAction();
        }

        if (isConflict(componentCode, installPlan)) {
            return conflictStrategy;
        }

        return InstallAction.CREATE;
    }

    /**
     * check if the received compInstallPlansByType map contains a value for the key represented by componentCode.
     * @param compInstallPlansByType the map in which search for the InstallAction
     * @param componentCode the code of the component of which search the InstallAction
     * @return true if the map contains a ComponentInstallPlan with a valid InstallAction for the desired component code
     */
    private boolean installActionExistForComponent(Map<String, ComponentInstallPlan> compInstallPlansByType,
            String componentCode) {

        return compInstallPlansByType.containsKey(componentCode)
                && null != compInstallPlansByType.get(componentCode).getAction();
    }

    protected boolean isConflict(String contentId, InstallPlan installPlan) {
        Map<String, ComponentInstallPlan> planByType = installPlan.getPlanByType(getSupportedComponentType());

        return planByType.containsKey(contentId)
                && planByType.get(contentId).getStatus() != Status.NEW;
    }

    protected EntandoComponentManagerException makeMeaningfulException(Exception e) {
        return new EntandoComponentManagerException(
                String.format("Error processing %s components", getSupportedComponentType().getTypeName()), e);
    }

    /**
     * replace any occurrence of the bundle id placeholder in the received input string.
     * @param bundleId the bundle id to use as replacement value
     * @param getter the supplier providing the input in which search the placeholder
     * @param setter the consumer to set the new replaced value
     */
    protected void applyBundleIdPlaceholderReplacement(String bundleId, Supplier<String> getter, Consumer<String> setter) {
        final String replacedValue = replaceBundleIdPlaceholder(getter.get(), bundleId);
        setter.accept(replacedValue);
    }

    /**
     * replace any occurrence of the bundle id placeholder in the received input string.
     * @param input the string in which apply the replacement
     * @param bundleId the string to use as replacement value
     * @return the string containing the replaced value
     */
    protected String replaceBundleIdPlaceholder(String input, String bundleId) {
        if (StringUtils.isEmpty(input)) {
            return input;
        }
        return input.replace(BUNDLE_ID_PLACEHOLDER, bundleId);
    }
}
