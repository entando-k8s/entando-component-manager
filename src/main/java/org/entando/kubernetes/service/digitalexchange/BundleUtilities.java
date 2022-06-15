package org.entando.kubernetes.service.digitalexchange;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.zjsonpatch.internal.guava.Strings;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.EntandoValidationException;
import org.entando.kubernetes.model.DbmsVendor;
import org.entando.kubernetes.model.bundle.BundleType;
import org.entando.kubernetes.model.bundle.EntandoBundleVersion;
import org.entando.kubernetes.model.bundle.descriptor.DockerImage;
import org.entando.kubernetes.model.bundle.descriptor.plugin.EnvironmentVariable;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptorV1Role;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleDetails;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPluginBuilder;
import org.entando.kubernetes.model.plugin.ExpectedRole;
import org.entando.kubernetes.model.plugin.Permission;
import org.entando.kubernetes.model.plugin.PluginSecurityLevel;
import org.entando.kubernetes.validator.ImageValidator;
import org.entando.kubernetes.validator.ValidationFunctions;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

@UtilityClass
@Slf4j
public class BundleUtilities {

    public static final String OFFICIAL_SEMANTIC_VERSION_REGEX = "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-("
            + "(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\"
            + ".[0-9a-zA-Z-]+)*))?$";

    public static final int GENERIC_K8S_ENTITY_MAX_LENGTH = 253;

    public static final String BUNDLE_TYPE_LABEL_NAME = "bundle-type";

    public static final String LATEST_VERSION = "latest";

    private static final String DESCRIPTOR_VERSION_STARTING_CHAR = "v";

    public static final String BUNDLE_PROTOCOL_REGEX = "^((git@)|(git:\\/\\/)|(ssh:\\/\\/)|(http:\\/\\/)|(https:\\/\\/))";
    public static final Pattern BUNDLE_PROTOCOL_REGEX_PATTERN = Pattern.compile(BUNDLE_PROTOCOL_REGEX);

    public static final String GIT_AND_SSH_PROTOCOL_REGEX = "^((git@)|(git:\\/\\/)|(ssh:\\/\\/))";
    public static final Pattern GIT_AND_SSH_PROTOCOL_REGEX_PATTERN = Pattern.compile(GIT_AND_SSH_PROTOCOL_REGEX);
    public static final String HTTP_OVER_GIT_REPLACER = ValidationFunctions.HTTP_PROTOCOL + "://";
    public static final String COLONS_REGEX = ":(?!\\/)";
    public static final Pattern COLONS_REGEX_PATTERN = Pattern.compile(COLONS_REGEX);
    public static final int PLUGIN_HASH_LENGTH = 8;

