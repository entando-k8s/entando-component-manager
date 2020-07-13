package org.entando.kubernetes.client;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.exception.k8ssvc.PluginNotFoundException;
import org.entando.kubernetes.model.EntandoDeploymentPhase;
import org.entando.kubernetes.model.bundle.EntandoComponentBundle;
import org.entando.kubernetes.model.link.EntandoAppPluginLink;
import org.entando.kubernetes.model.link.EntandoAppPluginLinkBuilder;
import org.entando.kubernetes.model.plugin.EntandoPlugin;

public class K8SServiceClientTestDouble implements K8SServiceClient {

    private Set<EntandoAppPluginLink> inMemoryLinks = new HashSet<>();
    private Set<EntandoPlugin> inMemoryPlugins = new HashSet<>();
    private Set<EntandoComponentBundle> inMemoryBundles = new HashSet<>();
    private EntandoDeploymentPhase deployedLinkPhase = EntandoDeploymentPhase.SUCCESSFUL;

    public void addInMemoryLinkedPlugins(EntandoPlugin plugin) {
        this.inMemoryPlugins.add(plugin);
    }

    public void addInMemoryLink(EntandoAppPluginLink link) {
        this.inMemoryLinks.add(link);
    }

    public void addInMemoryBundle(EntandoComponentBundle bundle) {
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
                .filter(p ->el.getSpec().getEntandoPluginName().equals(p.getMetadata().getName()))
                .findFirst().orElseThrow(PluginNotFoundException::new);
    }

    @Override
    public Optional<EntandoPlugin> getPluginByName(String pluginName) {
        return this.inMemoryPlugins.stream().filter(p -> p.getMetadata().getName().equals(pluginName)).findFirst();
    }

    @Override
    public void unlink(EntandoAppPluginLink el) {
        //Don't do anything atm
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
        link.getStatus().setEntandoDeploymentPhase(this.deployedLinkPhase);
        this.addInMemoryLink(link);
        return link;
    }

    @Override public Optional<EntandoAppPluginLink> getLinkByName(String linkName) {
        return inMemoryLinks.stream()
                .filter(l -> l.getMetadata().getName().equals(linkName))
                .findFirst();
    }

    @Override
    public List<EntandoComponentBundle> getBundlesInObservedNamespaces() {
        return inMemoryBundles.stream()
                .filter(b -> b.getMetadata().getNamespace().equals("entando-de-bundles"))
                .collect(Collectors.toList());
    }

    @Override
    public List<EntandoComponentBundle> getBundlesInNamespace(String namespace) {
        return inMemoryBundles.stream()
                .filter(b -> b.getMetadata().getNamespace().equals(namespace))
                .collect(Collectors.toList());
    }

    @Override
    public List<EntandoComponentBundle> getBundlesInNamespaces(List<String> namespaces) {
        return inMemoryBundles.stream()
                .filter(b -> namespaces.contains(b.getMetadata().getNamespace()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<EntandoComponentBundle> getBundleWithName(String name) {
        return inMemoryBundles.stream()
                .filter(b -> b.getSpec().getCode().equals(name))
                .findAny();
    }

    @Override
    public Optional<EntandoComponentBundle> getBundleWithNameAndNamespace(String name, String namespace) {
        return inMemoryBundles.stream()
                .filter(b -> b.getSpec().getCode().equals(name) && b.getMetadata().getNamespace()
                        .equals(namespace))
                .findFirst();
    }

    @Override
    public boolean isPluginReadyToServeApp(EntandoPlugin plugin, String appName) {
        return true;
    }

}
