package org.entando.kubernetes.client.k8ssvc;

import java.util.List;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.link.EntandoAppPluginLink;
import org.entando.kubernetes.model.plugin.EntandoPlugin;

public interface K8SServiceClient {

    List<EntandoAppPluginLink> getAppLinkedPlugins(String entandoAppName, String entandoAppNamespace);

    EntandoPlugin getPluginForLink(EntandoAppPluginLink el);

    void unlink(EntandoAppPluginLink el);

    void linkAppWithPlugin(String name, String namespace, EntandoPlugin plugin);

    List<EntandoDeBundle> getAvailableBundles();

}
