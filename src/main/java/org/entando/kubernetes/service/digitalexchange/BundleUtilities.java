package org.entando.kubernetes.service.digitalexchange;

import io.fabric8.zjsonpatch.internal.guava.Strings;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;

public class BundleUtilities {

    public static String getBundleVersionOrFail(EntandoDeBundle bundle, String version) {
       
        String versionToFind = version;
        if ( !isFormattedAsVersion(versionToFind)) {
            versionToFind = (String) bundle.getSpec().getDetails().getDistTags().get(version);
        }
        if (Strings.isNullOrEmpty(versionToFind)) {
            throw new RuntimeException("Invalid version '" + version + "' for bundle '" + bundle.getSpec().getDetails().getName() + "'");
        }
        return versionToFind;
        
    }

    private static boolean isFormattedAsVersion(String versionToFind) {
        return versionToFind.matches("\\d+\\.\\d+") || versionToFind.matches("\\d+\\.\\d+\\.\\d+");
    }
}
