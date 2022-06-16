package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.processor.PluginProcessor;
import org.entando.kubernetes.model.job.PluginDataEntity;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.repository.PluginDataRepository;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;

@Slf4j
public class PluginInstallable extends Installable<PluginDescriptor> {

    private final KubernetesService kubernetesService;
    private final PluginDataRepository pluginDataRepository;

    public PluginInstallable(KubernetesService kubernetesService, PluginDescriptor plugin, InstallAction action,
            PluginDataRepository pluginDataRepository) {
        super(plugin, action);
        this.kubernetesService = kubernetesService;
        this.pluginDataRepository = pluginDataRepository;
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

            PluginDataEntity apiPathEntity = new PluginDataEntity()
                    .setBundleId(representation.getDescriptorMetadata().getBundleId())
                    .setPluginId(representation.getDescriptorMetadata().getPluginId())
                    .setPluginName(representation.getDescriptorMetadata().getPluginName())
                    .setPluginCode(representation.getDescriptorMetadata().getPluginCode())
                    .setEndpoint(BundleUtilities.composeIngressPathFromDockerImage(representation));
            pluginDataRepository.save(apiPathEntity);
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

            if (! deletePluginData(pluginCode)) {
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
        return this.representation.getDescriptorMetadata().getPluginCode();
    }

    /**
     * delete from the db the info about the plugin api data.
     *
     * @param pluginCode the plugin code from which recover the data required to execute the query
     * @return true if a record has been delete, false otherwise
     */
    private boolean deletePluginData(String pluginCode) {
        if (ObjectUtils.isEmpty(pluginCode)) {
            log.warn("Empty plugin code retrieved by the database. Can't delete plugin detail");
            return false;
        }

        return pluginDataRepository.deleteByPluginCode(pluginCode) > 0;
    }
}
