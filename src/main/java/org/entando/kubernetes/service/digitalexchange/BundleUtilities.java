package org.entando.kubernetes.service.digitalexchange;

import io.fabric8.zjsonpatch.internal.guava.Strings;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.config.AppConfiguration;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.DbmsVendor;
import org.entando.kubernetes.model.bundle.BundleType;
import org.entando.kubernetes.model.bundle.EntandoBundle;
import org.entando.kubernetes.model.bundle.descriptor.DockerImage;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptorV1Role;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPluginBuilder;
import org.entando.kubernetes.model.plugin.ExpectedRole;
import org.entando.kubernetes.model.plugin.Permission;
import org.entando.kubernetes.model.plugin.PluginSecurityLevel;
import org.springframework.util.StringUtils;

@UtilityClass
@Slf4j
public class BundleUtilities {

    public static final String OFFICIAL_SEMANTIC_VERSION_REGEX = "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-("
            + "(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\"
            + ".[0-9a-zA-Z-]+)*))?$";

    public static final int MAX_K8S_POD_NAME_LENGTH = 63;
    public static final int RESERVED_K8S_POD_NAME_LENGTH = 31;
    public static final int MAX_ENTANDO_K8S_POD_NAME_LENGTH = MAX_K8S_POD_NAME_LENGTH - RESERVED_K8S_POD_NAME_LENGTH;
    public static final String DEPLOYMENT_BASE_NAME_MAX_LENGHT_EXCEEDED_ERROR = "The prefix \"%s\" of the pod that is "
            + "about to be created is longer than %d. The prefix has been created using %s";
    public static final String DEPLOYMENT_BASE_NAME_MAX_LENGHT_ERROR_DEPLOYMENT_END = "Please specify a shorter value "
            + "in the \"deploymentBaseName\" plugin descriptor property";
    public static final String DEPLOYMENT_BASE_NAME_MAX_LENGHT_ERROR_DOCKER_IMAGE_SUFFIX = "the format "
            + "[docker-organization]-[docker-image-name]-[docker-image-version]. "
            + DEPLOYMENT_BASE_NAME_MAX_LENGHT_ERROR_DEPLOYMENT_END;
    public static final String DEPLOYMENT_BASE_NAME_MAX_LENGHT_ERROR_DEPLOYMENT_SUFFIX = "the descriptor "
            + "\"deploymentBaseName\" property. " + DEPLOYMENT_BASE_NAME_MAX_LENGHT_ERROR_DEPLOYMENT_END;

    public static final String BUNDLE_TYPE_LABEL_NAME = "bundle-type";

    public static String getBundleVersionOrFail(EntandoDeBundle bundle, String versionReference) {
        String version = versionReference;
        if (!isSemanticVersion(versionReference)) {
            version = (String) bundle.getSpec().getDetails().getDistTags().get(versionReference);
        }

        if (Strings.isNullOrEmpty(version)) {
            throw new EntandoComponentManagerException(
                    "Invalid version '" + versionReference + "' for bundle '" + bundle.getSpec().getDetails().getName()
                            + "'");
        }
        return version;
    }

    public static boolean isSemanticVersion(String versionToFind) {
        String possibleSemVer = versionToFind.startsWith("v") ? versionToFind.substring(1) : versionToFind;
        return possibleSemVer.matches(getOfficialSemanticVersionRegex());
    }

    /**
     * Check semantic version definition: https://semver.org/#is-v123-a-semantic-version
     *
     * @return The semantic version PCRE compatible regular expression
     */
    public static String getOfficialSemanticVersionRegex() {
        return OFFICIAL_SEMANTIC_VERSION_REGEX;
    }

    public static List<ExpectedRole> extractRolesFromDescriptor(PluginDescriptor descriptor) {
        return descriptor.getRoles().stream()
                .distinct()
                .map(role -> new ExpectedRole(role, role))
                .collect(Collectors.toList());
    }

    public static List<Permission> extractPermissionsFromDescriptor(PluginDescriptor descriptor) {
        return Optional.ofNullable(descriptor.getPermissions())
                .orElse(Collections.emptyList())
                .stream()
                .distinct()
                .map(permission -> new Permission(permission.getClientId(), permission.getRole()))
                .collect(Collectors.toList());
    }

    public static String extractNameFromDescriptor(PluginDescriptor descriptor) {
        return composeDeploymentBaseNameAndTruncateIfNeeded(descriptor);
    }

