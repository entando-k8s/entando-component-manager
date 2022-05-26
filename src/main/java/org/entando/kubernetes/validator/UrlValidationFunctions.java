package org.entando.kubernetes.validator;

import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;
import org.entando.kubernetes.exception.EntandoValidationException;

@UtilityClass
public class UrlValidationFunctions {

    public static final String HOST_MUST_START_AND_END_WITH_ALPHANUMERIC_REGEX = "^[a-zA-Z0-9].*[a-zA-Z0-9]$";
    public static final Pattern HOST_MUST_START_AND_END_WITH_ALPHANUMERIC_REGEX_PATTERN = Pattern.compile(
            HOST_MUST_START_AND_END_WITH_ALPHANUMERIC_REGEX);

    public static final String HTTP_PROTOCOL = "http";
    public static final String HTTPS_PROTOCOL = "https";
    public static final List<String> VALID_PROTOCOLS = List.of(HTTP_PROTOCOL, HTTPS_PROTOCOL);


    /**
     * validate the received url using url regex.
     * checks that the url is not empty.
     * checks that the url is valid.
     *
     * @param stringUrl the string contianing the url to validate
     * @return the url as java.net.URL
     */
    public static URL composeUrlOrThrow(String stringUrl) {

        URL url;
        try {
            url = new URL(stringUrl);
            url.toURI();
        } catch (Exception e) {
            throw new EntandoValidationException("The received URL '" + stringUrl + "' is not valid");
        }

        if (!VALID_PROTOCOLS.contains(url.getProtocol())) {
            throw new EntandoValidationException(
                    "The received URL '" + url + "' is not valid - Protocol supported are : " + String.join(",",
                            VALID_PROTOCOLS));
        }

        if (!HOST_MUST_START_AND_END_WITH_ALPHANUMERIC_REGEX_PATTERN.matcher(url.getHost()).matches()) {
            throw new EntandoValidationException(
                    "The received URL '" + url
                            + "' is not valid - Hostname must start and finish with an alphanumeric character");
        }

        return url;
    }
}
