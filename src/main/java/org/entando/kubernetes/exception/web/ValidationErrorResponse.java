package org.entando.kubernetes.exception.web;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.MessageSource;
import org.springframework.validation.FieldError;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.util.Collections.singletonList;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ValidationErrorResponse extends ErrorResponse {

    private ErrorData data;

    public ValidationErrorResponse(final String message) {
        super(message);
        data = new ErrorData();
    }

    public ValidationErrorResponse(final String message, @NotNull final List<FieldError> errors) {
        this(message);
        errors.forEach(this::addError);
    }

    public void addError(@NotNull final FieldError fieldError) {
        final String message = fieldError.getDefaultMessage();
        if (fieldError.getArguments() != null) {
            this.data.getErrors().put(fieldError.getField(), singletonList(message));
        }
    }

    public void addError(final MessageSource messageSource, final Locale locale,
            final String property, final String error, final Object[] arguments) {
        final String message = messageSource.getMessage(error, arguments, locale);
        this.data.getErrors().put(property, singletonList(message));
    }

    @Data
    private static class ErrorData {

        private Map<String, List<String>> errors = new HashMap<>();
    }
}
