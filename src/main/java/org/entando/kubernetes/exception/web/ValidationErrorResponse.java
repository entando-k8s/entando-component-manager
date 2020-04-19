package org.entando.kubernetes.exception.web;

import static java.util.Collections.singletonList;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.MessageSource;
import org.springframework.validation.FieldError;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
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

    private void addError(@NotNull final FieldError fieldError) {
        final String message = fieldError.getDefaultMessage();
        this.data.getErrors().put(fieldError.getField(), singletonList(message));
    }

    public void addError(final MessageSource messageSource, final Locale locale, @NotNull final FieldError fieldError) {
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
    private class ErrorData {

        private Map<String, List<String>> errors = new HashMap<>();
    }
}
