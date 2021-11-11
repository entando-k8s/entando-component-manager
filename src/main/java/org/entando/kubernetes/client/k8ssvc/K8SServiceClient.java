package org.entando.kubernetes.client.k8ssvc;

import java.util.List;
import java.util.Optional;
import org.entando.kubernetes.client.ECMClient;
import org.entando.kubernetes.client.model.AnalysisReport;
import org.entando.kubernetes.model.bundle.reportable.Reportable;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.link.EntandoAppPluginLink;
import org.entando.kubernetes.model.plugin.EntandoPlugin;

public interface K8SServiceClient extends ECMClient {

    List<EntandoAppPluginLink> getAppLinks(String entandoAppName);

    EntandoPlugin getPluginForLink(EntandoAppPluginLink el);

    Optional<EntandoPlugin> getPluginByName(String pluginName);

    void unlink(EntandoAppPluginLink el);

    EntandoPlugin updatePlugin(EntandoPlugin plugin);

    EntandoAppPluginLink linkAppWithPlugin(String name, String namespace, EntandoPlugin plugin);

    Optional<EntandoAppPluginLink> getLinkByName(String linkName);

    List<EntandoDeBundle> getBundlesInObservedNamespaces();

    List<EntandoDeBundle> getBundlesInNamespace(String namespace);

    List<EntandoDeBundle> getBundlesInNamespaces(List<String> namespaces);

    Optional<EntandoDeBundle> getBundleWithName(String name);

    Optional<EntandoDeBundle> getBundleWithNameAndNamespace(String name, String namespace);

    boolean isPluginReadyToServeApp(EntandoPlugin plugin, String appName);

    AnalysisReport getAnalysisReport(List<Reportable> reportableList);

    EntandoDeBundle deployDeBundle(EntandoDeBundle entandoDeBundle);

    void undeployDeBundle(String bundleName);
}
