package org.entando.kubernetes.service.digitalexchange;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.entando.kubernetes.controller.digitalexchange.component.DigitalExchangeComponent;
import org.entando.kubernetes.model.EntandoPluginDeploymentRequest;
import org.entando.kubernetes.model.EntandoPluginDeploymentResponse;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.digitalexchange.JobStatus;
import org.entando.kubernetes.model.digitalexchange.JobType;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.component.DigitalExchangeComponentsService;
import org.entando.kubernetes.service.digitalexchange.model.DigitalExchange;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DigitalExchangeInstallService {

    private final @NonNull KubernetesService kubernetesService;
    private final @NonNull DigitalExchangesService exchangesService;
    private final @NonNull DigitalExchangeComponentsService digitalExchangeComponentsService;

    private static final Map<String, JobStatus> statusMap = new HashMap<>();

    static {
        statusMap.put("requested", JobStatus.CREATED);
        statusMap.put("started", JobStatus.IN_PROGRESS);
        statusMap.put("successful", JobStatus.COMPLETED);
        statusMap.put("failed", JobStatus.ERROR);
    }

    public DigitalExchangeJob install(final String digitalExchangeId, final String componentId) {
        final DigitalExchange digitalExchange = exchangesService.findById(digitalExchangeId);
        final DigitalExchangeComponent component = digitalExchangeComponentsService
                .getComponent(digitalExchange, componentId).getPayload();

        if (component.getMetadata() != null) {
            final EntandoPluginDeploymentRequest deploymentRequest = new EntandoPluginDeploymentRequest();
            deploymentRequest.setDbms(component.getMetadata().get("dbms"));
            deploymentRequest.setHealthCheckPath(component.getMetadata().get("healthCheckPath"));
            deploymentRequest.setIngressPath(component.getMetadata().get("ingressPath"));
            deploymentRequest.setImage(component.getMetadata().get("image"));
            deploymentRequest.setPlugin(componentId);

            // TODO roles and permissions pending
            kubernetesService.deploy(deploymentRequest, digitalExchange);

            return getJob(componentId);
        }

        return null;
    }

    public DigitalExchangeJob getJob(final String componentId) {
        final EntandoPluginDeploymentResponse deployment = kubernetesService.getDeployment(componentId);
        final DigitalExchangeJob job = new DigitalExchangeJob();

        job.setComponentId(componentId);
        job.setComponentName(deployment.getPlugin());
        job.setComponentVersion(deployment.getImage());
        job.setDigitalExchangeId(deployment.getDigitalExchangeId());
        job.setDigitalExchangeUrl(deployment.getDigitalExchangeUrl());
        job.setJobType(JobType.INSTALL);
        job.setProgress(0);
        job.setStatus(statusMap.getOrDefault(deployment.getDeploymentPhase(), JobStatus.CREATED));

        return job;
    }

}
