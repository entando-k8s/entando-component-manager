package org.entando.kubernetes.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.entando.kubernetes.model.link.EntandoAppPluginLink;
import org.entando.kubernetes.model.link.EntandoAppPluginLinkBuilder;
import org.entando.kubernetes.model.plugin.EntandoPlugin;

public class K8SServiceClientTestDouble implements K8SServiceClient{

    private List<EntandoAppPluginLink> inMemoryLinks = new ArrayList<>();


    public void addInMemoryLink(EntandoAppPluginLink link) {
        this.inMemoryLinks.add(link);
    }

    public void cleanInMemoryDatabase() {
        this.inMemoryLinks = new ArrayList<>();
    }

    public List<EntandoAppPluginLink> getInMemoryDatabaseCopy() {
        return new ArrayList<>(inMemoryLinks);
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
}
