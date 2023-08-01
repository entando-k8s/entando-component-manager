package org.entando.kubernetes.config.tenant;


import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Data
@Component
@RequestScope
public class TenantContext {
    private String tenantCode;
}

