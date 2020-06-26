package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleComponentJob;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPluginBuilder;
import org.entando.kubernetes.service.KubernetesService;

@Slf4j
public class PluginInstallable extends Installable<EntandoPlugin> {

    private final KubernetesService kubernetesService;

    public PluginInstallable(KubernetesService kubernetesService, EntandoPlugin plugin) {
        super(plugin);
        this.kubernetesService = kubernetesService;
    }

    @Override
    public CompletableFuture<Void> install() {
        return CompletableFuture.runAsync(() -> {
            log.info("Deploying plugin {}", getName());
            kubernetesService.linkPluginAndWaitForSuccess(representation);
        });
    }

    @Override
    public CompletableFuture<Void> uninstall() {
        return CompletableFuture.runAsync(() -> {
            log.info("Removing link to plugin {}", getName());
            kubernetesService.unlinkPlugin(getName());
        });
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.PLUGIN;
    }

    @Override
    public String getName() {
        return this.representation.getMetadata().getName();
    }

    @Override
    public InstallPriority getInstallPriority() {
        return InstallPriority.PLUGIN;
    }
}
