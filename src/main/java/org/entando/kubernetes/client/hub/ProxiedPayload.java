package org.entando.kubernetes.client.hub;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

@Data
@Builder
public class ProxiedPayload<T> {

    private HttpStatus status;
    private T payload;
    private String exceptionMessage;
    private String exceptionClass;

    public boolean hasError() {
        return ! status.is2xxSuccessful() ||
                (StringUtils.isNotBlank(exceptionMessage)
                && StringUtils.isNotBlank(exceptionMessage));
    }

    public ProxiedPayload<T> payload(T payload) {
        this.payload = payload;
        return this;
    }
}
