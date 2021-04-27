package org.entando.kubernetes.model.bundle.processor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.installable.PluginInstallable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.reportable.EntandoK8SServiceReportableProcessor;
import org.entando.kubernetes.model.bundle.reportable.Reportable;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.model.plugin.PluginSecurityLevel;
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
                plugin = ensurePluginDescriptorVersionIsSet(plugin);
                validateDescriptorOrThrow(plugin);
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
        String deploymentBaseName = descriptor.generateDeploymentBaseNameNotTruncated();
        if (deploymentBaseName.length() > BundleUtilities.MAX_ENTANDO_K8S_POD_NAME_LENGTH) {

            String errMessage = descriptor.isVersion1()
                    ? DEPLOYMENT_BASE_NAME_MAX_LENGHT_TRUNCATED_V1 :
                    DEPLOYMENT_BASE_NAME_MAX_LENGHT_TRUNCATED_V2;

            log.warn(errMessage,
                    descriptor.getDockerImage(),
                    BundleUtilities.MAX_ENTANDO_K8S_POD_NAME_LENGTH,
                    BundleUtilities.truncatePodPrefixName(deploymentBaseName));
        }
    }

    private PluginDescriptor ensurePluginDescriptorVersionIsSet(PluginDescriptor descriptor) {
        if (StringUtils.isEmpty(descriptor.getDescriptorVersion())) {
            Integer intVersion = BundleUtilities.getPluginDescriptorIntegerVersion(descriptor);
            descriptor.setDescriptorVersion(BundleUtilities.composePluginDescriptorVersion(intVersion));
        }

        return descriptor;
    }

    private void validateDescriptorOrThrow(PluginDescriptor descriptor) {

        // validate version
        Matcher matcher = Pattern.compile(BundleUtilities.PLUGIN_DESCRIPTOR_VERSION_REGEXP).matcher(descriptor.getDescriptorVersion());
        if (!matcher.matches()) {
            String error = String.format(VERSION_NOT_VALID, descriptor.getComponentKey().getKey());
            log.debug(error);
            throw new InvalidBundleException(error);
        }

        // validate securityLevel property
        if (!StringUtils.isEmpty(descriptor.getSecurityLevel())
                || (descriptor.isVersion1() && !StringUtils.isEmpty(descriptor.getSpec().getSecurityLevel()))) {

            String securityLevel =
                    descriptor.isVersion1() ? descriptor.getSpec().getSecurityLevel() : descriptor.getSecurityLevel();

            Arrays.stream(PluginSecurityLevel.values())
                    .filter(pluginSecurityLevel -> pluginSecurityLevel.toName().equals(securityLevel))
                    .findFirst()
                    .orElseThrow(() -> new InvalidBundleException(SECURITY_LEVEL_NOT_RECOGNIZED)); // NOSONAR
        }
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
    public static final String SECURITY_LEVEL_NOT_RECOGNIZED =
            "The received plugin descriptor contains an unknown securityLevel. Accepted values are: "
                    + Arrays.stream(PluginSecurityLevel.values()).map(PluginSecurityLevel::toName)
                    .collect(Collectors.joining(", "));
    public static final String VERSION_NOT_VALID =
            "The plugin %s descriptor contains an invalid descriptorVersion";
}
