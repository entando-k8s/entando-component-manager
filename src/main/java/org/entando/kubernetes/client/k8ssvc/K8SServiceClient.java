package org.entando.kubernetes.client.k8ssvc;

import java.util.List;
import java.util.Optional;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.link.EntandoAppPluginLink;
import org.entando.kubernetes.model.plugin.EntandoPlugin;

public interface K8SServiceClient {

    List<EntandoAppPluginLink> getAppLinks(String entandoAppName);

    EntandoPlugin getPluginForLink(EntandoAppPluginLink el);

    void unlink(EntandoAppPluginLink el);

    void linkAppWithPlugin(String name, String namespace, EntandoPlugin plugin);

    List<EntandoDeBundle> getBundlesInObservedNamespaces();

    List<EntandoDeBundle> getBundlesInNamespace(String namespace);

    List<EntandoDeBundle> getBundlesInNamespaces(List<String> namespaces);

    Optional<EntandoDeBundle> getBundleWithName(String name);

    Optional<EntandoDeBundle> getBundleWithNameAndNamespace(String name, String namespace);

    boolean isPluginReadyToServeApp(EntandoPlugin plugin, String appName);

}
