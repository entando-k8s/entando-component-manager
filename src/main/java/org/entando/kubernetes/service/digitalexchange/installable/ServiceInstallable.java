package org.entando.kubernetes.service.digitalexchange.installable;

import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.EntandoPluginDeploymentRequest;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.job.model.ServiceDescriptor;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class ServiceInstallable extends Installable<ServiceDescriptor> {

    private final KubernetesService kubernetesService;
    private final DigitalExchangeJob job;

    public ServiceInstallable(final ServiceDescriptor serviceDescriptor,
                              final KubernetesService kubernetesService,
                              final DigitalExchangeJob job) {
        super(serviceDescriptor);
        this.kubernetesService = kubernetesService;
        this.job = job;
    }

    @Override
    public CompletableFuture install() {
        return CompletableFuture.runAsync(() -> {
            final EntandoPluginDeploymentRequest deploymentRequest = new EntandoPluginDeploymentRequest();
            deploymentRequest.setPlugin(job.getComponentId());
            deploymentRequest.setDbms(representation.getDbms());
            deploymentRequest.setHealthCheckPath(representation.getHealthCheckPath());
            deploymentRequest.setIngressPath(representation.getIngressPath());
            deploymentRequest.setImage(representation.getImage());
            deploymentRequest.setPermissions(representation.getPermissions());
            deploymentRequest.setRoles(representation.getRoles());

            log.info("Deploying a new service {}", deploymentRequest.getImage());
            kubernetesService.deploy(deploymentRequest);
        });
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.DEPLOYMENT;
    }

    @Override
    public String getName() {
        return job.getComponentId();
    }

}
