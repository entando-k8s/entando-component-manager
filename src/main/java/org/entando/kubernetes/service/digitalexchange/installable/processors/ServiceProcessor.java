package org.entando.kubernetes.service.digitalexchange.installable.processors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.EntandoPluginDeploymentRequest;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJobComponent;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.installable.ComponentProcessor;
import org.entando.kubernetes.service.digitalexchange.installable.Installable;
import org.entando.kubernetes.service.digitalexchange.job.ZipReader;
import org.entando.kubernetes.service.digitalexchange.job.model.ComponentDescriptor;
import org.entando.kubernetes.service.digitalexchange.job.model.ComponentSpecDescriptor;
import org.entando.kubernetes.service.digitalexchange.job.model.ServiceDescriptor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.Optional.ofNullable;

/**
 * Processor to perform a deployment on the Kubernetes Cluster.
 *
 * Will read the service property on the component descriptor yaml and
 * convert it into a EntandoPlugin custom resource
 *
 * @author Sergio Marcelino
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceProcessor implements ComponentProcessor {

    private final KubernetesService kubernetesService;

    @Override
    public List<? extends Installable> process(final DigitalExchangeJob job, final ZipReader zipReader, final ComponentDescriptor descriptor) {
        return ofNullable(descriptor.getComponents())
                .map(ComponentSpecDescriptor::getService)
                .map(serviceDescriptor -> new ServiceInstallable(serviceDescriptor, job))
                .map(Collections::singletonList)
                .orElseGet(Collections::emptyList);
    }

    @Override
    public boolean shouldProcess(final ComponentType componentType) {
        return componentType == ComponentType.DEPLOYMENT;
    }

    @Override
    public void uninstall(final DigitalExchangeJobComponent component) {
        log.info("Removing deployment {}", component.getName());
        kubernetesService.unlinkPlugin(component.getName());
    }

    public class ServiceInstallable extends Installable<ServiceDescriptor> {

        private final DigitalExchangeJob job;

        public ServiceInstallable(final ServiceDescriptor serviceDescriptor,
                                  final DigitalExchangeJob job) {
            super(serviceDescriptor);
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
                kubernetesService.linkPlugin(deploymentRequest);
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
}
