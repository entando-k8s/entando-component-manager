package org.entando.kubernetes.validator;

import org.apache.commons.lang3.ObjectUtils;
import org.entando.kubernetes.exception.EntandoValidationException;
import org.entando.kubernetes.model.entandohub.EntandoHubRegistry;
import org.springframework.stereotype.Component;

@Component
public class EntandoHubRegistryValidator {

    public static final boolean VALIDATE_ID_TOO = true;

    /**
     * validate the received EntandoHubRegistry.
     *
     * @param entandoHubRegistry the EntandoHubRegistry to validate
     * @param idMustNotBeEmpty   if true the validator checks also that the id is not empty, if false the id MUST be
     *                           empty
     * @return true if the validation succeeds
     * @throws EntandoValidationException if the validation fails
     */
    public boolean validateEntandoHubRegistryOrThrow(EntandoHubRegistry entandoHubRegistry, boolean idMustNotBeEmpty) {

        if (null == entandoHubRegistry) {
            throw new EntandoValidationException("The received Entando Hub registry is null");
        }

        if (idMustNotBeEmpty) {
            if (ObjectUtils.isEmpty(entandoHubRegistry.getId())) {
                throw new EntandoValidationException(
                        "The received Entando Hub registry has an empty ID and it needs a populated one");
            }
        } else {
            if (!ObjectUtils.isEmpty(entandoHubRegistry.getId())) {
                throw new EntandoValidationException(
                        "The received Entando Hub registry has a populated ID and it needs an empty one");
            }
        }
        if (ObjectUtils.isEmpty(entandoHubRegistry.getName())) {
            throw new EntandoValidationException("The received Entando Hub registry has an empty name");
        }
        // TODO implement when rebased on the branch with the host validation regex
//        if (ObjectUtils.isEmpty(entandoHubRegistry.getUrl())) {
//            throw new EntandoValidationException("The received Entando Hub registry has an empty name");
//        }
        return true;
    }

}
