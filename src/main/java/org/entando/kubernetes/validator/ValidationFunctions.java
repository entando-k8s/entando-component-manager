package org.entando.kubernetes.validator;

import java.net.URL;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.ObjectUtils;
import org.entando.kubernetes.exception.EntandoValidationException;

@UtilityClass
public class ValidationFunctions {

    public static final String URL_REGEX = "^((https?)://)(([a-zA-Z0-9]+[-|\\.|_]?)*[a-zA-Z0-9])(\\:[0-9]{1,5})?(/([a-zA-Z0-9]+([-|\\.|/_][a-zA-Z0-9])?)*)*$";
    public static final Pattern URL_REGEX_PATTERN = Pattern.compile(URL_REGEX);

    /**
     * validate the received url using url regex. checks that the url is not empty. if this fails, throw
     * EntandoValidationException with nullError message checks that the url is valid. if this fails, throw
     * EntandoValidationException with invalidError message
     *
     * @param url          the url to validate
     * @param nullError    the message to add to the EntandoValidationException if the url is empty
     * @param invalidError the message to add to the EntandoValidationException if the url is not compliant
     */
    public static void validateUrlOrThrow(URL url, String nullError, String invalidError) {

        if (ObjectUtils.isEmpty(url)) {
            throw new EntandoValidationException(nullError);
        }
        if (!URL_REGEX_PATTERN.matcher(url.toString()).matches()) {
            throw new EntandoValidationException(invalidError + ": " + url);
        }
    }
}
