package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.bundle.processor.PluginProcessor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.service.KubernetesService;

@Slf4j
public class PluginInstallable extends Installable<EntandoPlugin> {

    private final DigitalExchangeJob job;
    private KubernetesService kubernetesService;

    public PluginInstallable(
            KubernetesService kubernetesService,
            EntandoPlugin plugin,
            DigitalExchangeJob job) {
        super(plugin);
        this.job = job;
        this.kubernetesService = kubernetesService;
    }

    @Override
    public CompletableFuture install() {
        return CompletableFuture.runAsync(() -> {
            log.info("Deploying a new plugin {}", representation.getSpec().getImage());
            kubernetesService.linkAndWaitForPlugin(representation);
        });
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.PLUGIN;
    }

    @Override
    public String getName() {
        return job.getComponentId();
    }

}
