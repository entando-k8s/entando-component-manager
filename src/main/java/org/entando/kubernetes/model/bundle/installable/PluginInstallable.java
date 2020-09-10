package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.bundle.descriptor.DockerImage;
import org.entando.kubernetes.model.bundle.descriptor.PluginDescriptor;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.service.KubernetesService;

@Slf4j
public class PluginInstallable extends Installable<PluginDescriptor> {

    private final KubernetesService kubernetesService;

    public PluginInstallable(KubernetesService kubernetesService, PluginDescriptor plugin) {
        super(plugin);
        this.kubernetesService = kubernetesService;
    }

    @Override
    public CompletableFuture<Void> install() {
        return CompletableFuture.runAsync(() -> {
            log.info("Deploying plugin {}", getName());
            EntandoPlugin plugin = kubernetesService.generatePluginFromDescriptor(representation);
            kubernetesService.linkPluginAndWaitForSuccess(plugin);
        });
    }


    @Override
    public CompletableFuture<Void> uninstall() {
        return CompletableFuture.runAsync(() -> {
            log.info("Removing link to plugin {}", getName());
            PluginDescriptor descriptor = PluginDescriptor.builder().image(getName()).build();
            kubernetesService.unlinkPlugin(kubernetesService.extractNameFromDescriptor(descriptor));
        });
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.PLUGIN;
    }

    @Override
    public String getName() {
        return this.representation.getDockerImage().toString();
    }

}