    public static String extractIngressPathFromDescriptor(PluginDescriptor descriptor) {
        return Optional.ofNullable(descriptor.getIngressPath())
                .orElse(composeIngressPathFromDockerImage(descriptor.getDockerImage()));
    }

    public static Map<String, String> extractLabelsFromDescriptor(PluginDescriptor descriptor) {
        DockerImage dockerImage = descriptor.getDockerImage();
        return getLabelsFromImage(dockerImage);
    }

    public static String composeDeploymentBaseName(PluginDescriptor descriptor) {

        if (StringUtils.hasLength(descriptor.getDeploymentBaseName())) {
            return makeKubernetesCompatible(descriptor.getDeploymentBaseName());
        } else {
            return composeNameFromDockerImage(descriptor.getDockerImage());
        }
    }

    private static String composeDeploymentBaseNameAndTruncateIfNeeded(PluginDescriptor descriptor) {

        String deploymentBaseName;
        String errorSuffix;

        if (StringUtils.hasLength(descriptor.getDeploymentBaseName())) {
            deploymentBaseName = makeKubernetesCompatible(descriptor.getDeploymentBaseName());
            errorSuffix = DEPLOYMENT_BASE_NAME_MAX_LENGHT_ERROR_DEPLOYMENT_SUFFIX;
        } else {
            deploymentBaseName = composeNameFromDockerImage(descriptor.getDockerImage());
            errorSuffix = DEPLOYMENT_BASE_NAME_MAX_LENGHT_ERROR_DOCKER_IMAGE_SUFFIX;
        }

        if (AppConfiguration.isTruncatePluginBaseNameIfLonger()) {
            deploymentBaseName = truncatePodPrefixName(deploymentBaseName);
        }

        return validateAndReturnDeploymentBaseName(deploymentBaseName, errorSuffix,
                AppConfiguration.isTruncatePluginBaseNameIfLonger());
    }

    /**
     * validate the deploymentBaseName. if the validation fails an EntandoComponentManagerException is thrown
     *
     * @param deploymentBaseName          the base name to use for the deployments that have to be generated in
     *                                    kubernetes
     * @param errorSuffix                 the suffix to append to the error that specifies which properties was used to
     *                                    generate the deployment base name
     * @param isTruncatePluginBaseEnabled if true the plugin base name should be truncated if it's longer than the
     *                                    admitted, so no validation is applied here
     * @return the validated string
     */
    private static String validateAndReturnDeploymentBaseName(
            String deploymentBaseName,
            String errorSuffix,
            boolean isTruncatePluginBaseEnabled) {

        // deploymentBaseName has to not be longer than 63 chars
        if (!isTruncatePluginBaseEnabled && deploymentBaseName.length() > MAX_ENTANDO_K8S_POD_NAME_LENGTH) {

            throw new EntandoComponentManagerException(
                    String.format(
                            DEPLOYMENT_BASE_NAME_MAX_LENGHT_EXCEEDED_ERROR,
                            deploymentBaseName,
                            MAX_ENTANDO_K8S_POD_NAME_LENGTH,
                            errorSuffix));
        }

        return deploymentBaseName;
    }


    public static String composeNameFromDockerImage(DockerImage image) {

        return String.join("-",
                makeKubernetesCompatible(image.getOrganization()),
                makeKubernetesCompatible(image.getName()));
    }

    public static String truncatePodPrefixName(String podPrefixName) {

        if (podPrefixName.length() > MAX_ENTANDO_K8S_POD_NAME_LENGTH) {

            podPrefixName = podPrefixName
                    .substring(0, Math.min(MAX_ENTANDO_K8S_POD_NAME_LENGTH, podPrefixName.length()))
                    .replaceAll("-$", "");        // remove a possible ending hyphen
        }

        return podPrefixName;
    }

    private static String composeIngressPathFromDockerImage(DockerImage image) {
        return "/" + String.join("/",
                makeKubernetesCompatible(image.getOrganization()),
                makeKubernetesCompatible(image.getName()),
                makeKubernetesCompatible(image.getVersion()));
    }

    public static Map<String, String> getLabelsFromImage(DockerImage dockerImage) {
        Map<String, String> labels = new HashMap<>();
        labels.put("organization", dockerImage.getOrganization());
        labels.put("name", dockerImage.getName());
        labels.put("version", dockerImage.getVersion());
        return labels;
    }


