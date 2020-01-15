package org.entando.kubernetes.model.web;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.entando.web.request.PagedListRequest;
import org.entando.web.response.PagedMetadata;
import org.entando.web.response.RestError;

@Getter@Setter
@NoArgsConstructor
public class ResilientPagedMetadata<T> extends PagedMetadata<T> {

    @JsonIgnore
    private List<RestError> errors = new ArrayList<>();

    public ResilientPagedMetadata(final PagedListRequest req, final List<T> body, final int totalItems) {
        super(req, body, totalItems);
    }

    public void addError(RestError restError) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(restError);
    }
}
