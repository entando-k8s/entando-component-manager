package org.entando.kubernetes.client.k8ssvc;

import java.util.List;
import java.util.Optional;
import org.entando.kubernetes.model.bundle.EntandoComponentBundle;
import org.entando.kubernetes.model.link.EntandoAppPluginLink;
import org.entando.kubernetes.model.plugin.EntandoPlugin;

public interface K8SServiceClient {

    List<EntandoAppPluginLink> getAppLinks(String entandoAppName);

    EntandoPlugin getPluginForLink(EntandoAppPluginLink el);

    Optional<EntandoPlugin> getPluginByName(String pluginName);

    void unlink(EntandoAppPluginLink el);

    EntandoAppPluginLink linkAppWithPlugin(String name, String namespace, EntandoPlugin plugin);

    Optional<EntandoAppPluginLink> getLinkByName(String linkName);

    List<EntandoComponentBundle> getBundlesInObservedNamespaces();

    List<EntandoComponentBundle> getBundlesInNamespace(String namespace);

    List<EntandoComponentBundle> getBundlesInNamespaces(List<String> namespaces);

    Optional<EntandoComponentBundle> getBundleWithName(String name);

    Optional<EntandoComponentBundle> getBundleWithNameAndNamespace(String name, String namespace);

    boolean isPluginReadyToServeApp(EntandoPlugin plugin, String appName);

}
