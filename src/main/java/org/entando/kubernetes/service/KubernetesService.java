package org.entando.kubernetes.service;

import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimStatus;
import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apps.DeploymentCondition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.exception.PluginNotFoundException;
import org.entando.kubernetes.model.AbstractServerStatus;
import org.entando.kubernetes.model.DbmsImageVendor;
import org.entando.kubernetes.model.DeploymentStatus;
import org.entando.kubernetes.model.EntandoCustomResourceStatus;
import org.entando.kubernetes.model.EntandoDeploymentPhase;
import org.entando.kubernetes.model.EntandoPluginDeploymentRequest;
import org.entando.kubernetes.model.EntandoPluginDeploymentResponse;
import org.entando.kubernetes.model.PluginServiceStatus;
import org.entando.kubernetes.model.PodStatus;
import org.entando.kubernetes.model.plugin.DoneableEntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPluginList;
import org.entando.kubernetes.model.plugin.EntandoPluginSpecBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KubernetesService {

    public static final String ENTANDOPLUGIN_CRD_NAME = "entandoplugins.entando.org";

    private final @NonNull
    KubernetesClient client;
    private final @NonNull
    String entandoAppNamespace;
    private final @NonNull
    String entandoAppName;
    private final @NonNull
    String keycloakServerName;
    private final @NonNull
    String keycloakServerNamespace;

    public KubernetesService(@Autowired final KubernetesClient client,
            @Value("${entando.app.namespace}") final String entandoAppNamespace,
            @Value("${entando.app.name}") final String entandoAppName,
            @Value("${entando.keycloak.server.name}") String keycloakServerName,
            @Value("${entando.keycloak.server.namespace}") String keycloakServerNamespace) {
        this.client = client;
        this.entandoAppNamespace = entandoAppNamespace;
        this.entandoAppName = entandoAppName;
        this.keycloakServerName = keycloakServerName;
        this.keycloakServerNamespace = keycloakServerNamespace;
    }

    public EntandoPluginDeploymentResponse getDeployment(final String pluginId) {
        return getDeploymentOptional(pluginId)
                .orElseThrow(PluginNotFoundException::new);
    }

    public Optional<EntandoPluginDeploymentResponse> getDeploymentOptional(final String pluginId) {
        return ofNullable(getOperation().withName(pluginId).get())
                .map(this::map);
    }

    public Optional<EntandoPlugin> getPluginOptional(final String pluginId) {
        return ofNullable(getOperation().withName(pluginId).get());
    }

    public void deleteDeployment(final String pluginId) {
        final NonNamespaceOperation<EntandoPlugin, EntandoPluginList, DoneableEntandoPlugin,
                Resource<EntandoPlugin, DoneableEntandoPlugin>> operation = getOperation();
        ofNullable(operation.withName(pluginId).get())
                .ifPresent(operation::delete);
    }

    public List<EntandoPluginDeploymentResponse> getDeployments() {
        return getOperation().list()
                .getItems().stream()
                .map(this::map)
                .collect(Collectors.toList());
    }

    public void deploy(final EntandoPluginDeploymentRequest request) {
        final EntandoPlugin plugin = new EntandoPlugin();
        final ObjectMeta objectMeta = new ObjectMeta();

        objectMeta.setName(request.getPlugin());

        final EntandoPluginSpecBuilder specBuilder = new EntandoPluginSpecBuilder()
                .withDbms(DbmsImageVendor.forValue(request.getDbms()))
                .withImage(request.getImage())
                .withIngressPath(request.getIngressPath())
                .withHealthCheckPath(request.getHealthCheckPath())
                .withEntandoApp(entandoAppNamespace, entandoAppName)
                .withKeycloakServer(keycloakServerNamespace, keycloakServerName)
                .withReplicas(1);

        request.getRoles().forEach(r -> specBuilder.withRole(r.getName(), r.getCode()));
        request.getPermissions().forEach(p -> specBuilder.withPermission(p.getClientId(), p.getRole()));

        plugin.setMetadata(objectMeta);
        plugin.setSpec(specBuilder.build());
        plugin.setApiVersion("entando.org/v1alpha1");

        getOperation().create(plugin);
    }

    private EntandoPluginDeploymentResponse map(final EntandoPlugin deployment) {
        final String pluginId = deployment.getMetadata().getName();
        final EntandoPluginDeploymentResponse response = new EntandoPluginDeploymentResponse();
        final EntandoCustomResourceStatus entandoStatus = deployment.getStatus();
        final EntandoDeploymentPhase deploymentPhase = ofNullable(entandoStatus)
                .map(EntandoCustomResourceStatus::getEntandoDeploymentPhase)
                .orElse(EntandoDeploymentPhase.STARTED);

        response.setPath(deployment.getSpec().getIngressPath());
        response.setPlugin(pluginId);
        response.setReplicas(deployment.getSpec().getReplicas());
        response.setDeploymentPhase(deploymentPhase.toValue());


        if(entandoStatus != null) {
            entandoStatus.forServerQualifiedBy("jeeServer")
                    .map(status -> convert(status, "jeeServer"))
                    .ifPresent(response::setServerStatus);

            entandoStatus.forDbQualifiedBy("dbServer")
                    .map(status -> convert(status, "dbServer"))
                    .ifPresent(status -> response.setExternalServiceStatuses(Collections.singletonList(status)));
        }

        final Integer availableReplicas = ofNullable(response.getServerStatus())
                .map(PluginServiceStatus::getDeploymentStatus)
                .map(DeploymentStatus::getAvailableReplicas)
                .orElse(0);
        response.setOnline(EntandoDeploymentPhase.SUCCESSFUL.equals(deploymentPhase) && availableReplicas > 0);

        return response;
    }

    private PluginServiceStatus convert(final AbstractServerStatus status, final String type) {
        final PluginServiceStatus serviceStatus = new PluginServiceStatus();
        final PodStatus podStatus = new PodStatus();
        final DeploymentStatus deploymentStatus = new DeploymentStatus();

        serviceStatus.setReplicas(status.getDeploymentStatus().getReplicas());
        serviceStatus.setType(type);
        serviceStatus.setVolumePhase(status.getPersistentVolumeClaimStatuses().stream().map(
                PersistentVolumeClaimStatus::getPhase)
                .collect(Collectors.joining(", ")));
        serviceStatus.setPodStatus(podStatus);
        serviceStatus.setDeploymentStatus(deploymentStatus);

        podStatus.setPhase(status.getPodStatus().getPhase());
        podStatus.setContainerStatuses(status.getPodStatus().getInitContainerStatuses());
        podStatus.setConditions(sort(status.getPodStatus().getConditions(), comparing(this::localDateTime)));

        deploymentStatus.setAvailableReplicas(status.getDeploymentStatus().getAvailableReplicas());
        deploymentStatus.setReadyReplicas(status.getDeploymentStatus().getReadyReplicas());
        deploymentStatus.setUpdatedReplicas(status.getDeploymentStatus().getUpdatedReplicas());
        deploymentStatus.setReplicas(status.getDeploymentStatus().getReplicas());
        deploymentStatus
                .setConditions(sort(status.getDeploymentStatus().getConditions(), comparing(this::localDateTime)));

        return serviceStatus;
    }

    private LocalDateTime localDateTime(final PodCondition condition) {
        return LocalDateTime.parse(condition.getLastTransitionTime(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private LocalDateTime localDateTime(final DeploymentCondition condition) {
        return LocalDateTime.parse(condition.getLastTransitionTime(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private <T> List<T> sort(final List<T> list, final Comparator<T> comparator) {
        final List<T> sortedList = new ArrayList<>(list);
        sortedList.sort(comparator);
        return sortedList;
    }

    private NonNamespaceOperation<EntandoPlugin, EntandoPluginList, DoneableEntandoPlugin, Resource<EntandoPlugin, DoneableEntandoPlugin>> getOperation() {
        final CustomResourceDefinition entandoPluginCrd = client.customResourceDefinitions()
                .withName(ENTANDOPLUGIN_CRD_NAME).get();
        return client
                .customResources(entandoPluginCrd, EntandoPlugin.class, EntandoPluginList.class,
                        DoneableEntandoPlugin.class)
                .inNamespace(entandoAppNamespace);
    }

}
