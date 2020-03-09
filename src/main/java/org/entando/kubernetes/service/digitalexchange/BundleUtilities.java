package org.entando.kubernetes.service.digitalexchange;

import io.fabric8.zjsonpatch.internal.guava.Strings;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;

public class BundleUtilities {

    public static String getBundleVersionOrFail(EntandoDeBundle bundle, String versionReference) {
       
        String version = versionReference;
        if ( !hasVersionFormat(versionReference)) {
            version = (String) bundle.getSpec().getDetails().getDistTags().get(versionReference);
        }
        if (Strings.isNullOrEmpty(version)) {
            throw new RuntimeException("Invalid version '" + versionReference + "' for bundle '" + bundle.getSpec().getDetails().getName() + "'");
        }
        return version;
        
    }

    private static boolean hasVersionFormat(String versionToFind) {
        // Check if the provided string has version format ##.##.##
        return versionToFind.matches("\\d+\\.\\d+") || versionToFind.matches("\\d+\\.\\d+\\.\\d+");
    }
}
