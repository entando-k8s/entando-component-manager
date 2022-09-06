package org.entando.kubernetes.client.k8ssvc;

import java.util.List;
import java.util.Optional;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.entando.kubernetes.client.ECMClient;
import org.entando.kubernetes.client.model.AnalysisReport;
import org.entando.kubernetes.model.bundle.reportable.Reportable;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.link.EntandoAppPluginLink;
import org.entando.kubernetes.model.plugin.EntandoPlugin;

public interface K8SServiceClient extends ECMClient {

    String ECR_INSTALL_CAUSE_ANNOTATION = "ecr.entando.org/install-cause";
    String ECR_INSTALL_CAUSE_ANNOTATION_POSTINIT_VALUE = "postinit";
    String ECR_INSTALL_CAUSE_ANNOTATION_STANDARD_VALUE = "standard";

    List<EntandoAppPluginLink> getAppLinks(String entandoAppName);

    EntandoPlugin getPluginForLink(EntandoAppPluginLink el);

    Optional<EntandoPlugin> getPluginByName(String pluginName);

    void unlink(EntandoAppPluginLink el);

    void unlinkAndScaleDown(EntandoAppPluginLink el);

    EntandoPlugin updatePlugin(EntandoPlugin plugin);

    EntandoAppPluginLink linkAppWithPlugin(String name, String namespace, EntandoPlugin plugin);

    Optional<EntandoAppPluginLink> getLinkByName(String linkName);

    List<EntandoDeBundle> getBundlesInObservedNamespaces();

    List<EntandoDeBundle> getBundlesInObservedNamespaces(Optional<String> ecrInstallCause);

    List<EntandoDeBundle> getBundlesInNamespace(String namespace);

    List<EntandoDeBundle> getBundlesInNamespace(String namespace, Optional<String> ecrInstallCause);

    List<EntandoDeBundle> getBundlesInNamespaces(List<String> namespaces);

    List<EntandoDeBundle> getBundlesInNamespaces(List<String> namespaces, Optional<String> ecrInstallCause);

    Optional<EntandoDeBundle> getBundleWithName(String name);

    Optional<EntandoDeBundle> getBundleWithNameAndNamespace(String name, String namespace);

    boolean isPluginReadyToServeApp(EntandoPlugin plugin, String appName);

    AnalysisReport getAnalysisReport(List<Reportable> reportableList);

    EntandoDeBundle deployDeBundle(EntandoDeBundle entandoDeBundle);

    void undeployDeBundle(String bundleName);

    ApplicationStatus getAppStatusPhase(String appName);

    @Data
    @NoArgsConstructor
    class ApplicationStatus {

        private String status;
    }

}
