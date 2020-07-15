package org.entando.kubernetes.service.digitalexchange;

import java.text.DateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import lombok.SneakyThrows;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.EntandoComponentBundle;
import org.entando.kubernetes.model.bundle.EntandoComponentBundleVersion;

public class BundleUtilities {

    private static DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.getDefault());

    private BundleUtilities() { }

    public static EntandoComponentBundleVersion getBundleVersionOrFail(EntandoComponentBundle bundle, String version) {
        if (isLatestVersion(version)) {
            return getBundleLatestVersion(bundle);
        }

        if (!isSemanticVersion(version)) {
            throw new EntandoComponentManagerException(
                    "Invalid version '" + version + "' for bundle '" + bundle.getSpec().getCode() + "'");
        }

        return bundle.getSpec().getVersions().stream()
                .filter(v -> v.getVersion().equals(version))
                .findFirst()
                .orElseThrow(() -> new EntandoComponentManagerException(
                        "Invalid version '" + version + "' for bundle '" + bundle.getSpec().getCode() + "'"));
    }

    public static EntandoComponentBundleVersion getBundleLatestVersion(EntandoComponentBundle bundle) {
        return bundle.getSpec().getVersions().stream()
                .max(Comparator.comparing(BundleUtilities::parseDate)) //Find latest version
                .orElseThrow(() -> new EntandoComponentManagerException(
                        "No versions found for Bundle '" + bundle.getSpec().getCode() + "'"));
    }

    @SneakyThrows
    private static Date parseDate(EntandoComponentBundleVersion version) {
        return dateFormat.parse(version.getTimestamp());
    }

    public static boolean isLatestVersion(String version) {
        return version == null || version.equals("latest");
    }

    public static boolean isSemanticVersion(String versionToFind) {
        String possibleSemVer = versionToFind.startsWith("v") ? versionToFind.substring(1) : versionToFind;
        return possibleSemVer.matches(getOfficialSemanticVersionRegex());
    }

    /**
     * Check semantic version definition: https://semver.org/#is-v123-a-semantic-version
     * @return The semantic version PCRE compatible regular expression
     */
    public static String getOfficialSemanticVersionRegex() {
        return "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$";
    }
}
