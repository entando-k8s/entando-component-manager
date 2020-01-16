package org.entando.kubernetes.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.link.EntandoAppPluginLink;
import org.entando.kubernetes.model.link.EntandoAppPluginLinkBuilder;
import org.entando.kubernetes.model.plugin.EntandoPlugin;

public class K8SServiceClientTestDouble implements K8SServiceClient {

    private List<EntandoAppPluginLink> inMemoryLinks = new ArrayList<>();
    private List<EntandoPlugin> inMemoryPlugins = new ArrayList<>();
    private List<EntandoDeBundle> inMemoryBundles = new ArrayList<>();

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
        this.inMemoryLinks = new ArrayList<>();
        this.inMemoryBundles = new ArrayList<>();
    }

    public List<EntandoAppPluginLink> getInMemoryLinkCopy() {
        return new ArrayList<>(inMemoryLinks);
    }
    public List<EntandoPlugin> getInMemoryPluginsCopy() {
        return new ArrayList<>(inMemoryPlugins);
    }

    @Override
    public List<EntandoAppPluginLink> getAppLinkedPlugins(String entandoAppName, String entandoAppNamespace) {
        return this.inMemoryLinks.stream().filter(link ->
                link.getSpec().getEntandoAppName().equals(entandoAppName) &&
                        link.getSpec().getEntandoAppNamespace().equals(entandoAppNamespace))
                .collect(Collectors.toList());

    }

    @Override
    public EntandoPlugin getPluginForLink(EntandoAppPluginLink el) {
        return null;
    }

    @Override
    public void unlink(EntandoAppPluginLink el) {
        //Don't do anything atm
    }

    @Override
    public void linkAppWithPlugin(String name, String namespace, EntandoPlugin plugin) {
        this.inMemoryPlugins.add(plugin);
        EntandoAppPluginLink link = new EntandoAppPluginLinkBuilder()
                .withNewMetadata()
                    .withName(name + "-to-" + plugin.getMetadata().getName() + "-link")
                    .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                    .withEntandoApp(namespace,name)
                    .withEntandoPlugin(plugin.getMetadata().getNamespace(), plugin.getMetadata().getName())
                .endSpec()
                .build();
        this.addInMemoryLink(link);
    }

    @Override
    public List<EntandoDeBundle> getBundlesInDefaultNamespace() {
        return inMemoryBundles.stream()
                .filter(b -> b.getMetadata().getNamespace().equals("entando-de-bundles"))
                .collect(Collectors.toList());
    }

    @Override
    public List<EntandoDeBundle> getBundlesInNamespace(String namespace) {
        return inMemoryBundles.stream()
                .filter(b -> b.getMetadata().getNamespace().equals(namespace))
                .collect(Collectors.toList());
    }

    @Override
    public List<EntandoDeBundle> getBundlesInNamespaces(List<String> namespaces) {
        return inMemoryBundles.stream()
                .filter(b -> namespaces.contains(b.getMetadata().getNamespace()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<EntandoDeBundle> getBundleWithName(String name) {
        return inMemoryBundles.stream().filter(b -> b.getSpec().getDetails().getName().equals(name)).findAny();
    }

    @Override
    public Optional<EntandoDeBundle> getBundleWithNameAndNamespace(String name, String namespace) {
        return inMemoryBundles.stream()
                .filter(b -> b.getSpec().getDetails().getName().equals(name) && b.getMetadata().getNamespace().equals(namespace))
                .findFirst();
    }


}
