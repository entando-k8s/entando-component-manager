/**
 * request to install a bundle using an install plans previously fetched.
 */

package org.entando.kubernetes.controller.digitalexchange.job.model;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class InstallWithPlansRequest extends InstallPlan {

    private String version = BundleUtilities.LATEST_VERSION;

    /**
     * if required, apply needed normalizations so the InstallWithPlansRequest and return a NEW InstallWithPlansRequest.
     *
     * @return a new normalized InstallWithPlansRequest
     */
    public InstallWithPlansRequest normalize() {

        return (InstallWithPlansRequest) new InstallWithPlansRequest()
                .setVersion(this.getVersion())
                .setHasConflicts(this.getHasConflicts())
                .setWidgets(setCreateStrategyIfStatusNewAndActionIsNull(this.getWidgets()))
                .setFragments(setCreateStrategyIfStatusNewAndActionIsNull(this.getFragments()))
                .setPages(setCreateStrategyIfStatusNewAndActionIsNull(this.getPages()))
                .setPageTemplates(setCreateStrategyIfStatusNewAndActionIsNull(this.getPageTemplates()))
                .setContents(setCreateStrategyIfStatusNewAndActionIsNull(this.getContents()))
                .setContentTemplates(setCreateStrategyIfStatusNewAndActionIsNull(this.getContentTemplates()))
                .setContentTypes(setCreateStrategyIfStatusNewAndActionIsNull(this.getContentTypes()))
                .setAssets(setCreateStrategyIfStatusNewAndActionIsNull(this.getAssets()))
                .setDirectories(setCreateStrategyIfStatusNewAndActionIsNull(this.getDirectories()))
                .setResources(setCreateStrategyIfStatusNewAndActionIsNull(this.getResources()))
                .setPlugins(setCreateStrategyIfStatusNewAndActionIsNull(this.getPlugins()))
                .setCategories(setCreateStrategyIfStatusNewAndActionIsNull(this.getCategories()))
                .setGroups(setCreateStrategyIfStatusNewAndActionIsNull(this.getGroups()))
                .setLabels(setCreateStrategyIfStatusNewAndActionIsNull(this.getLabels()))
                .setLanguages(setCreateStrategyIfStatusNewAndActionIsNull(this.getLanguages()));
    }

    /**
     * for each map value, if status = NEW and no action, set the default action CREATE.
     *
     * @param componentsMap the map of components on which set the default strategy
     * @return the updated components map
     */
    private Map<String, ComponentInstallPlan> setCreateStrategyIfStatusNewAndActionIsNull(
            Map<String, ComponentInstallPlan> componentsMap) {

        if (componentsMap != null) {
            componentsMap.forEach((key, componentInstallPlan) -> {
                if (componentInstallPlan.getStatus().equals(Status.NEW) && componentInstallPlan.getAction() == null) {
                    componentInstallPlan.setAction(InstallAction.CREATE);
                }
            });
        }

        return componentsMap;
    }


}
