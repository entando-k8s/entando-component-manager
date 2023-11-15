package org.entando.kubernetes.validator;

import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.EntandoValidationException;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;

@UtilityClass
@Slf4j
public class ValidationFunctions {

    public static final String HOST_MUST_START_AND_END_WITH_ALPHANUMERIC_REGEX = "^[a-zA-Z0-9].*[a-zA-Z0-9]$";
    public static final String VALID_CHARS_RFC_1123_REGEX = "^[a-z0-9.\\-].*$";
    public static final String VALID_ENTITY_CODE_REGEX = "^[\\w|-]+-[0-9a-fA-F]{8}$";
    public static final Pattern HOST_MUST_START_AND_END_WITH_ALPHANUMERIC_REGEX_PATTERN = Pattern.compile(
            HOST_MUST_START_AND_END_WITH_ALPHANUMERIC_REGEX);
    public static final Pattern VALID_CHARS_RFC_1123_REGEX_PATTERN = Pattern.compile(VALID_CHARS_RFC_1123_REGEX);
    public static final Pattern VALID_ENTITY_CODE_REGEX_PATTERN = Pattern.compile(VALID_ENTITY_CODE_REGEX);
    public static final String SUPPORTED_DB = "mysql|postgresql|oracle";
    public static final String DB_URL_REGEX = "^jdbc:(" + SUPPORTED_DB + ")://([a-zA-Z0-9_.]*[a-zA-Z0-9_]):([0-9]{4,5})(/)([a-zA-Z0-9-_?=&]+)$";
    public static final String GIT_PROTOCOL = "git";
    public static final String HTTP_PROTOCOL = "http";
    public static final String HTTPS_PROTOCOL = "https";
    public static final List<String> VALID_PROTOCOLS = List.of(GIT_PROTOCOL, HTTP_PROTOCOL, HTTPS_PROTOCOL);
    public static final String SEPARATOR = "://";
    public static final String DEFAULT_PROTOCOL = "http".concat(SEPARATOR);

    /**
     * if the url uses the git or ssh protocol, replace it with http validate the received url using url regex. checks
     * that the url is not empty. if this fails, throw EntandoValidationException with nullError message checks that the
     * url is valid. if this fails, throw EntandoValidationException with invalidError message
     *
     * @param stringUrl    the string containing the url to validate
     * @param nullError    the message to add to the EntandoValidationException if the url is empty
     * @param invalidError the message to add to the EntandoValidationException if the url is not compliant
     * @return the received url
     */

    public static String composeUrlForcingHttpProtocolOrThrow(String stringUrl, String nullError, String invalidError) {
        final String httpProtocolUrl = BundleUtilities.gitSshProtocolToHttp(stringUrl);
        composeUrlOrThrow(httpProtocolUrl, nullError, invalidError);
        return stringUrl;
    }

    public static String composeCommonUrlOrThrow(String stringUrl, String nullError, String invalidError) {
        ImageValidator imageValidator = ImageValidator.parse(stringUrl);
        if (imageValidator.isTransportValid()) {
            // docker url with no transport e no tag
            return imageValidator.composeCommonUrlOrThrow(invalidError);
        } else {
            // git url
            return composeUrlForcingHttpProtocolOrThrow(stringUrl, nullError, invalidError);
        }
    }

    /**
     * validate the received url using url regex. checks that the url is not empty. if this fails, throw
     * EntandoValidationException with nullError message checks that the url is valid. if this fails, throw
     * EntandoValidationException with invalidError message
     *
     * @param stringUrl    the string containing the url to validate
     * @param nullError    the message to add to the EntandoValidationException if the url is empty
     * @param invalidError the message to add to the EntandoValidationException if the url is not compliant
     * @return the url as java.net.URL
     */
    public static URL composeUrlOrThrow(String stringUrl, String nullError, String invalidError) {

        if (ObjectUtils.isEmpty(stringUrl)) {
            throw new EntandoValidationException(nullError);
        }

        URL url;
        try {
            url = new URL(stringUrl);
            url.toURI();
        } catch (Exception e) {
            throw new EntandoValidationException(invalidError + ": " + stringUrl);
        }

        if (!VALID_PROTOCOLS.contains(url.getProtocol())) {
            throw new EntandoValidationException(
                    invalidError + ": " + url + " - Protocol supported are : " + String.join(",", VALID_PROTOCOLS));
        }

        if (!HOST_MUST_START_AND_END_WITH_ALPHANUMERIC_REGEX_PATTERN.matcher(url.getHost()).matches()) {
            throw new EntandoValidationException(
                    invalidError + ": " + url + " - Hostname must start and finish with an alphanumeric character");
        }

        return url;
    }

