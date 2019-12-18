package org.entando.kubernetes.exception.http;

import org.springframework.http.HttpStatus;

public interface HttpNotFoundException extends HttpException{

    default HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
