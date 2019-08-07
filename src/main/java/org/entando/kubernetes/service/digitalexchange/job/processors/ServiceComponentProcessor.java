package org.entando.kubernetes.service.digitalexchange.job.processors;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.EntandoPluginDeploymentRequest;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.job.ZipReader;
import org.entando.kubernetes.service.digitalexchange.job.model.ServiceDescriptor;
import org.entando.kubernetes.service.digitalexchange.model.DigitalExchange;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceComponentProcessor implements ComponentProcessor<ServiceDescriptor> {

    private final @NonNull KubernetesService kubernetesService;

    @Override
    public void processComponent(final DigitalExchange digitalExchange, final String componentId,
                                 final ServiceDescriptor descriptor, final ZipReader zipReader,
                                 final String folder) {

        final EntandoPluginDeploymentRequest deploymentRequest = new EntandoPluginDeploymentRequest();
        deploymentRequest.setPlugin(componentId);
        deploymentRequest.setDbms(descriptor.getDbms());
        deploymentRequest.setHealthCheckPath(descriptor.getHealthCheckPath());
        deploymentRequest.setIngressPath(descriptor.getIngressPath());
        deploymentRequest.setImage(descriptor.getImage());
        deploymentRequest.setPermissions(descriptor.getPermissions());
        deploymentRequest.setRoles(descriptor.getRoles());

        log.info("Deploying a new service {}", deploymentRequest);
        kubernetesService.deploy(deploymentRequest, digitalExchange);
    }
}
