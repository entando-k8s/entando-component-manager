package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.processor.PluginProcessor;
import org.entando.kubernetes.model.job.PluginAPIDataEntity;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.repository.PluginAPIDataRepository;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;

@Slf4j
public class PluginInstallable extends Installable<PluginDescriptor> {

    private final KubernetesService kubernetesService;
    private final PluginAPIDataRepository pluginAPIDataRepository;

    public PluginInstallable(KubernetesService kubernetesService, PluginDescriptor plugin, InstallAction action,
            PluginAPIDataRepository pluginAPIDataRepository) {
        super(plugin, action);
        this.kubernetesService = kubernetesService;
        this.pluginAPIDataRepository = pluginAPIDataRepository;
    }

    @Override
    public CompletableFuture<Void> install() {
        return CompletableFuture.runAsync(() -> {

            logConflictStrategyAction();

            if (shouldSkip()) {
                return; //Do nothing
            }

            EntandoPlugin plugin = BundleUtilities.generatePluginFromDescriptor(representation);

            if (shouldCreate()) {
                kubernetesService.linkPluginAndWaitForSuccess(plugin);
            } else if (shouldOverride()) {
                kubernetesService.unlink(plugin.getMetadata().getName());
                kubernetesService.linkPluginAndWaitForSuccess(plugin);
            } else {
                throw new EntandoComponentManagerException("Illegal state detected");
            }

            PluginAPIDataEntity apiPathEntity = new PluginAPIDataEntity()
                    .setBundleCode(representation.getDescriptorMetadata().getBundleId())
                    .setPluginCode(representation.getDescriptorMetadata().getPluginCode())
                    .setEndpoint(BundleUtilities.composeIngressPathFromDockerImage(representation));
            pluginAPIDataRepository.save(apiPathEntity);
        });
    }


    @Override
    public CompletableFuture<Void> uninstall() {
        return CompletableFuture.runAsync(() -> {

            String pluginCode = representation.getComponentKey().getKey();

            if (shouldSkip()) {
                return; //Do nothing
            }

            log.info("Removing link to plugin {}", pluginCode);
            kubernetesService.unlinkAndScaleDownPlugin(pluginCode);

            if (! deletePluginApiData(pluginCode)) {
                log.warn("Plugin uninstalled but no data has been deleted from plugin_api_data db table");
            }
        });
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.PLUGIN;
    }

    @Override
    public String getName() {
        return this.representation.getDescriptorMetadata().getFullDeploymentName();
    }

    /**
     * delete from the db the info about the plugin api data.
     *
     * @param pluginId the plugin id from which recover the data required to execute the query
     * @return truye if a record has been delete, false otherwise
     */
    private boolean deletePluginApiData(String pluginId) {
        if (ObjectUtils.isEmpty(pluginId)) {
            return false;
        }

        pluginId = pluginId.replace(PluginProcessor.PLUGIN_DEPLOYMENT_PREFIX, "");
        String[] tokens = pluginId.split("-", 2);

        if (tokens.length < 2) {
            return false;
        }

        return pluginAPIDataRepository.deleteByBundleCodeAndPluginCode(tokens[0], tokens[1]) > 0;
    }
}
