package org.entando.kubernetes.security;

import lombok.Setter;
import org.entando.kubernetes.exception.web.AuthorizationDeniedException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("test")
@Component
public class AuthorizationCheckerDouble implements AuthorizationChecker {

    @Setter
    private boolean error = false;

    @Override
    public void checkPermissions(String authorizationHeader) throws AuthorizationDeniedException {
        if (error) {
            throw new AuthorizationDeniedException("error");
        }
    }
}
