package org.entando.kubernetes.config.tenant;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public class TenantContextManager {

    public static final String KEY_TENANT_CODE = "TENANT_CODE";

    public static void setTenantCode(String tenantCode) {
        RequestContextHolder.getRequestAttributes()
                .setAttribute(KEY_TENANT_CODE, tenantCode, RequestAttributes.SCOPE_REQUEST);
    }

    public static String getTenantCode() {
        return String.valueOf(
                RequestContextHolder.getRequestAttributes()
                        .getAttribute(KEY_TENANT_CODE, RequestAttributes.SCOPE_REQUEST));
    }
}
