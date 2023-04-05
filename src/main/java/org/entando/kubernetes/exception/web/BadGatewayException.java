package org.entando.kubernetes.exception.web;

import org.entando.kubernetes.exception.http.HttpException;
import org.springframework.http.HttpStatus;

public class BadGatewayException extends WebHttpException implements HttpException {

    public BadGatewayException(String message) {
        super(HttpStatus.BAD_GATEWAY, message);
    }

    @Override
    public HttpStatus getStatus() {
        return super.getStatus();
    }
}
