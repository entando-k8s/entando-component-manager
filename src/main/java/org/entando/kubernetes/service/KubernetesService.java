package org.entando.kubernetes.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.model.link.EntandoAppPluginLink;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPluginBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KubernetesService {

    public static final String ENTANDOPLUGIN_CRD_NAME = "entandoplugins.entando.org";
    public static final String KUBERNETES_NAMESPACE_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/namespace";

    private final K8SServiceClient k8sServiceClient;
    private final String entandoAppName;
    private final String entandoAppNamespace;

    public KubernetesService(K8SServiceClient k8SServiceClient,
            @Value("${entando.app.name}") String entandoAppName,
            @Value("${entando.app.namespace}") String entandoAppNamespace) {
        this.k8sServiceClient = k8SServiceClient;
        this.entandoAppName = entandoAppName;
        this.entandoAppNamespace = getCurrentKubernetesNamespace().orElse(entandoAppNamespace);
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

    public void linkPlugin(EntandoPlugin plugin) {
        EntandoPlugin newPlugin = new EntandoPluginBuilder()
                .withMetadata(plugin.getMetadata())
                .withSpec(plugin.getSpec())
                .build();

        newPlugin.getMetadata().setNamespace(null);

        k8sServiceClient.linkAppWithPlugin(entandoAppName, entandoAppNamespace, newPlugin);
    }

    private Optional<String> getCurrentKubernetesNamespace() {
        Path namespacePath = Paths.get(KUBERNETES_NAMESPACE_PATH);
        String namespace = null;
        if (namespacePath.toFile().exists()) {
            try {
                namespace = new String(Files.readAllBytes(namespacePath));
            } catch (IOException e) {
                log.error(String.format("An error occurred while reading the namespace from file %s", namespacePath.toString()), e);
            }
        }
        return Optional.ofNullable(namespace);
    }

}
