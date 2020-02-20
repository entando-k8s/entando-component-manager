package org.entando.kubernetes.model.web.response;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter@Setter
@NoArgsConstructor
public class RestResponse<T, M> {

    private T payload;
    private M metaData;
    private List<RestError> errors = new ArrayList<>();

    public void setMetaData(M metadata) {
        this.metaData = metadata;
    }

    public RestResponse(final T payload) {
        setPayload(payload);
    }

    public RestResponse(final T payload, final M metaData) {
        setPayload(payload);
        setMetaData(metaData);
    }

    public RestResponse(final T payload, final M metaData, final List<RestError> errors) {
        this(payload, metaData);
        setErrors(errors);
    }

    public void addError(final RestError error) {
        errors.add(error);
    }

}
