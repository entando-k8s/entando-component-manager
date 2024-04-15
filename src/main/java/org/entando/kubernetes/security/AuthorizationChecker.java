package org.entando.kubernetes.security;

import org.entando.kubernetes.exception.web.AuthorizationDeniedException;

public interface AuthorizationChecker {

    void checkPermissions(String authorizationHeader) throws AuthorizationDeniedException;
}
