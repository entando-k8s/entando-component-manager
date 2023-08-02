package org.entando.kubernetes.config.tenant;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@AllArgsConstructor
public class TenantContext {
    private String tenantCode;
}

