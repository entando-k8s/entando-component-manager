package org.entando.kubernetes.validator;

import java.net.URL;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.entando.kubernetes.exception.EntandoValidationException;

@UtilityClass
public class ValidationFunctions {

    /**
     * validate the received url using the received url validator. checks that the url is not empty. if this fails,
     * throw EntandoValidationException with nullError message checks that the url is valid. if this fails, throw
     * EntandoValidationException with invalidError message
     *
     * @param urlValidator the validator
     * @param url          the url to validate
     * @param nullError    the message to add to the EntandoValidationException if the url is empty
     * @param invalidError the message to add to the EntandoValidationException if the url is not compliant
     */
    public static void validateUrlOrThrow(UrlValidator urlValidator, URL url, String nullError, String invalidError) {

        if (ObjectUtils.isEmpty(url)) {
            throw new EntandoValidationException(nullError);
        }
        if (!urlValidator.isValid(url.toString())) {
            throw new EntandoValidationException(invalidError);
        }
    }
}
