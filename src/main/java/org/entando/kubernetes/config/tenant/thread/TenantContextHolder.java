package org.entando.kubernetes.config.tenant.thread;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class TenantContextHolder {

    public static String getCurrentTenantCode() {

//        log.info("Getting tenant {}", threadLocal.get().getTenantCode());
        log.debug("Getting tenant {}", threadLocal.get() != null ? threadLocal.get().getTenantCode(): "primary");
        return threadLocal.get() != null ? threadLocal.get().getTenantCode(): "primary";
    }

    public static void setCurrentTenantCode(String tenant) {
        log.debug("Setting tenant {}", tenant);
        threadLocal.set(new TenantContext(tenant));
    }

    private static final ThreadLocal<TenantContext> threadLocal = new InheritableThreadLocal<>() {

        @Override
        protected TenantContext childValue(TenantContext parentValue) {
            return new TenantContext(parentValue.getTenantCode());
        }
    };

    public static void destroy() {
        log.debug("destroy tenant context");
        threadLocal.remove();
    }
    
}