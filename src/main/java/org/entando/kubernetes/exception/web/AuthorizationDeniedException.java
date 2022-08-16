package org.entando.kubernetes.exception.web;

import org.entando.kubernetes.exception.http.HttpException;
import org.springframework.http.HttpStatus;

public class AuthorizationDeniedException extends WebHttpException implements HttpException {

    public AuthorizationDeniedException(final String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
