package org.entando.kubernetes.service;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.K8SServiceClient;
import org.entando.kubernetes.model.DbmsImageVendor;
import org.entando.kubernetes.model.EntandoPluginDeploymentRequest;
import org.entando.kubernetes.model.link.EntandoAppPluginLink;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPluginSpecBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KubernetesService {

    public static final String ENTANDOPLUGIN_CRD_NAME = "entandoplugins.entando.org";

    private final K8SServiceClient k8sServiceClient;
    private final String entandoAppName;
    private final String entandoAppNamespace;

    public KubernetesService(K8SServiceClient k8SServiceClient,
            @Value("${entando.app.name}") String entandoAppName,
            @Value("${entando.app.namespace}") String entandoAppNamespace) {
        this.k8sServiceClient = k8SServiceClient;
        this.entandoAppName = entandoAppName;
        this.entandoAppNamespace = entandoAppNamespace;
    }

    public List<EntandoPlugin> getLinkedPlugins() {
        return getCurrentAppLinkedPlugins()
                .stream().map(k8sServiceClient::getPluginForLink)
                .collect(Collectors.toList());
    }

    public EntandoPlugin getLinkedPlugin(String pluginId) {
        return getCurrentAppLinkedPlugin(pluginId)
                .map(k8sServiceClient::getPluginForLink)
                .orElseThrow(NotFoundException::new);
    }

    public boolean isLinkedPlugin(String pluginId) {
        return getCurrentAppLinkedPlugin(pluginId).isPresent();
    }

    private List<EntandoAppPluginLink> getCurrentAppLinkedPlugins() {
        return k8sServiceClient.getAppLinkedPlugins(entandoAppName, entandoAppNamespace);
    }

    private Optional<EntandoAppPluginLink> getCurrentAppLinkedPlugin(String pluginId) {
        return getCurrentAppLinkedPlugins()
                .stream()
                .filter(el -> el.getSpec().getEntandoPluginName().equals(pluginId))
                .findFirst();
    }

    public void unlinkPlugin(String pluginId) {
        getCurrentAppLinkedPlugin(pluginId).ifPresent(k8sServiceClient::unlink);
    }

    public void linkPlugin(EntandoPluginDeploymentRequest request) {
        EntandoPlugin plugin = new EntandoPlugin();
        ObjectMeta objectMeta = new ObjectMeta();

        objectMeta.setName(request.getPlugin());
        objectMeta.setNamespace(entandoAppNamespace);

        EntandoPluginSpecBuilder specBuilder = new EntandoPluginSpecBuilder()
                .withDbms(DbmsImageVendor.forValue(request.getDbms()))
                .withImage(request.getImage())
                .withIngressPath(request.getIngressPath())
                .withHealthCheckPath(request.getHealthCheckPath())
                .withReplicas(1);

        request.getRoles().forEach(r -> specBuilder.addNewRole(r.getName(), r.getCode()));
        request.getPermissions().forEach(p -> specBuilder.addNewPermission(p.getClientId(), p.getRole()));

        plugin.setMetadata(objectMeta);
        plugin.setSpec(specBuilder.build());
        plugin.setApiVersion("entando.org/v1alpha1");

        k8sServiceClient.linkAppWithPlugin(entandoAppName, entandoAppNamespace, plugin);
    }

}
