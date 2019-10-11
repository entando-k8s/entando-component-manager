package org.entando.kubernetes.client;

import java.util.List;
import org.entando.kubernetes.model.link.EntandoAppPluginLink;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.springframework.web.client.RestTemplate;

public interface K8SServiceClient {

    List<EntandoAppPluginLink> getAppLinkedPlugins(String entandoAppName, String entandoAppNamespace);

    EntandoPlugin getPluginForLink(EntandoAppPluginLink el);

    void unlink(EntandoAppPluginLink el);

    void linkAppWithPlugin(String name, String namespace, EntandoPlugin plugin);

}
