package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleJob;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.service.KubernetesService;

@Slf4j
public class PluginInstallable extends Installable<EntandoPlugin> {

    private final EntandoBundleJob job;
    private final KubernetesService kubernetesService;

    public PluginInstallable(
            KubernetesService kubernetesService,
            EntandoPlugin plugin,
            EntandoBundleJob job) {
        super(plugin);
        this.job = job;
        this.kubernetesService = kubernetesService;
    }

    @Override
    public CompletableFuture<Void> install() {
        return CompletableFuture.runAsync(() -> {
            log.info("Deploying plugin {}", representation.getMetadata().getName());
            kubernetesService.linkPluginAndWaitForSuccess(representation);
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

}
