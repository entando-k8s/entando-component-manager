package org.entando.kubernetes.client.hub;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

@Data
@Builder
public class ProxiedPayload {

    private HttpStatus status;
    private Object payload;
    private String exceptionMessage;
    private String exceptionClass;

    public boolean hasError() {
        return (StringUtils.isNotBlank(exceptionMessage)
                && StringUtils.isNotBlank(exceptionMessage));
    }
}
