package org.entando.kubernetes.exception.http;

import org.springframework.http.HttpStatus;

public interface HttpException {

    public HttpStatus getStatus();

}
