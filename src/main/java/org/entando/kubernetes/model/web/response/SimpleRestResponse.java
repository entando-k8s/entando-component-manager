package org.entando.kubernetes.model.web.response;

import static java.util.Optional.ofNullable;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter@Setter
public class SimpleRestResponse<T> extends RestResponse<T, Map<String, Object>> {

    public SimpleRestResponse(final T payload) {
        super(payload);
    }

    public SimpleRestResponse() {
        super();
    }

    public void addMetadata(final String key, final Object value) {
        ofNullable(getMetaData()).orElseGet(this::init)
                .put(key, value);
    }

    private Map<String, Object> init() {
        setMetaData(new HashMap<>());
        return getMetaData();
    }

}
