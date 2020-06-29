package org.entando.kubernetes.model.bundle.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.BundleReader;
import org.entando.kubernetes.model.bundle.descriptor.ComponentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.installable.PluginInstallable;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleComponentJob;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPluginBuilder;
import org.entando.kubernetes.service.KubernetesService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Processor to perform a deployment on the Kubernetes Cluster.
 * <p>
 * Will read the service property on the component descriptor yaml and convert it into a EntandoPlugin custom resource
 *
 * @author Sergio Marcelino
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PluginProcessor implements ComponentProcessor<EntandoPlugin> {

    private final KubernetesService kubernetesService;

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.PLUGIN;
    }

    @Override
    public List<Installable<EntandoPlugin>> process(BundleReader npr) {
        try {
            ComponentDescriptor descriptor = npr.readBundleDescriptor();
            Optional<List<String>> optionalPlugins = ofNullable(descriptor.getComponents())
                    .map(ComponentSpecDescriptor::getPlugins);

            List<Installable<EntandoPlugin>> installableList = new ArrayList<>();
            if (optionalPlugins.isPresent()) {
                for (String filename : optionalPlugins.get()) {
                    EntandoPlugin plugin = npr
                            .readDescriptorFile(filename, org.entando.kubernetes.model.plugin.EntandoPlugin.class);
                    installableList.add(new PluginInstallable(kubernetesService, plugin));
                }
            }
            return installableList;
        } catch (IOException e) {
            throw new EntandoComponentManagerException("Error reading bundle", e);
        }
    }

    @Override
    public List<Installable<EntandoPlugin>> process(List<EntandoBundleComponentJob> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == ComponentType.PLUGIN)
                .map(c -> new PluginInstallable(kubernetesService, this.buildDescriptorFromComponentJob(c)))
                .collect(Collectors.toList());
    }

    @Override
    public EntandoPlugin buildDescriptorFromComponentJob(EntandoBundleComponentJob component) {
        return new EntandoPluginBuilder()
                .withNewMetadata()
                .withName(component.getName())
                .endMetadata()
                .build();
    }

}
