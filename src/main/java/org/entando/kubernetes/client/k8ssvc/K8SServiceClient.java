package org.entando.kubernetes.client.k8ssvc;

import java.util.List;
import java.util.Optional;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.link.EntandoAppPluginLink;
import org.entando.kubernetes.model.plugin.EntandoPlugin;

public interface K8SServiceClient {

    public static final String DEFAULT_BUNDLE_NAMESPACE = "entando-de-bundles";

    List<EntandoAppPluginLink> getAppLinkedPlugins(String entandoAppName, String entandoAppNamespace);

    EntandoPlugin getPluginForLink(EntandoAppPluginLink el);

    void unlink(EntandoAppPluginLink el);

    void linkAppWithPlugin(String name, String namespace, EntandoPlugin plugin);

    List<EntandoDeBundle> getBundlesInAllNamespaces();

    List<EntandoDeBundle> getBundlesInNamespace(String namespace);

    List<EntandoDeBundle> getBundlesInNamespaces(List<String> namespaces);

    Optional<EntandoDeBundle> getBundleWithNameAndNamespace(String name, String namespace);

}
