package org.entando.kubernetes.controller;

import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.exception.http.HttpException;
import org.entando.kubernetes.exception.http.WithArgumentException;
import org.entando.kubernetes.exception.http.WithPredefinedMessage;
import org.entando.kubernetes.exception.web.ErrorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.zalando.problem.spring.web.advice.ProblemHandling;

@Slf4j
@RestControllerAdvice
public class GlobalControllerExceptionHandler implements ProblemHandling {

    private final MessageSource messageSource;

    @Autowired
    public GlobalControllerExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }


    @ExceptionHandler(Exception.class)
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

        ErrorResponse errorResponse;
        if (exception instanceof WithPredefinedMessage) {
            String predefinedMessage = ((WithPredefinedMessage) exception).getPredefinedMessage();
            errorResponse = new ErrorResponse(messageSource.getMessage(predefinedMessage, args, locale));
        } else {
            errorResponse = new ErrorResponse(exception.getMessage());
        }

        return ResponseEntity.status(status)
                .body(errorResponse);

    }

}
