package org.entando.kubernetes.model.bundle.processor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor.DescriptorMetadata;
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

    public static final String PLUGIN_DEPLOYMENT_PREFIX = "pn-";

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
                PluginDescriptor pluginDescriptor = bundleReader.readDescriptorFile(filename, PluginDescriptor.class);
                setPluginMetadata(pluginDescriptor, bundleReader);
                pluginDescriptorValidator.validateOrThrow(pluginDescriptor);
                logDescriptorWarnings(pluginDescriptor);
                InstallAction action = extractInstallAction(pluginDescriptor.getComponentKey().getKey(),
                        conflictStrategy,
                        installPlan);
                installableList.add(new PluginInstallable(kubernetesService, pluginDescriptor, action));
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
        return new PluginDescriptor()
                .setDescriptorMetadata(new DescriptorMetadata(null, component.getComponentId()));
    }

    @Override
    public Reportable getReportable(BundleReader bundleReader, ComponentProcessor<?> componentProcessor) {

        List<String> idList = new ArrayList<>();

        try {
            List<String> contentDescriptorList = componentProcessor.getDescriptorList(bundleReader);
            for (String fileName : contentDescriptorList) {

                PluginDescriptor pluginDescriptor = (PluginDescriptor) bundleReader
                        .readDescriptorFile(fileName, componentProcessor.getDescriptorClass());
                setPluginMetadata(pluginDescriptor, bundleReader);
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

    private void setPluginMetadata(PluginDescriptor pluginDescriptor, BundleReader bundleReader) {

        final String url = BundleUtilities.removeProtocolFromUrl(bundleReader.getBundleUrl());
        final String bundleId = BundleUtilities.signBundleId(url);

        pluginDescriptor.setDescriptorMetadata(
                bundleId,
                generateFullDeploymentName(pluginDescriptor, bundleId));
    }



    private void logDescriptorWarnings(PluginDescriptor descriptor) {

        // deprecated descriptor
        if (descriptor.isVersion1()) {
            log.warn(DEPRECATED_DESCRIPTOR, descriptor.getSpec().getImage());
        }
    }

    /**
     * generate the full deployment name (deployment-name + bundleId) the deployment name is generated starting by the.
     * deployment base name or by the docker image apply a truncation if required
     *
     * @param descriptor the PluginDescriptor from which read data
     * @param bundleId   the bundle identifier to concatenate to the deployment name
     * @return the generated full deployment name
     */
    public String generateFullDeploymentName(PluginDescriptor descriptor, String bundleId) {

        String deploymentBaseName;
        String inputValueForSha;

        if (StringUtils.hasLength(descriptor.getDeploymentBaseName())) {
            deploymentBaseName = BundleUtilities.makeKubernetesCompatible(descriptor.getDeploymentBaseName());
            inputValueForSha = descriptor.getDeploymentBaseName();
        } else {
            deploymentBaseName = BundleUtilities.composeNameFromDockerImage(descriptor.getDockerImage());
            inputValueForSha = descriptor.getDockerImage().toString();
        }

        String fullDeploymentName = PLUGIN_DEPLOYMENT_PREFIX + String.join("-",
                DigestUtils.sha256Hex(inputValueForSha).substring(0, BundleUtilities.PLUGIN_HASH_LENGTH),
                BundleUtilities.makeKubernetesCompatible(bundleId), deploymentBaseName);

        if (fullDeploymentName.length() > pluginDescriptorValidator.getFullDeploymentNameMaxlength()) {
            throw new EntandoComponentManagerException("The resulting plugin full deployment name \""
                    + fullDeploymentName + "\" exceeds the max allowed length "
                    + pluginDescriptorValidator.getFullDeploymentNameMaxlength() + ". You can configure the max length "
                    + "by setting the desired value of the environment variable FULL_DEPLOYMENT_NAME_MAXLENGTH");
        }

        return fullDeploymentName;
    }

    public static final String DEPRECATED_DESCRIPTOR = "The descriptor for plugin with docker image "
            + "'{}' uses a deprecated format. To have full control over plugins we encourage you to migrate "
            + "to the new plugin descriptor format.";
}
