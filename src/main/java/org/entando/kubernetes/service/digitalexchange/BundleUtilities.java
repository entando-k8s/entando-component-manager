package org.entando.kubernetes.service.digitalexchange;

import io.fabric8.zjsonpatch.internal.guava.Strings;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;

public class BundleUtilities {

    public static final String OFFICIAL_SEMANTIC_VERSION_REGEX = "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$";

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
}
