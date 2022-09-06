package org.entando.kubernetes.service.digitalexchange.job;

import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.service.digitalexchange.job.PostInitConfigurationService.PostInitItem;
import org.entando.kubernetes.validator.ValidationFunctions;

@Slf4j
public final class PostInitServiceUtility {

    private PostInitServiceUtility(){

    }

    public static String calculateBundleCode(PostInitItem item) {
        String bundleId = BundleUtilities.removeProtocolAndGetBundleId(item.getUrl());

        // https://kubernetes.io/docs/concepts/overview/working-with-objects/names/#dns-subdomain-names
        ValidationFunctions.bundleNameValidOrThrow(item.getName());

        String bundleName = item.getName();
        return BundleUtilities.composeBundleCode(bundleName, bundleId);
    }

}