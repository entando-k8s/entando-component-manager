package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;

@Slf4j
public class PluginInstallable extends Installable<PluginDescriptor> {

    private final KubernetesService kubernetesService;

    public PluginInstallable(KubernetesService kubernetesService, PluginDescriptor plugin, InstallAction action) {
        super(plugin, action);
        this.kubernetesService = kubernetesService;
    }

    @Override
    public CompletableFuture<Void> install() {
        return CompletableFuture.runAsync(() -> {

            logConflictStrategyAction();

            if (shouldSkip()) {
                return; //Do nothing
            }

            EntandoPlugin plugin = BundleUtilities.generatePluginFromDescriptor(representation);

            if (shouldCreate() || shouldOverride()) {
                kubernetesService.linkPluginAndWaitForSuccess(plugin);
            } else {
                throw new EntandoComponentManagerException("Illegal state detected");
            }
        });
    }


    @Override
    public CompletableFuture<Void> uninstall() {
        return CompletableFuture.runAsync(() -> {
            log.info("Removing link to plugin {}", representation.getComponentKey().getKey());
            if (shouldSkip()) {
                return; //Do nothing
            }

            kubernetesService.unlinkPlugin(representation.getComponentKey().getKey());
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
