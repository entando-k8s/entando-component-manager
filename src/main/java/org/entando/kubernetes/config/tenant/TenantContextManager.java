package org.entando.kubernetes.config.tenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantContextManager {
    private final TenantContextHolder holder;
    public String getTenantCode() {
        String tenantCode = holder.get().getTenantCode();
        log.info("Get tenantCode from TenantContextHolder: {}", tenantCode);
        return tenantCode;
    }
}
