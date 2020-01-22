package org.entando.kubernetes.controller;

import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.exception.http.HttpException;
import org.entando.kubernetes.exception.http.WithArgumentException;
import org.entando.web.exception.ErrorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ExceptionController {

    private final MessageSource messageSource;

    @Autowired
    public ExceptionController(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(EntandoComponentManagerException.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception, Locale locale) {
        log.warn("Exception caught {}", exception.getMessage(), exception);

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        Object[] args = new Object[]{};
        if (exception instanceof HttpException) {
            status = ((HttpException) exception).getStatus();
        }

        if (exception instanceof WithArgumentException) {
            args = ((WithArgumentException) exception).getArgs();
        }

        return ResponseEntity.status(status)
                .body(new ErrorResponse(messageSource.getMessage(exception.getMessage(), args, locale)));

    }

}
