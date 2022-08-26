package org.entando.kubernetes.validator;

import static org.entando.kubernetes.service.digitalexchange.BundleUtilities.BUNDLE_PROTOCOL_REGEX_PATTERN;
import static org.entando.kubernetes.service.digitalexchange.BundleUtilities.COLONS_REGEX_PATTERN;
import static org.entando.kubernetes.service.digitalexchange.BundleUtilities.GIT_AND_SSH_PROTOCOL_REGEX_PATTERN;
import static org.entando.kubernetes.service.digitalexchange.BundleUtilities.HTTP_OVER_GIT_REPLACER;

import java.net.URL;
import org.entando.kubernetes.exception.EntandoValidationException;

public class GitUrlValidator {

    private final String originalUrl;
    private boolean isOverSSH = false;
    private boolean isValid;

    private GitUrlValidator(String url) {
        originalUrl = url;
        if (BUNDLE_PROTOCOL_REGEX_PATTERN.matcher(url).matches()) {
            if (GIT_AND_SSH_PROTOCOL_REGEX_PATTERN.matcher(url).matches()) {
                isOverSSH = true;
            }
            isValid = true;
            return;
        }
        isValid = false;
    }

    /**
     * This method parses a "fully qualified" URL for git over https or ssh to obtain an object that can be used to
     * validate the individual fields that make it up.
     *
     * @param url the string containing the fully qualified git url to parse and validate
     * @return an GitUrlValidator object with the parsed url
     */
    public static GitUrlValidator parse(String url) {
        return new GitUrlValidator(url);
    }

    /**
     * This method checks if the all sections are correct.
     *
     * @param invalidError the error string to use within EntandoValidationException error message
     * @return returns true if all sections are correct otherwise raises an EntandoValidationException exception
     */
    public boolean isValidOrThrow(String invalidError) {
        return isTransportValid(invalidError);
    }

    /**
     * This method checks if the transport section is correct.
     *
     * @param invalidError the error string to use within EntandoValidationException error message
     * @return returns true if transport is equal to "git:// ssh:// https:// http:// git@" otherwise raises an
     *     EntandoValidationException exception
     */
    public boolean isTransportValid(String invalidError) {
        if (isValid) {
            return true;
        } else {
            throw new EntandoValidationException(invalidError);
        }
    }

    /**
     * This method checks if the all sections are correct and then compose the common url with transport.
     *
     * @return returns the common Url to use to generate PluginID if all sections are correct otherwise raises an
     *     EntandoValidationException exception
     */
    public String composeCommonUrlOrThrow() {
        String url = null;
        if (isOverSSH) {
            // done just for validation with URL class
            url = convertGitOverSshUrlToGitOverHttp(originalUrl);
        } else {
            url = originalUrl;
        }
        URL bundleUrl = ValidationFunctions.composeUrlOrThrow(url,
                "The repository URL of the bundle is null",
                "The repository URL of the bundle is invalid");

        return bundleUrl.toString();
    }


    /**
     * This method checks if the all sections are correct and then compose the common url without transport.
     *
     * @return returns the common Url to use to generate PluginID if all sections are correct otherwise raises an
     *     EntandoValidationException exception
     */
    public String composeCommonUrlWithoutTransportOrThrow() {
        String url = null;
        if (isOverSSH) {
            // done just for validation with URL class
            url = convertGitOverSshUrlToGitOverHttp(originalUrl);
        } else {
            url = originalUrl;
        }
        URL bundleUrl = ValidationFunctions.composeUrlOrThrow(url,
                "The repository URL of the bundle is null",
                "The repository URL of the bundle is invalid");
        final int index = bundleUrl.toString().indexOf(bundleUrl.getHost());
        return bundleUrl.toString().substring(index);
    }

    private String convertGitOverSshUrlToGitOverHttp(String url) {
        String repoUrl = GIT_AND_SSH_PROTOCOL_REGEX_PATTERN.matcher(url).replaceFirst(HTTP_OVER_GIT_REPLACER);
        return COLONS_REGEX_PATTERN.matcher(repoUrl).replaceFirst("/");
    }
}
