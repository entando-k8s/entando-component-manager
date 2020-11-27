package org.entando.kubernetes.model.bundle.processor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.controller.digitalexchange.job.model.AnalysisReport;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallActionsByComponentType;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallRequest.InstallAction;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.installable.PluginInstallable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.reportable.EntandoK8SServiceReportableProcessor;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.springframework.stereotype.Service;

/**
 * Processor to perform a deployment on the Kubernetes Cluster.
 *
 * <p>Will read the service property on the component descriptor yaml and convert it into a EntandoPlugin custom
 * resource
 *
 * @author Sergio Marcelino
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PluginProcessor extends BaseComponentProcessor<PluginDescriptor> implements
        EntandoK8SServiceReportableProcessor {

    public static final String DEPRECATED_DESCRIPTOR = "The descriptor for plugin with docker image "
            + "'{}' uses a deprecated format. To have full control over plugin pods names we encourage you to migrate "
            + "to the new plugin descriptor format.";
    public static final String DEPLOYMENT_BASE_NAME_MAX_LENGHT_TRUNCATED =
            "The prefix of the pod using the docker image "
                    + "'{}' is longer than {}. The prefix has been created using the format "
                    + "[docker-organization]-[docker-image-name]-[docker-image-version]. Plugin pods names will be truncated to '{}'";

    private final KubernetesService kubernetesService;

    @Override
    public ComponentType getSupportedComponentType() {
        return ComponentType.PLUGIN;
    }

    @Override
    public Class<PluginDescriptor> getDescriptorClass() {
        return PluginDescriptor.class;
    }

    @Override
    public Optional<Function<ComponentSpecDescriptor, List<String>>> getComponentSelectionFn() {
        return Optional.of(ComponentSpecDescriptor::getPlugins);
    }

    @Override
    public List<Installable<PluginDescriptor>> process(BundleReader bundleReader) {
        return this.process(bundleReader, InstallAction.CREATE, new InstallActionsByComponentType(),
                new AnalysisReport());
    }

    @Override
    public List<Installable<PluginDescriptor>> process(BundleReader bundleReader, InstallAction conflictStrategy,
            InstallActionsByComponentType actions, AnalysisReport report) {
        try {
            final List<String> descriptorList = getDescriptorList(bundleReader);

            List<Installable<PluginDescriptor>> installableList = new ArrayList<>();
            for (String filename : descriptorList) {
                PluginDescriptor plugin = bundleReader.readDescriptorFile(filename, PluginDescriptor.class);
                logDescriptorWarnings(plugin);
                InstallAction action = extractInstallAction(plugin.getComponentKey().getKey(), actions,
                        conflictStrategy, report);
                installableList.add(new PluginInstallable(kubernetesService, plugin, action));
            }
            return installableList;
        } catch (IOException e) {
            throw new EntandoComponentManagerException("Error reading bundle", e);
        }
    }

    @Override
    public List<Installable<PluginDescriptor>> process(List<EntandoBundleComponentJobEntity> components) {
        return components.stream()
                .filter(c -> c.getComponentType() == getSupportedComponentType())
                .map(c -> new PluginInstallable(kubernetesService, buildDescriptorFromComponentJob(c), c.getAction()))
                .collect(Collectors.toList());
    }

    @Override
    public PluginDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component) {
        return PluginDescriptor.builder().deploymentBaseName(component.getComponentId()).build();
    }


    private void logDescriptorWarnings(PluginDescriptor descriptor) {

        if (descriptor.isVersion1()) {
            log.warn(DEPRECATED_DESCRIPTOR, descriptor.getSpec().getImage());

            String deploymentBaseName = BundleUtilities.composeNameFromDockerImage(descriptor.getDockerImage());
            if (deploymentBaseName.length() > BundleUtilities.MAX_ENTANDO_K8S_POD_NAME_LENGTH) {

                log.warn(DEPLOYMENT_BASE_NAME_MAX_LENGHT_TRUNCATED,
                        descriptor.getSpec().getImage(),
                        BundleUtilities.MAX_ENTANDO_K8S_POD_NAME_LENGTH,
                        BundleUtilities.truncatePodPrefixName(deploymentBaseName));
            }
        }
    }
}