    /**
     * validate the received code against the code regex.
     * @param entityCode the code to validate
     * @return true if the code is valid, false otherwise
     */
    public static boolean isEntityCodeValid(String entityCode) {
        try {
            validateEntityCodeOrThrow(entityCode);
        } catch (EntandoComponentManagerException e) {
            return false;
        }

        return true;
    }

    /**
     * validate the received code against the code regex.
     * @param entityCode the code to validate
     * @return the validated code
     * @throws EntandoComponentManagerException if the validation fails
     */
    public static String validateEntityCodeOrThrow(String entityCode) throws EntandoComponentManagerException {
        if (StringUtils.isBlank(entityCode)) {
            throw new EntandoComponentManagerException(
                    "The received code is empty");
        }

        if (!VALID_ENTITY_CODE_REGEX_PATTERN.matcher(entityCode).matches()) {
            throw new EntandoComponentManagerException(
                    "The received code \"" + entityCode + "\" does not respect the format: " + VALID_ENTITY_CODE_REGEX);
        }

        return entityCode;
    }

    /**
     * Check the URL for correctness. Database connection string are NOT handled by this method
     * The strategy here is to convert the URL to a URI object and take advantage of the Java library
     * to detect any error.
     *
     * @param candidate the URL to test
     * @param validateProtocol when false, the check against the protocol is skipped
     * @param validatePort when false, the port presence is ignored
     * @param validatePath when false the presence of the path is ignored
     * @return true if the URL is correct
     */
    public static boolean validateURL(String candidate, boolean validateProtocol, boolean validatePort,
            boolean validatePath) {

        try {
            candidate = validateProtocol ? candidate : appendProtocolIfMissing(candidate, null);

            final URL url = new URL(candidate);
            final String port = String.valueOf(url.getPort());

            return ((StringUtils.isNotBlank(url.toURI().toString())
                    && StringUtils.isNotBlank(url.getProtocol()))
                    && StringUtils.isNotBlank(url.getHost())
                    && (!validatePort
                    || (StringUtils.isNotBlank(port) && StringUtils.isNumeric(port)))
                    && (!validatePath || StringUtils.isNotBlank(url.getPath())));
        } catch (Exception t) {
            log.debug("URL validation failed for '{}'", candidate);
        }
        return false;
    }

    public static boolean validateDbURL(String dbUrl) {
        Pattern pattern = Pattern.compile(DB_URL_REGEX);
        Matcher matcher = pattern.matcher(dbUrl);
        return matcher.matches();
    }

    private String appendProtocolIfMissing(String candidate, String proto) {
        if (StringUtils.isNotBlank(candidate)
                && !candidate.contains(SEPARATOR)) {
            return StringUtils.isNotBlank(proto) ? proto.concat(candidate)
                    : DEFAULT_PROTOCOL.concat(candidate);
        }
        return candidate;
    }

    /**
     * Validate a FQDN using regexp.
     *
     * @param fqdn the fully qualified domain to validate
     * @return true if the verification is successful
     */
    public static boolean validateFQDN(String fqdn) {
        if (StringUtils.isNotBlank(fqdn) && fqdn.length() <= 255) {
            final String FQDN_REGEXP = "(?=^.{4,253}$)(^((?!-)[a-zA-Z0-9-]{0,62}[a-zA-Z0-9]\\.)+[a-zA-Z]{2,63}$)"; // NOSONAR

            Pattern pattern = Pattern.compile(FQDN_REGEXP);
            Matcher matcher = pattern.matcher(fqdn);
            return matcher.matches();
        }
        return false;
    }

}
