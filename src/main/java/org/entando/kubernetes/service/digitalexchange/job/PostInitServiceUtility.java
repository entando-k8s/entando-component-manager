package org.entando.kubernetes.service.digitalexchange.job;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.exception.digitalexchange.InvalidBundleException;
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
        if (StringUtils.length(item.getName()) > 200
                || !ValidationFunctions.VALID_CHARS_RFC_1123_REGEX_PATTERN.matcher(item.getName()).matches()
                || !ValidationFunctions.HOST_MUST_START_AND_END_WITH_ALPHANUMERIC_REGEX_PATTERN.matcher(item.getName())
                .matches()) {
            throw new InvalidBundleException("Error bundle name not valid (RFC 1123) " + item.getName());
        }
        String bundleName = item.getName();
        return BundleUtilities.composeBundleCode(bundleName, bundleId);
    }

}