    public static EntandoPlugin generatePluginFromDescriptor(PluginDescriptor descriptor) {
        return descriptor.isVersion1()
                ? generatePluginFromDescriptorV1(descriptor) :
                generatePluginFromDescriptorV2(descriptor);
    }

    public static EntandoPlugin generatePluginFromDescriptorV2(PluginDescriptor descriptor) {
        return new EntandoPluginBuilder()
                .withNewMetadata()
                .withName(extractNameFromDescriptor(descriptor))
                .withLabels(extractLabelsFromDescriptor(descriptor))
                .endMetadata()
                .withNewSpec()
                .withDbms(DbmsVendor.valueOf(descriptor.getDbms().toUpperCase()))
                .withImage(descriptor.getImage())
                .withIngressPath(extractIngressPathFromDescriptor(descriptor))
                .withRoles(extractRolesFromDescriptor(descriptor))
                .withHealthCheckPath(descriptor.getHealthCheckPath())
                .withPermissions(extractPermissionsFromDescriptor(descriptor))
                .withSecurityLevel(PluginSecurityLevel.forName(descriptor.getSecurityLevel()))
                .endSpec()
                .build();
    }

    public static EntandoPlugin generatePluginFromDescriptorV1(PluginDescriptor descriptor) {
        return new EntandoPluginBuilder()
                .withNewMetadata()
                .withName(extractNameFromDescriptor(descriptor))
                .withLabels(getLabelsFromImage(descriptor.getDockerImage()))
                .endMetadata()
                .withNewSpec()
                .withDbms(DbmsVendor.valueOf(descriptor.getSpec().getDbms().toUpperCase()))
                .withImage(descriptor.getDockerImage().toString())
                .withIngressPath(composeIngressPathFromDockerImage(descriptor.getDockerImage()))
                .withRoles(extractRolesFromRoleList(descriptor.getSpec().getRoles()))
                .withHealthCheckPath(descriptor.getSpec().getHealthCheckPath())
                .withSecurityLevel(PluginSecurityLevel.forName(descriptor.getSpec().getSecurityLevel()))
                .endSpec()
                .build();
    }


    public static List<ExpectedRole> extractRolesFromRoleList(List<PluginDescriptorV1Role> roleList) {
        return roleList.stream()
                .distinct()
                .map(role -> new ExpectedRole(role.getCode(), role.getName()))
                .collect(Collectors.toList());
    }

    private static String makeKubernetesCompatible(String value) {
        return value.toLowerCase()
                .replaceAll("[\\/\\.\\:_]", "-");
    }

    /**
     * extract the bundle type from the received EntandoDeBundle.
     *
     * @param entandoDeBundle the EntandoDeBundle from which extract the bundle type
     * @return the BundleType reflecting the value found in the received EntandoDeBundle, BundleType.STANDARD_BUNDLE if
     *          no type is found
     */
    public static BundleType extractBundleTypeFromBundle(EntandoDeBundle entandoDeBundle) {

        if (null == entandoDeBundle) {
            throw new EntandoComponentManagerException("The received EntandoDeBundle is null");
        }

        if (null == entandoDeBundle.getMetadata() || null == entandoDeBundle.getMetadata().getLabels()) {
            return BundleType.STANDARD_BUNDLE;
        }

        return entandoDeBundle.getMetadata().getLabels()
                .entrySet().stream()
                .filter(entry -> entry.getKey().equals(BUNDLE_TYPE_LABEL_NAME))
                .findFirst()
                .map(bundleTypeEntry -> BundleType.valueOf(bundleTypeEntry.getValue()))
                .orElse(BundleType.STANDARD_BUNDLE);
    }


    /**
     * determine and return the resource root folder for the current bundle.
     *  - if the current bundle is a standard bundle => root folder = current_bundle_code + '/resources'
     *  - otherwise => '/resources'
     * @param bundleReader the reader of the current bundle
     * @return the resource root folder for the current bundle
     * @throws IOException if a read error occurs during the bundle reading
     */
    public static String determineBundleResourceRootFolder(BundleReader bundleReader) throws IOException {

        BundleType bundleType = bundleReader.readBundleDescriptor().getBundleType();

        return "/" + (null == bundleType || bundleType == BundleType.STANDARD_BUNDLE
                        ? bundleReader.getBundleCode()
                        : "");
    }
}