    public static String getBundleVersionOrFail(EntandoDeBundle bundle, String versionReference) {

        if (Strings.isNullOrEmpty(versionReference)) {
            throw new EntandoComponentManagerException("Null or empty version property received");
        }

        String version = versionReference;

        if (version.equals(LATEST_VERSION)) {
            version = composeLatestVersionFromDistTags(bundle)
                    .map(EntandoBundleVersion::getVersion)
                    .orElse(null);
        } else if (!isSemanticVersion(versionReference)) {
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

    /**
     * define and return the latest version respect to the sem version rules applied to the available versions list.
     *
     * @param entandoDeBundle the EntandoDeBundle of which return the latest version
     * @return the latest version respect to the sem version rules
     */
    public static Optional<EntandoBundleVersion> composeLatestVersionFromDistTags(EntandoDeBundle entandoDeBundle) {

        if (entandoDeBundle == null || entandoDeBundle.getSpec() == null
                || entandoDeBundle.getSpec().getDetails() == null) {
            return Optional.empty();
        }

        Optional<EntandoBundleVersion> latestVersionOpt;

        final EntandoDeBundleDetails details = entandoDeBundle.getSpec().getDetails();

        // get the latest from the spec.details.dist-tags.latest property if available
        if (details.getDistTags() != null
                && details.getDistTags().containsKey(LATEST_VERSION)) {

            latestVersionOpt = Optional.of(new EntandoBundleVersion()
                    .setVersion(details.getDistTags().get(LATEST_VERSION).toString()));

        } else if (!CollectionUtils.isEmpty(details.getVersions())) {

            // calculate the latest from the versions list
            latestVersionOpt = details.getVersions().stream()
                    .map(version -> new EntandoBundleVersion().setVersion(version))
                    .max(Comparator.comparing(EntandoBundleVersion::getSemVersion));
        } else {
            latestVersionOpt = Optional.empty();
        }

        return latestVersionOpt;
    }

    /**
     * compose the plugin descriptor version by concatenating the received version number to the leading char v.
     *
     * @param version the integer version
     * @return the composed plugin descriptor version
     */
    public static String composePluginDescriptorVersion(int version) {
        return DESCRIPTOR_VERSION_STARTING_CHAR + version;
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

    public static String extractIngressPathFromDescriptor(PluginDescriptor descriptor) {
        return Optional.ofNullable(composeIngressPathFromIngressPathProperty(descriptor))
                .orElse(composeIngressPathFromDockerImage(descriptor));
    }

    public static Map<String, String> extractLabelsFromDescriptor(PluginDescriptor descriptor) {
        var dockerImage = descriptor.getDockerImage();
        return getLabelsFromImage(dockerImage);
    }


    public static String composeNameFromDockerImage(DockerImage image) {

        return String.join("-",
                makeKubernetesCompatible(image.getOrganization()),
                makeKubernetesCompatible(image.getName()));
    }

    /**
     * read the ingress path property from the plugin descriptor and return its value if present, null otherwise.
     *
     * @param descriptor the PluginDescriptor from which get the ingress path
     * @return the ingress path read from the plugin descriptor property or null if it is not present
     */
    private static String composeIngressPathFromIngressPathProperty(PluginDescriptor descriptor) {

        String ingressPath = null;

        if (StringUtils.length(descriptor.getIngressPath()) > 0) {
            ingressPath = descriptor.getIngressPath();
            if (ingressPath.charAt(0) != '/') {
                ingressPath = "/" + ingressPath;
            }
        }

        return ingressPath;
    }

    private static String composeIngressPathFromDockerImage(PluginDescriptor descriptor) {

        DockerImage image = descriptor.getDockerImage();

        List<String> ingressSegmentList = new ArrayList<>(Arrays.asList(image.getOrganization(), image.getName()));

        if (descriptor.isVersionLowerThan3()) {
            ingressSegmentList.add(image.getVersion());
        }

        List<String> kubeCompatiblesSegmentList = ingressSegmentList.stream()
                .map(BundleUtilities::makeKubernetesCompatible).collect(Collectors.toList());

        return "/" + String.join("/", kubeCompatiblesSegmentList);
    }


    public static Map<String, String> getLabelsFromImage(DockerImage dockerImage) {
        Map<String, String> labels = new HashMap<>();
        labels.put("organization", dockerImage.getOrganization());
        labels.put("name", dockerImage.getName());
        labels.put("version", dockerImage.getVersion());
        return labels;
    }


    /**
     * generate the EntandoPlugin CR starting by the received plugin descriptor.
     *
     * @param descriptor the plugin descriptor from which get the CR data
     * @return the EntandoPlugin CR generated starting by the descriptor data
     */
    public static EntandoPlugin generatePluginFromDescriptor(PluginDescriptor descriptor) {
        return descriptor.isVersion1()
                ? generatePluginFromDescriptorV1(descriptor) :
                generatePluginFromDescriptorV2Plus(descriptor);
    }

    /**
     * generate the EntandoPlugin CR starting by the received plugin descriptor version equal or major than 2.
     *
     * @param descriptor the plugin descriptor from which get the CR data
     * @return the EntandoPlugin CR generated starting by the descriptor data
     */
    public static EntandoPlugin generatePluginFromDescriptorV2Plus(PluginDescriptor descriptor) {
        return new EntandoPluginBuilder()
                .withNewMetadata()
                .withName(descriptor.getDescriptorMetadata().getFullDeploymentName())
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
                .withEnvironmentVariables(assemblePluginEnvVars(descriptor.getEnvironmentVariables()))
                .endSpec()
                .build();
    }

    /**
     * generate the EntandoPlugin CR starting by the received plugin descriptor version equal to 1.
     *
     * @param descriptor the plugin descriptor from which get the CR data
     * @return the EntandoPlugin CR generated starting by the descriptor data
     */
    public static EntandoPlugin generatePluginFromDescriptorV1(PluginDescriptor descriptor) {
        return new EntandoPluginBuilder()
                .withNewMetadata()
                .withName(descriptor.getDescriptorMetadata().getFullDeploymentName())
                .withLabels(getLabelsFromImage(descriptor.getDockerImage()))
                .endMetadata()
                .withNewSpec()
                .withDbms(DbmsVendor.valueOf(descriptor.getSpec().getDbms().toUpperCase()))
                .withImage(descriptor.getDockerImage().toString())
                .withIngressPath(composeIngressPathFromDockerImage(descriptor))
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

    public static String makeKubernetesCompatible(String value) {
        return value.toLowerCase()
                .replaceAll("[\\/\\.\\:_]", "-");
    }

    /**
     * extract the bundle type from the received EntandoDeBundle.
     *
     * @param entandoDeBundle the EntandoDeBundle from which extract the bundle type
     * @return the BundleType reflecting the value found in the received EntandoDeBundle, BundleType.STANDARD_BUNDLE if
     *     no type is found
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
                .map(bundleTypeEntry -> BundleType.fromType(bundleTypeEntry.getValue()))
                .orElse(BundleType.STANDARD_BUNDLE);
    }


    /**
     * determine and return the resource root folder for the current bundle. - if the current bundle is a standard
     * bundle, root folder = current_bundle_code + '/resources' - otherwise '/resources'
     *
     * @param bundleReader the reader of the current bundle
     * @return the resource root folder for the current bundle
     * @throws IOException if a read error occurs during the bundle reading
     */
    public static String determineBundleResourceRootFolder(BundleReader bundleReader) throws IOException {

        var bundleType = bundleReader.readBundleDescriptor().getBundleType();

        return "/" + (null == bundleType || bundleType == BundleType.STANDARD_BUNDLE
                ? bundleReader.getBundleCode()
                : "");
    }

    /**
     * receives a list of environment variables and convert them to the K8S env var format.
     *
     * @param environmentVariableList the PluginDescriptor from which get the env vars to convert
     * @return the list of K8S compatible EnvVar
     */
    public static List<EnvVar> assemblePluginEnvVars(List<EnvironmentVariable> environmentVariableList) {

        return Optional.ofNullable(environmentVariableList)
                .orElseGet(ArrayList::new)
                .stream().map(envVar -> {
                    EnvVarBuilder builder = new EnvVarBuilder()
                            .withName(envVar.getName());

                    if (envVar.safeGetValueFrom().getSecretKeyRef() == null) {
                        builder.withValue(envVar.getValue());
                    } else {
                        builder.withNewValueFrom()
                                .withNewSecretKeyRef()
                                .withName(envVar.safeGetValueFrom().getSecretKeyRef().getName())
                                .withKey(envVar.safeGetValueFrom().getSecretKeyRef().getKey())
                                .endSecretKeyRef()
                                .endValueFrom();
                    }

                    return builder.build();
                })
                .collect(Collectors.toList());
    }

    /**
     * compose the name of a bundle starting by its repository url.
     *
     * @param bundleUrl the repository url of the bundle
     * @return the composed name of the bundle
     */
    public static String composeBundleIdentifier(String bundleUrl) {
        if (ObjectUtils.isEmpty(bundleUrl)) {
            return "";
        }

        // remove the protocol
        String urlNoProtocol = BUNDLE_PROTOCOL_REGEX_PATTERN.matcher(bundleUrl).replaceFirst("");

        // remove final .git and split by /
        final String[] urlTokens = urlNoProtocol.replaceAll(".git$", "")
                .replace(":", "/")
                .split("/");

        // reverse the array and join by . (to ensure k8s compatibility)
        String id = IntStream.rangeClosed(1, urlTokens.length)
                .mapToObj(i -> urlTokens[urlTokens.length - i])
                .collect(Collectors.joining("."));

        // remove possible leading and final dots
        if (id.charAt(0) == '.') {
            id = id.substring(1);
        }
        if (id.charAt(id.length() - 1) == '.') {
            id = id.substring(0, id.length() - 1);
        }

        // remove double points
        id = id.replace("..", ".");

        if (id.length() > GENERIC_K8S_ENTITY_MAX_LENGTH) {
            throw new EntandoValidationException(
                    "The bundle resulting name is \"" + id + "\" but its size exceeds " + GENERIC_K8S_ENTITY_MAX_LENGTH
                            + " characters");
        }

        return id;
    }

    /**
     * return the received url without the protocol.
     *
     * @param url the url to manipulate to remove the protocol
     * @return the received url without the protocol
     */
    public static String removeProtocolFromUrl(String url) {
        ImageValidator imageValidator = ImageValidator.parse(url);
        if (imageValidator.isTransportValid()) {
            return imageValidator.composeCommonUrlOrThrow("The image fully qualified URL of the bundle is invalid");
        } else {
            URL bundleUrl = ValidationFunctions.composeUrlOrThrow(url,
                    "The repository URL of the bundle is null",
                    "The repository URL of the bundle is invalid");
            final int index = bundleUrl.toString().indexOf(bundleUrl.getHost());
            return bundleUrl.toString().substring(index);
        }
    }

    /**
     * sign the bundle id prepending the first 8 chars of the bundle url.
     *
     * @param bundleUrl the url of the repository of the bundle
     * @return the signed bundle id
     */
    public static String signBundleId(String bundleUrl) {
        return DigestUtils.sha256Hex(bundleUrl).substring(0, BundleUtilities.PLUGIN_HASH_LENGTH);
    }

    /**
     * if the received url string starts with a git or ssh protocol, it replaces the protocol with a simple http:// .
     *
     * @param url the string url to check and possibly replace
     * @return the url with the replaces protocol or the original string itself
     */
    public static String gitSshProtocolToHttp(String url) {
        String repoUrl = GIT_AND_SSH_PROTOCOL_REGEX_PATTERN.matcher(url).replaceFirst(HTTP_OVER_GIT_REPLACER);
        return COLONS_REGEX_PATTERN.matcher(repoUrl).replaceFirst("/");
    }
}
