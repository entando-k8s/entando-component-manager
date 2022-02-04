package org.entando.kubernetes.model.bundle.processor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.config.AppConfiguration;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.installable.PluginInstallable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.reportable.EntandoK8SServiceReportableProcessor;
import org.entando.kubernetes.model.bundle.reportable.Reportable;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.validator.PluginDescriptorValidator;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

    private final KubernetesService kubernetesService;
    private final PluginDescriptorValidator pluginDescriptorValidator;

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
        return this.process(bundleReader, InstallAction.CREATE, new InstallPlan());
    }

    @Override
    public List<Installable<PluginDescriptor>> process(BundleReader bundleReader, InstallAction conflictStrategy,
            InstallPlan installPlan) {

        List<Installable<PluginDescriptor>> installableList = new ArrayList<>();

        try {
            final List<String> descriptorList = getDescriptorList(bundleReader);

            for (String filename : descriptorList) {
                PluginDescriptor plugin = bundleReader.readDescriptorFile(filename, PluginDescriptor.class);
                plugin.setBundleId(bundleReader.getEntandoDeBundleId());
                plugin.setFullDeploymentName(
                        composeFullDeploymentNameAndTruncateIfNeeded(plugin, plugin.getBundleId()));
                pluginDescriptorValidator.validateOrThrow(plugin);
                logDescriptorWarnings(plugin);
                InstallAction action = extractInstallAction(plugin.getComponentKey().getKey(), conflictStrategy,
                        installPlan);
                installableList.add(new PluginInstallable(kubernetesService, plugin, action));
            }
        } catch (IOException e) {
            throw makeMeaningfulException(e);
        }

        return installableList;
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

    @Override
    public Reportable getReportable(BundleReader bundleReader, ComponentProcessor<?> componentProcessor) {

        List<String> idList = new ArrayList<>();

        try {
            List<String> contentDescriptorList = componentProcessor.getDescriptorList(bundleReader);
            for (String fileName : contentDescriptorList) {

                PluginDescriptor pluginDescriptor = (PluginDescriptor) bundleReader
                        .readDescriptorFile(fileName, componentProcessor.getDescriptorClass());
                pluginDescriptor.setBundleId(bundleReader.getEntandoDeBundleId());
                pluginDescriptor.setFullDeploymentName(composeFullDeploymentNameAndTruncateIfNeeded(pluginDescriptor,
                        pluginDescriptor.getBundleId()));
                logDescriptorWarnings(pluginDescriptor);
                idList.add(pluginDescriptor.getComponentKey().getKey());
            }

            return new Reportable(componentProcessor.getSupportedComponentType(), idList,
                    this.getReportableRemoteHandler());

        } catch (IOException e) {
            throw new EntandoComponentManagerException(String.format("Error generating Reportable for %s components",
                    componentProcessor.getSupportedComponentType().getTypeName()), e);
        }
    }


    private void logDescriptorWarnings(PluginDescriptor descriptor) {

        // deprecated descriptor
        if (descriptor.isVersion1()) {
            log.warn(DEPRECATED_DESCRIPTOR, descriptor.getSpec().getImage());
        }

        // plugin base name too long
        String deploymentBaseName = descriptor.getComponentKey().getKey();
        if (deploymentBaseName.length() > BundleUtilities.MAX_ENTANDO_K8S_POD_NAME_LENGTH) {

            String errMessage = descriptor.isVersion1()
                    ? DEPLOYMENT_BASE_NAME_MAX_LENGHT_TRUNCATED_V1 :
                    DEPLOYMENT_BASE_NAME_MAX_LENGHT_TRUNCATED_V2;

            log.warn(errMessage,
                    descriptor.getDockerImage(),
                    BundleUtilities.MAX_ENTANDO_K8S_POD_NAME_LENGTH,
                    this.truncatePodPrefixName(deploymentBaseName));
        }
    }

    public String composeFullDeploymentNameAndTruncateIfNeeded(PluginDescriptor descriptor, String bundleId) {

        String deploymentBaseName;

        if (StringUtils.hasLength(descriptor.getDeploymentBaseName())) {
            deploymentBaseName = BundleUtilities.makeKubernetesCompatible(descriptor.getDeploymentBaseName());
        } else {
            deploymentBaseName = BundleUtilities.composeNameFromDockerImage(descriptor.getDockerImage());
        }

        String fullDeploymentName = deploymentBaseName + "-" + BundleUtilities.makeKubernetesCompatible(bundleId);

        if (AppConfiguration.isTruncatePluginBaseNameIfLonger()) {
            fullDeploymentName = this.truncatePodPrefixName(fullDeploymentName);
        }

        return fullDeploymentName;
    }

    protected String truncatePodPrefixName(String podPrefixName) {
        if (podPrefixName.length() > pluginDescriptorValidator.getFullDeploymentNameMaxlength()) {

            podPrefixName = podPrefixName
                    .substring(0, Math.min(pluginDescriptorValidator.getFullDeploymentNameMaxlength(), podPrefixName.length()))
                    .replaceAll("-$", "");        // remove a possible ending hyphen
        }

        return podPrefixName;
    }

    public static final String DEPRECATED_DESCRIPTOR = "The descriptor for plugin with docker image "
            + "'{}' uses a deprecated format. To have full control over plugins we encourage you to migrate "
            + "to the new plugin descriptor format.";
    public static final String DEPLOYMENT_BASE_NAME_MAX_LENGHT_TRUNCATED_V1 =
            "The prefix of the pod using the docker image "
                    + "'{}' is longer than {}. The prefix has been created using the format "
                    + "[docker-organization]-[docker-image-name]-[docker-image-version]. "
                    + "Plugin pods names will be truncated to '{}'";
    public static final String DEPLOYMENT_BASE_NAME_MAX_LENGHT_TRUNCATED_V2 =
            "The prefix of the pod using the docker image "
                    + "'{}' is longer than {}. Plugin pods names will be truncated to '{}'";


}
