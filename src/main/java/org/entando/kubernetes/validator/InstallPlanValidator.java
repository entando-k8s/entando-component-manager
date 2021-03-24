package org.entando.kubernetes.validator;

import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.controller.digitalexchange.job.model.ComponentInstallPlan;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.controller.digitalexchange.job.model.Status;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.springframework.stereotype.Component;

@Component
public class InstallPlanValidator {

    public boolean validateInstallPlanOrThrow(InstallPlan installPlan) {

        if (null != installPlan) {
            this.validateComponentMapOfThrow(installPlan.getFragments(), ComponentType.FRAGMENT);
            this.validateComponentMapOfThrow(installPlan.getAssets(), ComponentType.ASSET);
            this.validateComponentMapOfThrow(installPlan.getCategories(), ComponentType.CATEGORY);
            this.validateComponentMapOfThrow(installPlan.getContents(), ComponentType.CONTENT);
            this.validateComponentMapOfThrow(installPlan.getContentTemplates(), ComponentType.CONTENT_TEMPLATE);
            this.validateComponentMapOfThrow(installPlan.getContentTypes(), ComponentType.CONTENT_TYPE);
            this.validateComponentMapOfThrow(installPlan.getDirectories(), ComponentType.DIRECTORY);
            this.validateComponentMapOfThrow(installPlan.getGroups(), ComponentType.GROUP);
            this.validateComponentMapOfThrow(installPlan.getLabels(), ComponentType.LABEL);
            this.validateComponentMapOfThrow(installPlan.getLanguages(), ComponentType.LANGUAGE);
            this.validateComponentMapOfThrow(installPlan.getPages(), ComponentType.PAGE);
            this.validateComponentMapOfThrow(installPlan.getPageTemplates(), ComponentType.PAGE_TEMPLATE);
            this.validateComponentMapOfThrow(installPlan.getPlugins(), ComponentType.PLUGIN);
            this.validateComponentMapOfThrow(installPlan.getResources(), ComponentType.RESOURCE);
            this.validateComponentMapOfThrow(installPlan.getWidgets(), ComponentType.WIDGET);
        }

        return true;
    }

    private void validateComponentMapOfThrow(Map<String, ComponentInstallPlan> componentInstallPlanMap,
            ComponentType componentType) {

        componentInstallPlanMap.forEach((key, componentInstallPlan) -> {
            validateKeyOrThrow(key, componentType);
            validateActionOrThrow(key, componentInstallPlan.getAction(), componentType);
            validateStatusNEW(key, componentInstallPlan.getStatus(), componentInstallPlan.getAction(), componentType);
            validateStatusNotNEW(key, componentInstallPlan.getStatus(), componentInstallPlan.getAction(), componentType);
        });
    }

    /**
     * key must NOT be empty.
     * @param key the key of the current component
     * @param componentType he current component type
     */
    private void validateKeyOrThrow(String key, ComponentType componentType) {
        if (StringUtils.isEmpty(key)) {
            throw new EntandoComponentManagerException(
                    String.format("Empty key found in the InstallPlan for components %s",
                            componentType.getTypeName()));
        }
    }

    /**
     * install action must NOT be null.
     * @param action the action to validate
     * @param componentType the current component type
     * @param key the key of the current component
     */
    private void validateActionOrThrow(String key, InstallAction action, ComponentType componentType) {
        if (null == action) {
            throw new EntandoComponentManagerException(
                    String.format("Null InstallAction found in the InstallPlan for %s %s",
                            componentType.getTypeName(), key));
        }
    }

    /**
     * if the Status == NEW the action can be only CREATE.
     * @param action the action to validate
     * @param componentType the current component type
     * @param key the key of the current component
     */
    private void validateStatusNEW(String key, Status status, InstallAction action, ComponentType componentType) {
        if (status == Status.NEW && action != InstallAction.CREATE) {
            throw new EntandoComponentManagerException(
                    String.format(
                            "The InstallAction %s supplied for the %s %s is not valid. A component with Status.NEW "
                                    + "can receives only an InstallAction.CREATE",
                            action, componentType.getTypeName(), key));
        }
    }

    /**
     * if the Status != NEW the action can be only SKIP or OVERRIDE.
     * @param action the action to validate
     * @param componentType the current component type
     * @param key the key of the current component
     */
    private void validateStatusNotNEW(String key, Status status, InstallAction action, ComponentType componentType) {
        if (status != Status.NEW && action == InstallAction.CREATE) {
            throw new EntandoComponentManagerException(
                    String.format(
                            "The InstallAction %s supplied for the %s %s is not valid. A component with Status.DIFF "
                                    + "or Status.EQUAL can receives only InstallAction.SKIP or InstallAction.OVERRIDE",
                            action, componentType.getTypeName(), key));
        }
    }
}
