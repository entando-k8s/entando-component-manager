package org.entando.kubernetes.model.bundle.processor;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.bundle.ZipReader;
import org.entando.kubernetes.model.bundle.descriptor.ComponentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PluginDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJobComponent;
import org.entando.kubernetes.service.KubernetesService;
import org.springframework.stereotype.Service;

/**
 * Processor to perform a deployment on the Kubernetes Cluster. Will read the service property on the component descriptor yaml and convert
 * it into a EntandoPlugin custom resource.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class PluginProcessor implements ComponentProcessor {

    private final KubernetesService kubernetesService;

    @Override
    public List<Installable> process(DigitalExchangeJob job,
            ZipReader zipReader,
            ComponentDescriptor descriptor) throws IOException {
        Optional<List<String>> optionalPlugins = ofNullable(descriptor.getComponents())
                .map(ComponentSpecDescriptor::getPlugins);
        List<Installable> installableList = new ArrayList<>();
        if (optionalPlugins.isPresent()) {
            for (String filename : optionalPlugins.get()) {
                PluginDescriptor pluginDescriptor = zipReader.readPluginDescriptor(filename);
                installableList.add(new PluginInstallable(pluginDescriptor, job));
            }
        }
        return installableList;
    }

    @Override
    public boolean shouldProcess(final ComponentType componentType) {
        return componentType == ComponentType.PLUGIN;
    }

    @Override
    public void uninstall(final DigitalExchangeJobComponent component) {
        log.info("Removing deployment {}", component.getName());
        kubernetesService.unlinkPlugin(component.getName());
    }

    public class PluginInstallable extends Installable<PluginDescriptor> {

        private final DigitalExchangeJob job;

        public PluginInstallable(final PluginDescriptor plugin,
                final DigitalExchangeJob job) {
            super(plugin);
            this.job = job;
        }

        @Override
        public CompletableFuture install() {
            return CompletableFuture.runAsync(() -> {
                log.info("Deploying a new plugin {}", representation.getSpec().getImage());
                kubernetesService.linkPlugin(representation);
            });
        }

        @Override
        public ComponentType getComponentType() {
            return ComponentType.PLUGIN;
        }

        @Override
        public String getName() {
            return job.getComponentId();
        }

    }
}
