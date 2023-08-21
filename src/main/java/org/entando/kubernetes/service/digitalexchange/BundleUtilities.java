package org.entando.kubernetes.service.digitalexchange;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.zjsonpatch.internal.guava.Strings;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.client.k8ssvc.K8SServiceClient.PluginConfiguration;
import org.entando.kubernetes.config.tenant.thread.TenantContextHolder;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.EntandoValidationException;
import org.entando.kubernetes.model.bundle.BundleProperty;
import org.entando.kubernetes.model.bundle.BundleType;
import org.entando.kubernetes.model.bundle.EntandoBundleVersion;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.DescriptorVersion;
import org.entando.kubernetes.model.bundle.descriptor.DockerImage;
import org.entando.kubernetes.model.bundle.descriptor.VersionedDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.plugin.EnvironmentVariable;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginDescriptorV1Role;
import org.entando.kubernetes.model.bundle.descriptor.plugin.PluginResources;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.common.DbmsVendor;
import org.entando.kubernetes.model.common.EntandoResourceRequirements;
import org.entando.kubernetes.model.common.EntandoResourceRequirementsBuilder;
import org.entando.kubernetes.model.common.ExpectedRole;
import org.entando.kubernetes.model.common.Permission;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleDetails;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPluginBuilder;
import org.entando.kubernetes.model.plugin.PluginSecurityLevel;
import org.entando.kubernetes.validator.ImageValidator;
import org.entando.kubernetes.validator.ValidationFunctions;
import org.springframework.util.Assert;
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

    public static final String BUNDLE_PROTOCOL_REGEX = "^((git@)|(git:\\/\\/)|(ssh:\\/\\/)|(http:\\/\\/)|(https:\\/\\/))";
    public static final Pattern BUNDLE_PROTOCOL_REGEX_PATTERN = Pattern.compile(BUNDLE_PROTOCOL_REGEX);

    public static final String GIT_AND_SSH_PROTOCOL_REGEX = "^((git@)|(git:\\/\\/)|(ssh:\\/\\/))";
    public static final Pattern GIT_AND_SSH_PROTOCOL_REGEX_PATTERN = Pattern.compile(GIT_AND_SSH_PROTOCOL_REGEX);
    public static final String HTTP_OVER_GIT_REPLACER = ValidationFunctions.HTTP_PROTOCOL + "://";
    public static final String COLONS_REGEX = ":(?!\\/)";
    public static final Pattern COLONS_REGEX_PATTERN = Pattern.compile(COLONS_REGEX);
    public static final int ENTITY_CODE_HASH_LENGTH = 8;

    public static final String BUNDLES_FOLDER = "bundles";

    public static final String GLOBAL_PREFIX = "global:";

    public static final String ENTANDO_DOCKER_REGISTRY_OVERRIDE = "ENTANDO_DOCKER_REGISTRY_OVERRIDE";
    public static final String DOCKER_IMAGE_TRANSPORT_PREFIX = "docker://";


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
                && details.getDistTags().containsKey(LATEST_VERSION)
                && EntandoBundleVersion.isSemanticVersion(details.getDistTags().get(LATEST_VERSION).toString())) {

            latestVersionOpt = Optional.ofNullable(new EntandoBundleVersion()
                    .setVersion(details.getDistTags().get(LATEST_VERSION).toString()));

        } else if (!CollectionUtils.isEmpty(details.getVersions())) {

            // calculate the latest from the versions list
            latestVersionOpt = details.getVersions().stream()
                    .map(version -> new EntandoBundleVersion().setVersion(version))
                    .filter(Objects::nonNull)
                    .max(Comparator.comparing(EntandoBundleVersion::getSemVersion));
        } else {
            latestVersionOpt = Optional.empty();
        }

        return latestVersionOpt;
    }

    public static List<ExpectedRole> extractRolesFromDescriptor(PluginDescriptor descriptor) {
        return Optional.ofNullable(descriptor.getRoles()).orElseGet(ArrayList::new).stream()
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

    public static String extractIngressPathFromDescriptor(PluginDescriptor descriptor, String bundleCode) {

        // if v5
        if (descriptor.isVersionEqualOrGreaterThan(DescriptorVersion.V5)) {
            return composeIngressPathForV5(descriptor, bundleCode);
        }

        return composeIngressPathForV1(descriptor);
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
    public static String composeIngressPathFromIngressPathProperty(PluginDescriptor descriptor) {

        String ingressPath = null;

        if (StringUtils.length(descriptor.getIngressPath()) > 0) {
            ingressPath = descriptor.getIngressPath();
            if (ingressPath.charAt(0) != '/') {
                ingressPath = "/" + ingressPath;
            }
            ingressPath = buildTenantIdPath() + ingressPath;

        }

        return ingressPath;
    }

    public static String composeIngressPathFromDockerImage(PluginDescriptor descriptor) {
        DockerImage image = descriptor.getDockerImage();

        List<String> ingressSegmentList = new ArrayList<>(Arrays.asList(image.getOrganization(), image.getName()));

        if (descriptor.isVersionLowerThan3()) {
            ingressSegmentList.add(image.getTag());
        }

        List<String> kubeCompatiblesSegmentList = ingressSegmentList.stream()
                .map(BundleUtilities::makeKubernetesCompatible).collect(Collectors.toList());

        return buildTenantIdPath() + "/" + String.join("/", kubeCompatiblesSegmentList);
    }

    private String buildTenantIdPath() {
        return "/" + calculateTenantId(TenantContextHolder.getCurrentTenantCode());
    }
    
    /**
     * compose the plugin ingress path starting by its docker image.
     *
     * @param descriptor the PluginDescriptor from which take the docker image
     * @return the composed ingress path
     */
    public static String composeIngressPathForV1(PluginDescriptor descriptor) {

        // compose from ingress path property's value
        return Optional.ofNullable(composeIngressPathFromIngressPathProperty(descriptor))
                // otherwise compose from docker image
                .orElseGet(() -> composeIngressPathFromDockerImage(descriptor));
    }

    /**
     * compose the plugin ingress path starting by its bundle code and its name.
     *
     * @param descriptor the PluginDescriptor from which take the info
     * @return the composed ingress path
     */
    public static String composeIngressPathForV5(PluginDescriptor descriptor, String bundleCode) {
        String buildBundleCodeWithTenant = Optional.ofNullable(TenantContextHolder.getCurrentTenantCode())
                .map(tenantCode -> bundleCode + "-" + calculateTenantId(tenantCode))
                .orElse(bundleCode);

        return "/"
                + Stream.of(buildBundleCodeWithTenant,
                        makeKubernetesCompatible(descriptor.getName()))
                .map(BundleUtilities::makeKubernetesCompatible)
                .collect(Collectors.joining("/"));
    }


    public static Map<String, String> getLabelsFromImage(DockerImage dockerImage) {
        Map<String, String> labels = new HashMap<>();
        labels.put("organization", dockerImage.getOrganization());
        labels.put("name", dockerImage.getName());
        labels.put("version", dockerImage.getTag());
        return labels;
    }

    public static Map<String, String> getAnnotationsFromImage(DockerImage dockerImage) {
        Map<String, String> annotations = new HashMap<>();
        annotations.put("entando.org/image-tag", dockerImage.getTag());
        return annotations;
    }


    /**
     * generate the EntandoPlugin CR starting by the received plugin descriptor.
     *
     * @param descriptor the plugin descriptor from which get the CR data
     * @return the EntandoPlugin CR generated starting by the descriptor data
     */
    public static EntandoPlugin generatePluginFromDescriptor(PluginDescriptor descriptor, Optional<PluginConfiguration> conf) {
        return descriptor.isVersion1()
                ? generatePluginFromDescriptorV1(descriptor) :
                generatePluginFromDescriptorV2Plus(descriptor, conf);
    }

    /**
     * generate the EntandoPlugin CR starting by the received plugin descriptor version equal or major than 2.
     *
     * @param descriptor the plugin descriptor from which get the CR data
     * @return the EntandoPlugin CR generated starting by the descriptor data
     */
    public static EntandoPlugin generatePluginFromDescriptorV2Plus(PluginDescriptor descriptor,
                                                                   Optional<PluginConfiguration> conf) {
        return new EntandoPluginBuilder()
                .withNewMetadata()
                .withName(descriptor.getDescriptorMetadata().getPluginCodeTenantAware())
                .withLabels(extractLabelsFromDescriptor(descriptor))
                .withAnnotations(getAnnotationsFromImage(descriptor.getDockerImage()))
                .endMetadata()
                .withNewSpec()
                .withDbms(DbmsVendor.valueOf(descriptor.getDbms().toUpperCase()))
                .withImage(descriptor.getDockerImage().toString())
                .withIngressPath(descriptor.getDescriptorMetadata().getEndpoint())
                .withCustomIngressPath(descriptor.getDescriptorMetadata().getCustomEndpoint())
                .withRoles(extractRolesFromDescriptor(descriptor))
                .withHealthCheckPath(descriptor.getHealthCheckPath())
                .withPermissions(extractPermissionsFromDescriptor(descriptor))
                .withSecurityLevel(PluginSecurityLevel.forName(descriptor.getSecurityLevel()))
                .withEnvironmentVariables(assemblePluginEnvVars(descriptor.getEnvironmentVariables(),
                        conf.map(PluginConfiguration::getEnvironmentVariables).orElse(Collections.emptyList())))
                .withResourceRequirements(generateResourceRequirementsFromDescriptor(descriptor))
                .withTenantCode(descriptor.getDescriptorMetadata().getTenantCode())
                .endSpec()
                .build();
    }

    public static EntandoResourceRequirements generateResourceRequirementsFromDescriptor(PluginDescriptor descriptor) {

        final PluginResources pluginResources = descriptor.getResources();

        final EntandoResourceRequirementsBuilder builder = new EntandoResourceRequirementsBuilder();
        if (pluginResources != null) {
            builder
                    .withStorageRequest(pluginResources.getStorage())
                    .withMemoryRequest(pluginResources.getMemory())
                    .withCpuRequest(pluginResources.getCpu());
        }

        return builder.build();
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
                .withName(descriptor.getDescriptorMetadata().getPluginCode())
                .withLabels(getLabelsFromImage(descriptor.getDockerImage()))
                .withAnnotations(getAnnotationsFromImage(descriptor.getDockerImage()))
                .endMetadata()
                .withNewSpec()
                .withDbms(DbmsVendor.valueOf(descriptor.getSpec().getDbms().toUpperCase()))
                .withImage(descriptor.getDockerImage().toString())
                .withIngressPath(composeIngressPathForV1(descriptor))
                .withRoles(extractRolesFromRoleList(descriptor.getSpec().getRoles()))
                .withHealthCheckPath(descriptor.getSpec().getHealthCheckPath())
                .withSecurityLevel(PluginSecurityLevel.forName(descriptor.getSpec().getSecurityLevel()))
                .withTenantCode(descriptor.getDescriptorMetadata().getTenantCode())
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
                .map(bundleTypeEntry -> BundleType.fromType(bundleTypeEntry.getValue()))
                .orElse(BundleType.STANDARD_BUNDLE);
    }


    /**
     * convenience method using only the BundleReader.
     *
     * @param bundleReader the reader of the current bundle
     * @return the resource root folder for the current bundle
     * @throws IOException if a read error occurs during the bundle reading
     */
    public static String determineBundleResourceRootFolder(BundleReader bundleReader) throws IOException {
        final BundleDescriptor bundleDescriptor = bundleReader.readBundleDescriptor();
        return determineBundleResourceRootFolder(
                bundleDescriptor.getBundleType(),
                bundleDescriptor.getDescriptorVersion(),
                bundleReader.getCode());
    }

    /**
     * determine and return the resource root folder for the current bundle. - if the current bundle is a standard
     * bundle, root folder = current_bundle_code + '/resources' - otherwise '/resources'
     *
     * @param bundleType        the bundle type
     * @param descriptorVersion the bundle descriptor version
     * @param bundleCode        the bundle code
     * @return the resource root folder for the current bundle
     */
    public static String determineBundleResourceRootFolder(BundleType bundleType, String descriptorVersion,
            String bundleCode) {

        var resourceFolder = "/";

        if (null == bundleType || bundleType == BundleType.STANDARD_BUNDLE) {
            if (VersionedDescriptor.isVersion1(descriptorVersion)) {
                resourceFolder += extractNameFromEntityCode(bundleCode);
            } else {
                resourceFolder += bundleCode;
            }
        }

        return resourceFolder;
    }

    /**
     * return the bundle folder name full of the signing hash.
     *
     * @param bundleReader the BundleReader to use to compose the signed bundle folder name
     * @return the composed signed bundle folder name
     */
    public static String determineSignedBundleFolder(BundleReader bundleReader) throws IOException {
        final String resourceFolder = BundleUtilities.determineBundleResourceRootFolder(bundleReader);
        return Paths.get(BUNDLES_FOLDER, resourceFolder).toString();
    }

    /**
     * return the bundle folder name full of the signing hash.
     *
     * @param bundleType        the type of the bundle
     * @param descriptorVersion the version of the descriptor of the bundle
     * @param bundleCode        the code of the bundle
     * @return the composed signed bundle folder name
     */
    public static String determineSignedBundleFolder(BundleType bundleType, String descriptorVersion,
            String bundleCode) {
        final String resourceFolder = BundleUtilities.determineBundleResourceRootFolder(bundleType, descriptorVersion,
                bundleCode);
        return Paths.get(BUNDLES_FOLDER, resourceFolder).toString();
    }

    /**
     * receives a list of environment variables and convert them to the K8S env var format.
     *
     * @param environmentVariableList the PluginDescriptor from which get the env vars to convert
     * @return the list of K8S compatible EnvVar
     */
    public static List<EnvVar> assemblePluginEnvVars(List<EnvironmentVariable> environmentVariableList,
                                                     List<EnvVar> customEnvironmentVariablesList) {
        Map<String, EnvVar> customConfigurations = customEnvironmentVariablesList.stream()
                .collect(Collectors.toMap(EnvVar::getName, e -> e));
        return Optional.ofNullable(environmentVariableList)
                .orElseGet(ArrayList::new)
                .stream().map(envVar -> Optional.ofNullable(customConfigurations.get(envVar.getName()))
                        .orElseGet(() -> buildFromEnvironmentVariable(envVar)))
                .collect(Collectors.toList());
    }

    private EnvVar buildFromEnvironmentVariable(EnvironmentVariable envVar) {
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
            return imageValidator.composeCommonUrlWithoutTransportWithoutTagOrThrow(
                    "The image fully qualified URL of the bundle is invalid");
        } else {
            url = gitSshProtocolToHttp(url);
            URL bundleUrl = ValidationFunctions.composeUrlOrThrow(url,
                    "The repository URL of the bundle is null",
                    "The repository URL of the bundle is invalid");
            final int index = bundleUrl.toString().indexOf(bundleUrl.getHost());
            return bundleUrl.toString().substring(index);
        }
    }

    /**
     * get the bundle id returning the first 8 chars of the bundle url.
     *
     * @param bundleUrl the url of the repository of the bundle
     * @return the signed bundle id
     */
    public static String getBundleId(String bundleUrl) {
        return DigestUtils.sha256Hex(bundleUrl).substring(0, ENTITY_CODE_HASH_LENGTH);
    }

    /**
     * get the tenant id returning the first 8 chars of the tenant code digest.
     *
     * @param tenantCode the code of the tenant
     * @return the signed tenant id
     */
    public static String calculateTenantId(String tenantCode) {
        return DigestUtils.sha256Hex(tenantCode).substring(0, ENTITY_CODE_HASH_LENGTH);
    }

    /**
     * get the bundle id returning the first 8 chars of the bundle url.
     *
     * @param bundleUrl the url of the repository of the bundle
     * @return the signed bundle id
     */
    public static String removeProtocolAndGetBundleId(String bundleUrl) {
        final String url = BundleUtilities.removeProtocolFromUrl(bundleUrl);
        return getBundleId(url);
    }

    /**
     * if the received url string starts with a git or ssh protocol, it replaces the protocol with a simple http:// .
     *
     * @param url the string url to check and possibly replace
     * @return the url with the replaces protocol or the original string itself
     */
    public static String gitSshProtocolToHttp(String url) {
        String repoUrl = GIT_AND_SSH_PROTOCOL_REGEX_PATTERN.matcher(url).replaceFirst(HTTP_OVER_GIT_REPLACER);
        if (StringUtils.equals(url, repoUrl)) {
            return url;
        } else {
            return COLONS_REGEX_PATTERN.matcher(repoUrl).replaceFirst("/");
        }
    }


    /**
     * build the full path of a resource inside a bundle.
     *
     * @param bundleReader         the bundle reader responsible for reading the bundle
     * @param folderProp           the BundleProperty indicating the root folder of the file
     * @param fileDescriptorFolder the folder containing the current file
     * @return the built full path of a resource
     */
    public static String buildFullBundleResourcePath(BundleReader bundleReader, BundleProperty folderProp,
            String fileDescriptorFolder) throws IOException {

        final BundleDescriptor bundleDescriptor = bundleReader.readBundleDescriptor();
        return buildFullBundleResourcePath(
                bundleDescriptor.getBundleType(),
                bundleDescriptor.getDescriptorVersion(),
                folderProp,
                fileDescriptorFolder,
                bundleDescriptor.getCode());
    }


    /**
     * build the full path of a resource inside a bundle.
     *
     * @param bundleType           the type of the current  bundle
     * @param folderProp           the BundleProperty indicating the root folder of the file
     * @param fileDescriptorFolder the folder containing the current file
     * @param bundleCode           the code of the current bundle
     * @return the built full path of a resource
     */
    public static String buildFullBundleResourcePath(BundleType bundleType, String descriptorVersion,
            BundleProperty folderProp, String fileDescriptorFolder, String bundleCode) {

        final String signedBundleFolder = determineSignedBundleFolder(bundleType, descriptorVersion, bundleCode);
        Path fileFolder = Paths.get(folderProp.getValue()).relativize(Paths.get(fileDescriptorFolder));

        if (!VersionedDescriptor.isVersion1(descriptorVersion) && folderProp == BundleProperty.WIDGET_FOLDER_PATH) {
            fileFolder = signWidgetFolder(bundleCode, fileFolder);
        }

        return Paths.get(signedBundleFolder, folderProp.getValue()).resolve(fileFolder).toString();
    }

    /**
     * sign a widget folder.
     *
     * @param bundleCode   the code of the bundle containin the id to sign the widget folder
     * @param widgetFolder the widget folder to sign
     * @return the signed widget folder
     */
    private Path signWidgetFolder(String bundleCode, Path widgetFolder) {
        String bundleId = BundleUtilities.extractIdFromEntityCode(bundleCode);
        final String signedWidgetName = widgetFolder.subpath(0, 1).toString().concat("-").concat(bundleId);
        return Paths.get(signedWidgetName, widgetFolder.subpath(0, 1).relativize(widgetFolder).toString());

    }

    public static String composeDescriptorCode(String code, String name, VersionedDescriptor descriptor,
            String bundleUrl) {
        return composeDescriptorCode(code, name, descriptor, bundleUrl, "-");
    }

    /**
     * generic method to compose the descriptor code concatenating the bundle id to the descriptor name.
     *
     * @return the composed descriptor code
     */
    public static String composeDescriptorCode(String code, String name, VersionedDescriptor descriptor,
            String bundleUrl, String separator) {
        String bundleIdHash = BundleUtilities.removeProtocolAndGetBundleId(bundleUrl);
        String composedCode = code;
        if (descriptor.isVersion1() || !descriptor.isVersionEqualOrGreaterThan(DescriptorVersion.V5)) {
            if (!code.endsWith(bundleIdHash)) {
                composedCode += separator + bundleIdHash;
            }
        } else {
            composedCode = composeBundleCode(name, bundleIdHash, separator);
        }
        return composedCode;
    }

    public static String composeBundleCode(String bundleName, String bundleId) {
        return composeBundleCode(bundleName, bundleId, "-");
    }

    public static String composeBundleCode(String bundleName, String bundleId, String separator) {
        return bundleName + separator + bundleId;
    }

    /**
     * This method decodes the input URL with base64 algorithm and validates it.
     *
     * @param encodedUrl a nom-null String representing a valid URL encoded with the base64 algorithm
     * @return the URL decoded and validated
     */
    public static String decodeUrl(String encodedUrl) {
        Assert.notNull(encodedUrl, "repoUrl cannot be null");
        // repoUrl should be decoded from BASE64
        final String decodedRepoUrlString = new String(Base64.getDecoder().decode(encodedUrl));
        return ValidationFunctions.composeCommonUrlOrThrow(decodedRepoUrlString,
                "Repo url is empty", "Repo url is not valid");
    }

    public static String composeBundleResourceRootFolter(BundleReader bundleReader) throws IOException {
        if (bundleReader.isBundleV1() && bundleReader.containsBundleResourceFolder()) {
            return determineBundleResourceRootFolder(bundleReader);
        } else {
            return determineSignedBundleFolder(bundleReader);
        }
    }

    /**
     * Returns the registry of an imageAddress or null if not present, or it's not an image address.
     */
    public static String extractImageAddressRegistry(String imageAddress) {
        if (Strings.isNullOrEmpty(imageAddress)) {
            return null;
        }
        try {
            if (imageAddress.startsWith(DOCKER_IMAGE_TRANSPORT_PREFIX)) {
                return DockerImage.fromString(imageAddress.substring(DOCKER_IMAGE_TRANSPORT_PREFIX.length()))
                        .getRegistry();
            } else {
                return DockerImage.fromString(imageAddress).getRegistry();
            }
        } catch (Exception ex) {
            log.debug("Error detected while parsing the imageAddress \"{}\"", imageAddress);
            return null;
        }
    }

    /**
     * Determine the full qualified image address of an oci-based component.
     */
    public static String determineComponentFqImageAddress(
            String componentImageAddress,
            String bundleImageAddress,
            String fallback) {
        //~
        String componentImageRegistry = extractImageAddressRegistry(componentImageAddress);
        if (!Strings.isNullOrEmpty(componentImageRegistry)) {
            return componentImageAddress;
        }

        String parentBundleRegistry = extractImageAddressRegistry(bundleImageAddress);
        if (Strings.isNullOrEmpty(parentBundleRegistry)) {
            parentBundleRegistry = fallback;
        }

        return (Strings.isNullOrEmpty(parentBundleRegistry))
                ? componentImageAddress
                : Paths.get(parentBundleRegistry, componentImageAddress).toString();
    }

    public static String readDefaultImageRegistryOverride() {
        return Optional.ofNullable(System.getenv(ENTANDO_DOCKER_REGISTRY_OVERRIDE)).orElse("");
    }

    public String extractNameFromEntityCode(String entityCode) throws EntandoComponentManagerException {
        ValidationFunctions.validateEntityCodeOrThrow(entityCode);
        return entityCode.substring(0, entityCode.length() - (ENTITY_CODE_HASH_LENGTH + 1));
    }

    public static String extractIdFromEntityCode(String entityCode) throws EntandoComponentManagerException {
        ValidationFunctions.validateEntityCodeOrThrow(entityCode);
        return entityCode.substring(entityCode.length() - ENTITY_CODE_HASH_LENGTH);
    }
}
