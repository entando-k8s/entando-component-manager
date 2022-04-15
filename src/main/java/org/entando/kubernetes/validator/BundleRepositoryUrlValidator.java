/**
 * this class exposes some methods to validate the bundle repository URL before using it to generate the hash of
 * the bundle id in order to prevent attacks (collision, etc.)
 */

package org.entando.kubernetes.validator;

import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.exception.EntandoValidationException;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BundleRepositoryUrlValidator {

    public static final int STANDARD_REPO_URL_MAX_LENGTH = 1024;
    public static final int STANDARD_REPO_URL_MAX_SUBPATHS = 20;
    public static final int MAX_SUBPATH_LENGTH = 256;

    private final int repoUrlMaxLength;
    private final int repoUrlMaxSubpaths;

    public BundleRepositoryUrlValidator(
            @Value("${repo.url.max.length:" + STANDARD_REPO_URL_MAX_LENGTH + "}") int repoUrlMaxLength,
            @Value("${repo.url.max.subpaths:" + STANDARD_REPO_URL_MAX_SUBPATHS + "}") int repoUrlMaxSubpaths) {
        this.repoUrlMaxLength = repoUrlMaxLength;
        this.repoUrlMaxSubpaths = repoUrlMaxSubpaths;
    }


    public String validateOrThrow(String bundleUrl) {
        checkIfEmpty(bundleUrl);
        checkIfContainsFragment(bundleUrl);
        checkIfExceedsMaxLength(bundleUrl);
        checkSubpaths(bundleUrl);
        return composeUrlForcingHttpProtocolOrThrow(bundleUrl);
    }

    /**
     * if the url uses the git or ssh protocol, replace it with http validate the received url using url regex.
     * check that the url is not empty
     * check that the url is valid
     *
     * @param stringUrl the string contianing the url to validate
     * @return the received url
     */
    public String composeUrlForcingHttpProtocolOrThrow(String stringUrl) {
        checkIfEmpty(stringUrl);
        final String httpProtocolUrl = BundleUtilities.gitSshProtocolToHttp(stringUrl);
        UrlValidationFunctions.composeUrlOrThrow(httpProtocolUrl);
        return stringUrl;
    }

    private void checkIfEmpty(String repoUrl) {
        if (StringUtils.isEmpty(repoUrl)) {
            throw new EntandoValidationException("Empty repo URL detected");
        }
    }

    private void checkIfContainsFragment(String repoUrl) {
        if (repoUrl.contains("#")) {
            throw new EntandoValidationException(
                    String.format("The repo URL '%s' contains a '#' char. This is not allowed", repoUrl));
        }
    }

    private void checkIfExceedsMaxLength(String repoUrl) {
        if (repoUrl.length() > repoUrlMaxLength) {
            throw new EntandoValidationException(
                    String.format("The repo URL '%s' exceeds the maximum length of %d", repoUrl, repoUrlMaxLength));
        }
    }

    private void checkSubpaths(String repoUrl) {


        String urlNoProtocol = BundleUtilities.BUNDLE_PROTOCOL_REGEX_PATTERN.matcher(repoUrl).replaceFirst("");
        String[] subpaths = urlNoProtocol.split("/");

        // max number of subpaths
        if (subpaths.length > repoUrlMaxSubpaths) {
            throw new EntandoValidationException(String.format(
                    "The number of the repo URL's subpath (identified by splitting the id using the '/' char) exceeds the maximum length of %d",
                    repoUrlMaxSubpaths));
        }

        // max length of each subpath
        Arrays.stream(subpaths).forEach(subpath -> {
            if (subpath.length() > MAX_SUBPATH_LENGTH) {
                throw new EntandoValidationException(String.format(
                        "The subpath '%s' exceeds the maximum allowed length of %d", subpath, MAX_SUBPATH_LENGTH));
            }
        });
    }
}
