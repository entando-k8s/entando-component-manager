package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.job.PluginAPIDataEntity;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.repository.PluginAPIDataRepository;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;

@Slf4j
public class PluginInstallable extends Installable<PluginDescriptor> {

    private final KubernetesService kubernetesService;
    private final PluginAPIDataRepository pluginAPIPathRepository;

    public PluginInstallable(KubernetesService kubernetesService, PluginDescriptor plugin, InstallAction action,
            PluginAPIDataRepository pluginAPIPathRepository) {
        super(plugin, action);
        this.kubernetesService = kubernetesService;
        this.pluginAPIPathRepository = pluginAPIPathRepository;
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
                    .setBundleId(representation.getDescriptorMetadata().getBundleId())
                    .setServiceId(representation.getDescriptorMetadata().getPluginId())
                    .setIngressPath(BundleUtilities.composeIngressPathFromDockerImage(representation));
            pluginAPIPathRepository.save(apiPathEntity);
            // TODO prevent the custom ingress creation or create both? does it already work in this way?
        });
    }


    @Override
    public CompletableFuture<Void> uninstall() {
        return CompletableFuture.runAsync(() -> {

            String pluginId = representation.getComponentKey().getKey();

            if (shouldSkip()) {
                return; //Do nothing
            }

            log.info("Removing link to plugin {}", pluginId);
            kubernetesService.unlinkAndScaleDownPlugin(pluginId);
            pluginAPIPathRepository.deleteByBundleIdAndServiceId(representation.getDescriptorMetadata().getBundleId(),
                    pluginId);
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
}
