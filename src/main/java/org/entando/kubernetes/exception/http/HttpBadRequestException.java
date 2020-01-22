package org.entando.kubernetes.exception.http;

import org.springframework.http.HttpStatus;

public interface HttpBadRequestException extends HttpException {

    default HttpStatus getStatus() {
        return HttpStatus.BAD_REQUEST;
    }

}
