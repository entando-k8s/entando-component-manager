package org.entando.kubernetes.client;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.client.model.AnalysisReport;
import org.entando.kubernetes.exception.k8ssvc.PluginNotFoundException;
import org.entando.kubernetes.model.bundle.reportable.Reportable;
import org.entando.kubernetes.model.common.EntandoDeploymentPhase;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.link.EntandoAppPluginLink;
import org.entando.kubernetes.model.link.EntandoAppPluginLinkBuilder;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.stubhelper.AnalysisReportStubHelper;

public class K8SServiceClientTestDouble implements K8SServiceClient {

    private Set<EntandoAppPluginLink> inMemoryLinks = new HashSet<>();
    private Set<EntandoPlugin> inMemoryPlugins = new HashSet<>();
    private Set<EntandoDeBundle> inMemoryBundles = new HashSet<>();
    private EntandoDeploymentPhase deployedLinkPhase = EntandoDeploymentPhase.SUCCESSFUL;

    public void addInMemoryLinkedPlugins(EntandoPlugin plugin) {
        this.inMemoryPlugins.add(plugin);
    }

    public void addInMemoryLink(EntandoAppPluginLink link) {
        this.inMemoryLinks.add(link);
    }

    public void addInMemoryBundle(EntandoDeBundle bundle) {
        this.inMemoryBundles.add(bundle);
    }

    public void cleanInMemoryDatabases() {
        this.inMemoryLinks = new HashSet<>();
        this.inMemoryPlugins = new HashSet<>();
        this.inMemoryBundles = new HashSet<>();
    }

    public Set<EntandoAppPluginLink> getInMemoryLinks() {
        return inMemoryLinks;
    }

    public Set<EntandoPlugin> getInMemoryPlugins() {
        return inMemoryPlugins;
    }

    public EntandoDeploymentPhase setDeployedLinkPhase(EntandoDeploymentPhase phase) {
        this.deployedLinkPhase = phase;
        return this.deployedLinkPhase;
    }

    @Override
    public List<EntandoAppPluginLink> getAppLinks(String entandoAppName) {
        return this.inMemoryLinks.stream().filter(link ->
                        link.getSpec().getEntandoAppName().equals(entandoAppName))
                .collect(Collectors.toList());

    }

    @Override
    public EntandoPlugin getPluginForLink(EntandoAppPluginLink el) {
        return this.inMemoryPlugins.stream()
                .filter(p -> el.getSpec().getEntandoPluginName().equals(p.getMetadata().getName()))
                .findFirst().orElseThrow(PluginNotFoundException::new);
    }

    @Override
    public Optional<EntandoPlugin> getPluginByName(String pluginName) {
        return this.inMemoryPlugins.stream().filter(p -> p.getMetadata().getName().equals(pluginName)).findFirst();
    }

    @Override
    public void unlink(EntandoAppPluginLink el) {

    }

    @Override
    public void unlinkAndScaleDown(EntandoAppPluginLink el) {
        //Don't do anything atm
    }

    @Override
    public void removeIngressPathForPlugin(String pluginCode) {
        //Don't do anything atm
    }

    @Override
    public EntandoPlugin updatePlugin(EntandoPlugin plugin) {
        // TODO?
        return null;
    }

    @Override
    public EntandoAppPluginLink linkAppWithPlugin(String name, String namespace, EntandoPlugin plugin) {
        this.inMemoryPlugins.add(plugin);
        EntandoAppPluginLink link = new EntandoAppPluginLinkBuilder()
                .withNewMetadata()
                .withName(name + "-to-" + plugin.getMetadata().getName() + "-link")
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withEntandoApp(namespace, name)
                .withEntandoPlugin(plugin.getMetadata().getNamespace(), plugin.getMetadata().getName())
                .endSpec()
                .build();
        link.getStatus().updateDeploymentPhase(this.deployedLinkPhase, 1L);
        this.addInMemoryLink(link);
        return link;
    }

    @Override
    public Optional<EntandoAppPluginLink> getLinkByName(String linkName) {
        return inMemoryLinks.stream()
                .filter(l -> l.getMetadata().getName().equals(linkName))
                .findFirst();
    }

    @Override
    public List<EntandoDeBundle> getBundlesInObservedNamespaces(Optional<String> repoUrlFilter) {
        return inMemoryBundles.stream()
                .filter(b -> b.getMetadata().getNamespace().equals("entando-de-bundles"))
                .collect(Collectors.toList());
    }

    @Override
    public List<EntandoDeBundle> getBundlesInNamespace(String namespace, Optional<String> repoUrlFilter) {
        return inMemoryBundles.stream()
                .filter(b -> b.getMetadata().getNamespace().equals(namespace))
                .collect(Collectors.toList());
    }

    @Override
    public List<EntandoDeBundle> getBundlesInNamespaces(List<String> namespaces, Optional<String> repoUrlFilter) {
        return inMemoryBundles.stream()
                .filter(b -> namespaces.contains(b.getMetadata().getNamespace()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<EntandoDeBundle> getBundleWithName(String name) {
        return inMemoryBundles.stream()
                .filter(b -> b.getSpec().getDetails().getName().equals(name))
                .findAny();
    }

    @Override
    public Optional<EntandoDeBundle> getBundleWithNameAndNamespace(String name, String namespace) {
        return inMemoryBundles.stream()
                .filter(b -> b.getSpec().getDetails().getName().equals(name) && b.getMetadata().getNamespace()
                        .equals(namespace))
                .findFirst();
    }

    @Override
    public boolean isPluginReadyToServeApp(EntandoPlugin plugin, String appName) {
        return true;
    }

    @Override
    public AnalysisReport getAnalysisReport(List<Reportable> reportableList) {

        return AnalysisReportStubHelper.stubAnalysisReportWithPlugins();
    }

    @Override
    public EntandoDeBundle deployDeBundle(EntandoDeBundle entandoDeBundle) {
        return entandoDeBundle;
    }

    @Override
    public void undeployDeBundle(String bundleName) {

    }

    @Override
    public ApplicationStatus getAppStatusPhase(String appName) {
        ApplicationStatus appStatus = new ApplicationStatus();
        appStatus.setStatus(deployedLinkPhase.toValue());
        return appStatus;
    }

}
