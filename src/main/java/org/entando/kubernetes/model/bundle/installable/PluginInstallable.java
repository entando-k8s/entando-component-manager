package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallRequest.InstallAction;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.springframework.util.StringUtils;

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
            log.info("Deploying plugin {}", getName());
            EntandoPlugin plugin = BundleUtilities.generatePluginFromDescriptor(representation);
            if (shouldSkip()) {
                return; //Do nothing
            }

            kubernetesService.linkPluginAndWaitForSuccess(plugin);
        });
    }


    @Override
    public CompletableFuture<Void> uninstall() {
        return CompletableFuture.runAsync(() -> {
            log.info("Removing link to plugin {}", getName());
            if (shouldSkip()) {
                return; //Do nothing
            }

            kubernetesService.unlinkPlugin(BundleUtilities.extractNameFromDescriptor(representation));
        });
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.PLUGIN;
    }

    @Override
    public String getName() {

        if (!StringUtils.isEmpty(this.representation.getDeploymentBaseName())) {
            return this.representation.getDeploymentBaseName();
        } else {
            // TODO when we'll introduce a validation step, remove this try catch and move the check
            try {
                return this.representation.getDockerImage().toString();
            } catch (Exception e) {
                throw new EntandoComponentManagerException("There is an error in the build of the docker image. Please "
                        + "check to have supplied a valid docker image in the dedicated descriptor field");
            }
        }
    }
}
