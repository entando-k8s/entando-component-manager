package org.entando.kubernetes.service.digitalexchange;

import io.fabric8.zjsonpatch.internal.guava.Strings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.DbmsVendor;
import org.entando.kubernetes.model.bundle.descriptor.DockerImage;
import org.entando.kubernetes.model.bundle.descriptor.PluginDescriptor;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.plugin.EntandoPlugin;
import org.entando.kubernetes.model.plugin.EntandoPluginBuilder;
import org.entando.kubernetes.model.plugin.ExpectedRole;

public class BundleUtilities {

    public static final String OFFICIAL_SEMANTIC_VERSION_REGEX = "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-("
            + "(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\"
            + ".[0-9a-zA-Z-]+)*))?$";

    private BundleUtilities() {
    }

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

    public static String extractNameFromDescriptor(PluginDescriptor descriptor) {
        return composeNameFromDockerImage(descriptor.getDockerImage());
    }

    public static String extractIngressPathFromDescriptor(PluginDescriptor descriptor) {
        return composeIngressPathFromDockerImage(descriptor.getDockerImage());
    }

    public static Map<String, String> extractLabelsFromDescriptor(PluginDescriptor descriptor) {
        DockerImage dockerImage = descriptor.getDockerImage();
        return getLabelsFromImage(dockerImage);
    }

    private static String composeNameFromDockerImage(DockerImage image) {
        return String.join("-",
                makeKubernetesCompatible(image.getOrganization()),
                makeKubernetesCompatible(image.getName()),
                makeKubernetesCompatible(image.getVersion()));
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
                .endSpec()
                .build();
    }

    private static String makeKubernetesCompatible(String value) {
        value = value.toLowerCase();
        value = value.replaceAll("[._]", "-");
        return value;
    }
}
