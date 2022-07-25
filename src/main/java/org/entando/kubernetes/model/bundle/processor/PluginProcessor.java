package org.entando.kubernetes.model.bundle.processor;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.plugin.EnvironmentVariable;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor.DescriptorMetadata;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.installable.PluginInstallable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.bundle.reportable.EntandoK8SServiceReportableProcessor;
import org.entando.kubernetes.model.bundle.reportable.Reportable;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.repository.PluginDataRepository;
import org.entando.kubernetes.service.KubernetesService;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.validator.descriptor.PluginDescriptorValidator;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;

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
public class PluginProcessor extends BaseComponentProcessor<PluginDescriptor> implements
        EntandoK8SServiceReportableProcessor {

    public static final String PLUGIN_DEPLOYMENT_PREFIX = "pn-";
    public static final String SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_OIDC_ISSUER_URI =
            "SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_OIDC_ISSUER_URI";
    public static final String SERVER_SERVLET_CONTEXT_PATH = "SERVER_SERVLET_CONTEXT_PATH";
    public static final String ENTANDO_ECR_INGRESS_URL = "ENTANDO_ECR_INGRESS_URL";


    private final String cmEndpoint;

    private final KubernetesService kubernetesService;
    private final PluginDescriptorValidator descriptorValidator;
    private final PluginDataRepository pluginPathRepository;

    public PluginProcessor(KubernetesService kubernetesService,
            PluginDescriptorValidator descriptorValidator,
            PluginDataRepository pluginPathRepository) {
        this.kubernetesService = kubernetesService;
        this.descriptorValidator = descriptorValidator;
        this.pluginPathRepository = pluginPathRepository;
        this.cmEndpoint = composeCmEndpoint();
    }


    private String composeCmEndpoint() {

        String env = System.getenv(SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_OIDC_ISSUER_URI);
        if (ObjectUtils.isEmpty(env)) {
            return "";
        }

        final String contextPath = System.getenv(SERVER_SERVLET_CONTEXT_PATH);
        try {
            final URL cmUrl = new URL(env);
            return new DefaultUriBuilderFactory().builder()
                    .scheme(cmUrl.getProtocol())
                    .host(cmUrl.getHost())
                    .port(cmUrl.getPort())
                    .path(contextPath)
                    .build()
                    .toString();
        } catch (MalformedURLException e) {
            throw new EntandoComponentManagerException("Cannot compose " + ENTANDO_ECR_INGRESS_URL);
        }
    }

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
                // parse descriptor
                PluginDescriptor pluginDescriptor = bundleReader.readDescriptorFile(filename, PluginDescriptor.class);
                // set metadata
                setPluginMetadata(pluginDescriptor, bundleReader);
                // validate
                descriptorValidator.validateOrThrow(pluginDescriptor);
                // add CM endpoint env var
                final List<EnvironmentVariable> environmentVariables = Optional.ofNullable(
                        pluginDescriptor.getEnvironmentVariables()).orElseGet(ArrayList::new);
                environmentVariables.add(new EnvironmentVariable().setName(ENTANDO_ECR_INGRESS_URL)
                                .setValue(cmEndpoint));
                pluginDescriptor.setEnvironmentVariables(environmentVariables);
                // log
                logDescriptorWarnings(pluginDescriptor);
                // get install action
                InstallAction action = extractInstallAction(pluginDescriptor.getComponentKey().getKey(),
                        conflictStrategy,
                        installPlan);
                // add to installables
                installableList.add(new PluginInstallable(kubernetesService, pluginDescriptor, action,
                        pluginPathRepository));
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
                .map(c -> new PluginInstallable(kubernetesService, buildDescriptorFromComponentJob(c), c.getAction(),
                        pluginPathRepository))
                .collect(Collectors.toList());
    }

    @Override
    public PluginDescriptor buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component) {
        return new PluginDescriptor()
                .setDescriptorMetadata(
                        new DescriptorMetadata(null, null, null, null, component.getComponentId(), null, null));
    }

    @Override
    public Reportable getReportable(BundleReader bundleReader, ComponentProcessor<?> componentProcessor) {

        List<String> idList = new ArrayList<>();

        try {
            List<String> contentDescriptorList = componentProcessor.getDescriptorList(bundleReader);
            for (String fileName : contentDescriptorList) {
                // parse descriptor
                PluginDescriptor pluginDescriptor = (PluginDescriptor) bundleReader
                        .readDescriptorFile(fileName, componentProcessor.getDescriptorClass());
                // ensure version
                descriptorValidator.ensureDescriptorVersionIsSet(pluginDescriptor);
                // set plugin metadata
                setPluginMetadata(pluginDescriptor, bundleReader);
                // log
                logDescriptorWarnings(pluginDescriptor);
                // add plugin id to the list
                idList.add(pluginDescriptor.getComponentKey().getKey());
            }

            return new Reportable(componentProcessor.getSupportedComponentType(), idList,
                    this.getReportableRemoteHandler());

        } catch (IOException e) {
            throw new EntandoComponentManagerException(String.format("Error generating Reportable for %s components",
                    componentProcessor.getSupportedComponentType().getTypeName()), e);
        }
    }

    private void setPluginMetadata(PluginDescriptor pluginDescriptor, BundleReader bundleReader) throws IOException {

        final BundleDescriptor bundleDescriptor = bundleReader.readBundleDescriptor();

        final String url = BundleUtilities.removeProtocolFromUrl(bundleReader.getBundleUrl());
        final String bundleId = BundleUtilities.getBundleId(url);
        final String bundleCode = BundleUtilities.composeDescriptorCode(bundleDescriptor.getCode(),
                bundleDescriptor.getName(), bundleDescriptor, bundleReader.getBundleUrl());
        final String signedPluginDeplName = this.signPluginDeploymentName(pluginDescriptor);
        final String endpoint = BundleUtilities.extractIngressPathFromDescriptor(pluginDescriptor, bundleCode);
        final String customEndpoint = BundleUtilities.composeIngressPathFromIngressPathProperty(pluginDescriptor);

        pluginDescriptor.setDescriptorMetadata(
                bundleId,
                bundleCode,
                signedPluginDeplName.split("-")[0],
                ObjectUtils.isEmpty(pluginDescriptor.getName())
                        ? signedPluginDeplName.split("-", 2)[1]
                        : pluginDescriptor.getName(),
                generateFullDeploymentName(bundleId, signedPluginDeplName),
                endpoint,
                customEndpoint);
    }


    private void logDescriptorWarnings(PluginDescriptor descriptor) {

        // deprecated descriptor
        if (descriptor.isVersion1()) {
            log.warn(DEPRECATED_DESCRIPTOR, descriptor.getSpec().getImage());
        }
    }

    /**
     * generate a signed plugin deployment name starting by the deployment base name or by the docker image.
     *
     * @param descriptor the PluginDescriptor from which read data
     * @return the generated signed plugin deployment name
     */
    public String signPluginDeploymentName(PluginDescriptor descriptor) {

        String deploymentBaseName;
        String inputValueForSha;

        if (StringUtils.hasLength(descriptor.getDeploymentBaseName())) {
            deploymentBaseName = BundleUtilities.makeKubernetesCompatible(descriptor.getDeploymentBaseName());
            inputValueForSha = descriptor.getDeploymentBaseName();
        } else {
            deploymentBaseName = BundleUtilities.composeNameFromDockerImage(descriptor.getDockerImage());
            inputValueForSha = String.format("%s/%s", descriptor.getDockerImage().getOrganization(),
                    descriptor.getDockerImage().getName());
        }

        return String.join("-",
                DigestUtils.sha256Hex(inputValueForSha).substring(0, BundleUtilities.PLUGIN_HASH_LENGTH),
                deploymentBaseName);
    }

    /**
     * generate the full deployment name (deployment-name + bundleId) the deployment name is generated starting by the.
     * deployment base name or by the docker image apply a truncation if required
     *
     * @param bundleId         the bundle identifier to concatenate to the deployment name
     * @param signedPluginName the plugin identifier (hash-name) to concatenate to the deployment name
     * @return the generated full deployment name
     */
    public String generateFullDeploymentName(String bundleId, String signedPluginName) {

        String fullDeploymentName = PLUGIN_DEPLOYMENT_PREFIX + String.join("-",
                BundleUtilities.makeKubernetesCompatible(bundleId),
                signedPluginName);

        if (fullDeploymentName.length() > descriptorValidator.getFullDeploymentNameMaxlength()) {
            throw new EntandoComponentManagerException("The resulting plugin full deployment name \""
                    + fullDeploymentName + "\" exceeds the max allowed length "
                    + descriptorValidator.getFullDeploymentNameMaxlength() + ". You can configure the max length "
                    + "by setting the desired value of the environment variable FULL_DEPLOYMENT_NAME_MAXLENGTH");
        }

        return fullDeploymentName;
    }

    public static final String DEPRECATED_DESCRIPTOR = "The descriptor for plugin with docker image "
            + "'{}' uses a deprecated format. To have full control over plugins we encourage you to migrate "
            + "to the new plugin descriptor format.";
}
