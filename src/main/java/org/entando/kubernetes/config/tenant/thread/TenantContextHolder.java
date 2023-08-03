package org.entando.kubernetes.config.tenant.thread;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class TenantContextHolder {

    public static String getCurrentTenantCode() {
        log.debug("Getting tenant {}", threadLocal.get().getTenantCode());
        return threadLocal.get().getTenantCode();
    }

    public static void setCurrentTenantCode(String tenant) {
        log.debug("Setting tenant {}", tenant);
        threadLocal.set(new TenantContext(tenant));
    }

    private static final ThreadLocal<TenantContext> threadLocal = new InheritableThreadLocal<>() {

        @Override
        protected TenantContext childValue(TenantContext parentValue) {

            final String tenantCode = parentValue.getTenantCode();

            if (parentValue == null) {
                return null;
            }

            return new TenantContext(tenantCode);
        }
    };

    public static void destroy() {
        log.debug("destroy tenant context");
        threadLocal.remove();
    }
}
